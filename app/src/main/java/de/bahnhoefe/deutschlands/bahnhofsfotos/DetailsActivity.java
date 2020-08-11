package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityDetailsBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ReportProblemBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.StationInfoBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.UploadBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.KeyValueSpinnerItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.NavItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.createChooser;
import static android.graphics.Color.WHITE;

public class DetailsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, BitmapAvailableHandler {

    private static final String TAG = DetailsActivity.class.getSimpleName();

    // Names of Extras that this class reacts to
    public static final String EXTRA_TAKE_FOTO = "DetailsActivityTakeFoto";
    public static final String EXTRA_UPLOAD = "upload";
    public static final String EXTRA_STATION = "station";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private static final int REQUEST_TAKE_PICTURE = 2;
    private static final int REQUEST_SELECT_PICTURE = 3;
    private static final int ALPHA = 128;

    private static final String LINK_FORMAT = "<b><a href=\"%s\">%s</a></b>";

    private BaseApplication baseApplication;
    private RSAPI rsapi;

    private ActivityDetailsBinding binding;

    private Upload upload;
    private Station station;
    private Set<Country> countries;
    private boolean localFotoUsed = false;
    private String nickname;
    private String email;
    private String password;
    private boolean fullscreen;
    private Bitmap publicBitmap;
    private Double latitude;
    private Double longitude;
    private String bahnhofId;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();
        final DbAdapter dbAdapter = baseApplication.getDbAdapter();
        final Set<String> countryCodes = baseApplication.getCountryCodes();
        countries = dbAdapter.fetchCountriesWithProviderApps(countryCodes);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.details.imageview.setOnClickListener(v -> onPictureClicked());
        binding.details.buttonTakePicture.setOnClickListener(
                v -> takePicture()
        );
        binding.details.buttonSelectPicture.setOnClickListener(
                v -> selectPicture()
        );
        binding.details.buttonReportProblem.setOnClickListener(
                v -> reportProblem()
        );
        binding.details.buttonUpload.setOnClickListener(v -> {
                if (!isLoggedIn()) {
                    Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(binding.details.etbahnhofname.getText())) {
                    Toast.makeText(this, R.string.station_title_needed, Toast.LENGTH_LONG).show();
                } else {
                    uploadPhoto();
                }
            }
        );

        binding.details.licenseTag.setMovementMethod(LinkMovementMethod.getInstance());

        // switch off image and license view until we actually have a foto
        binding.details.imageview.setVisibility(View.INVISIBLE);
        binding.details.licenseTag.setVisibility(View.INVISIBLE);
        setPictureButtonsEnabled(false);
        setButtonEnabled(binding.details.buttonReportProblem, false);
        setButtonEnabled(binding.details.buttonUpload, false);

        fullscreen = false;

        readPreferences();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        boolean directPicture = false;
        if (intent != null) {
            upload = (Upload) intent.getSerializableExtra(EXTRA_UPLOAD);
            station = (Station) intent.getSerializableExtra(EXTRA_STATION);
            latitude = (Double) intent.getSerializableExtra(EXTRA_LATITUDE);
            longitude = (Double) intent.getSerializableExtra(EXTRA_LONGITUDE);

            if (station == null && upload != null && upload.isUploadForExistingStation()) {
                station = baseApplication.getDbAdapter().getStationForUpload(upload);
            }

            if (latitude == null && longitude == null && upload != null && upload.isUploadForMissingStation()) {
                latitude = upload.getLat();
                longitude = upload.getLon();
            }

            if (station == null && (latitude == null || longitude == null)) {
                Log.w(TAG, "EXTRA_BAHNHOF and EXTRA_LATITUDE or EXTRA_LONGITUDE in intent data missing");
                Toast.makeText(this, R.string.station_or_coords_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            setButtonEnabled(binding.details.buttonReportProblem, station != null);

            directPicture = intent.getBooleanExtra(EXTRA_TAKE_FOTO, false);
            if (station != null) {
                bahnhofId = station.getId();
                binding.details.etbahnhofname.setText(station.getTitle());
                binding.details.etbahnhofname.setInputType(EditorInfo.TYPE_NULL);
                binding.details.etbahnhofname.setSingleLine(false);

                if (upload == null) {
                    upload = baseApplication.getDbAdapter().getPendingUploadForStation(station);
                }

                final int markerRes;
                if (station.hasPhoto()) {
                    if (ConnectionUtil.checkInternetConnection(this)) {
                        BitmapCache.getInstance().getFoto(this, station.getPhotoUrl());
                    }
                    setPictureButtonsEnabled(canSetPhoto());

                    if (isOwner()) {
                        markerRes = station.isActive() ? R.drawable.marker_violet : R.drawable.marker_violet_inactive;
                    } else {
                        markerRes = station.isActive() ? R.drawable.marker_green : R.drawable.marker_green_inactive;
                    }

                    // check for local photo
                    final File localFile = getStoredMediaFile(upload);
                    if (localFile != null && localFile.canRead()) {
                        new SimpleDialogs().confirm(this, R.string.local_photo_exists, (dialog, which) -> {
                            setPictureButtonsEnabled(true);
                            setLocalBitmap(upload);
                        });
                    }
                } else {
                    markerRes = station.isActive() ? R.drawable.marker_red : R.drawable.marker_red_inactive;
                    setLocalBitmap(upload);
                }
                binding.details.marker.setImageDrawable(getDrawable(markerRes));
            } else {
                if (upload == null) {
                    upload = baseApplication.getDbAdapter().getPendingUploadForCoordinates(latitude, longitude);
                }

                binding.details.etbahnhofname.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                if (upload != null) {
                    binding.details.etbahnhofname.setText(upload.getTitle());
                }
                setLocalBitmap(upload);
            }

        }

        if (directPicture) {
            takePicture();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        readPreferences();
    }

    private void readPreferences() {
        nickname = baseApplication.getNickname();
        email = baseApplication.getEmail();
        password = baseApplication.getPassword();
    }

    private boolean canSetPhoto() {
        return station == null || !station.hasPhoto() || isOwner();
    }

    private boolean isOwner() {
        return station != null && TextUtils.equals(nickname, station.getPhotographer());
    }

    private boolean isLoggedIn() {
        return !TextUtils.isEmpty(password);
    }

    void takePicture() {
        if (!canSetPhoto() || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return;
        }

        if (!isLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
        }

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            verifyCurrentPhotoUploadExists();
            final File file = getCameraTempFile();
            final Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, getResources().getString(R.string.app_name));
            intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, binding.details.etbahnhofname.getText());
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_TAKE_PICTURE);
        }
    }

    void selectPicture() {
        if (!canSetPhoto()) {
            return;
        }

        if (!isLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            return;
        }

        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), REQUEST_SELECT_PICTURE);
    }

    void reportProblem() {
        if (!isLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        ReportProblemBinding reportProblemBinding = ReportProblemBinding.inflate(getLayoutInflater());
        if (upload != null && upload.isProblemReport()) {
            reportProblemBinding.etProblemComment.setText(upload.getComment());
        }

        final List<String> problemTypes = new ArrayList<>();
        problemTypes.add(getString(R.string.problem_please_specify));
        int selected = -1;
        for (final ProblemType type : ProblemType.values()) {
            problemTypes.add(getString(type.getMessageId()));
            if (upload != null && upload.getProblemType() == type) {
                selected = problemTypes.size() - 1;
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, problemTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportProblemBinding.problemType.setAdapter(adapter);
        if (selected > -1) {
            reportProblemBinding.problemType.setSelection(selected);
        }

        builder.setTitle(R.string.report_problem)
                .setView(reportProblemBinding.getRoot())
                .setIcon(R.drawable.ic_bullhorn_48px)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final int selectedType = reportProblemBinding.problemType.getSelectedItemPosition();
            if (selectedType == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.problem_please_specify), Toast.LENGTH_LONG).show();
                return;
            }
            final ProblemType type = ProblemType.values()[selectedType - 1];
            final String comment = reportProblemBinding.etProblemComment.getText().toString();
            if (StringUtils.isBlank(comment)) {
                Toast.makeText(getApplicationContext(), getString(R.string.problem_please_comment), Toast.LENGTH_LONG).show();
                return;
            }
            alertDialog.dismiss();

            if (upload == null || !upload.isProblemReport() || upload.isUploaded()) {
                upload = new Upload();
                upload.setCountry(station.getCountry());
                upload.setStationId(station.getId());
                upload.setProblemType(type);
                upload.setComment(comment);
                upload = baseApplication.getDbAdapter().insertUpload(upload);
            } else {
                upload.setProblemType(type);
                upload.setComment(comment);
                baseApplication.getDbAdapter().updateUpload(upload);
            }

            rsapi.reportProblem(RSAPI.Helper.getAuthorizationHeader(email, password), new ProblemReport(station.getCountry(), bahnhofId, comment, type)).enqueue(new Callback<InboxResponse>() {
                @Override
                public void onResponse(final Call<InboxResponse> call, final Response<InboxResponse> response) {
                    InboxResponse inboxResponse = null;
                    if (response.isSuccessful()) {
                        inboxResponse = response.body();
                    } else if (response.code() == 401) {
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.authorization_failed);
                        return;
                    } else {
                        final Gson gson = new Gson();
                        inboxResponse = gson.fromJson(response.errorBody().charStream(),InboxResponse.class);
                    }

                    upload.setRemoteId(inboxResponse.getId());
                    upload.setUploadState(inboxResponse.getState().getUploadState());
                    baseApplication.getDbAdapter().updateUpload(upload);
                    new SimpleDialogs().confirm(DetailsActivity.this, inboxResponse.getState().getMessageId());
                }

                @Override
                public void onFailure(final Call<InboxResponse> call, final Throwable t) {
                    Log.e(TAG, "Error reporting problem", t);
                    new SimpleDialogs().confirm(DetailsActivity.this,
                            String.format(getText(R.string.problem_report_failed).toString(), t.getMessage()));
                }
            });
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        try {
            verifyCurrentPhotoUploadExists();
            final File storagePictureFile = getStoredMediaFile(upload);
            if (requestCode == REQUEST_TAKE_PICTURE) {
                // die Kamera-App sollte auf temporären Cache-Speicher schreiben, wir laden das Bild von
                // dort und schreiben es in Standard-Größe in den permanenten Speicher
                final File cameraRawPictureFile = getCameraTempFile();
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true; // just query the image size in the first step
                BitmapFactory.decodeFile(
                        cameraRawPictureFile.getPath(),
                        options);

                final int sampling = options.outWidth / Constants.STORED_PHOTO_WIDTH;
                if (sampling > 1) {
                    options.inSampleSize = sampling;
                }
                options.inJustDecodeBounds = false;

                final Bitmap scaledScreen = BitmapFactory.decodeFile(
                        cameraRawPictureFile.getPath(),
                        options);

                saveScaledBitmap(storagePictureFile, scaledScreen);
                setLocalBitmap(upload);
                FileUtils.deleteQuietly(cameraRawPictureFile);
            } else if (requestCode == REQUEST_SELECT_PICTURE) {
                final Uri selectedImageUri = data.getData();
                final Bitmap bitmap = getBitmapFromUri(selectedImageUri);

                final int sampling = bitmap.getWidth() / Constants.STORED_PHOTO_WIDTH;
                Bitmap scaledScreen = bitmap;
                if (sampling > 1) {
                    scaledScreen = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / sampling, bitmap.getHeight() / sampling, false);
                }

                saveScaledBitmap(storagePictureFile, scaledScreen);
                setLocalBitmap(upload);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error processing photo", e);
            Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void verifyCurrentPhotoUploadExists() {
        if (upload == null || upload.isProblemReport() || upload.isUploaded()) {
            upload = new Upload();
            if (station != null) {
                upload.setCountry(station.getCountry());
                upload.setStationId(station.getId());
            }
            upload.setLat(latitude);
            upload.setLon(longitude);
            upload = baseApplication.getDbAdapter().insertUpload(upload);
        }
    }

    private void saveScaledBitmap(final File storagePictureFile, final Bitmap scaledScreen) throws FileNotFoundException {
        if (scaledScreen == null) {
            throw new RuntimeException(getString(R.string.error_scaling_photo));
        }
        Log.d(TAG, "img width " + scaledScreen.getWidth());
        Log.d(TAG, "img height " + scaledScreen.getHeight());
        if (scaledScreen.getWidth() < scaledScreen.getHeight()) {
            Toast.makeText(getApplicationContext(), R.string.photo_need_landscape_orientation, Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Save photo to: " + storagePictureFile);
        scaledScreen.compress(Bitmap.CompressFormat.JPEG, Constants.STORED_PHOTO_QUALITY, new FileOutputStream(storagePictureFile));
    }

    private Bitmap getBitmapFromUri(final Uri uri) throws IOException {
        final ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        final Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    /**
     * Get the file path for storing this stations foto
     *
     * @return the File
     */
    @Nullable
    public File getStoredMediaFile(final Upload upload) {
        if (upload == null) {
            return null;
        }
        return FileUtils.getStoredMediaFile(this, upload.getId());
    }

    /**
     * Get the file path for the Camera app to store the unprocessed photo to.
     */
    private File getCameraTempFile() {
        return FileUtils.getImageCacheFile(this, String.valueOf(upload.getId()));
    }


    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.details, menu);
        final MenuItem navToStation = menu.findItem(R.id.nav_to_station);
        navToStation.getIcon().mutate();
        navToStation.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);

        if (localFotoUsed || (station != null && station.hasPhoto())) {
            enableMenuItem(menu, R.id.share_photo);
        } else {
            disableMenuItem(menu, R.id.share_photo);
        }

        if (station != null && Country.getCountryByCode(countries, station.getCountry()).hasTimetableUrlTemplate()) {
            enableMenuItem(menu, R.id.timetable);
        } else {
            disableMenuItem(menu, R.id.timetable);
        }

        boolean hasProviderApps = false;
        if (station != null) {
            final Country countryByCode = Country.getCountryByCode(countries, station.getCountry());
            hasProviderApps = countryByCode.hasCompatibleProviderApps();
        }
        if (hasProviderApps) {
            enableMenuItem(menu, R.id.provider_android_app);
        } else {
            disableMenuItem(menu, R.id.provider_android_app);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void enableMenuItem(final Menu menu, final int id) {
        final MenuItem menuItem = menu.findItem(id).setEnabled(true);
        menuItem.getIcon().mutate();
        menuItem.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);
    }

    private void disableMenuItem(final Menu menu, final int id) {
        final MenuItem menuItem = menu.findItem(id).setEnabled(false);
        menuItem.getIcon().mutate();
        menuItem.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);
        menuItem.getIcon().setAlpha(ALPHA);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_to_station:
                startNavigation(DetailsActivity.this);
                break;
            case R.id.timetable:
                final Intent timetableIntent = new Timetable().createTimetableIntent(Country.getCountryByCode(countries, station.getCountry()), station);
                if (timetableIntent != null) {
                    startActivity(timetableIntent);
                } else {
                    Toast.makeText(this, R.string.timetable_missing, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.share_photo:
                final Intent shareIntent = createFotoSendIntent();
                shareIntent.putExtra(Intent.EXTRA_TEXT, Country.getCountryByCode(countries, station != null ? station.getCountry() : null).getTwitterTags() + " " + binding.details.etbahnhofname.getText());
                shareIntent.setType("image/jpeg");
                startActivity(createChooser(shareIntent, "send"));
                break;
            case R.id.station_info:
                showStationInfo(null);
                break;
            case R.id.provider_android_app:
                final Country country = Country.getCountryByCode(countries, station.getCountry());
                final List<ProviderApp> providerApps = country.getCompatibleProviderApps();
                if (providerApps.size() == 1) {
                    providerApps.get(0).openAppOrPlayStore(this);
                } else if (providerApps.size() > 1) {
                    final CharSequence[] appNames = new CharSequence[providerApps.size()];
                    for (int i = 0; i < providerApps.size(); i++) {
                        appNames[i] = providerApps.get(i).getName();
                    }
                    new SimpleDialogs().simpleSelect(this, getResources().getString(R.string.choose_provider_app), appNames, -1, (dialog, which) -> {
                        if (which >= 0 && providerApps.size() > which) {
                            providerApps.get(which).openAppOrPlayStore(DetailsActivity.this);
                        }
                    });
                } else {
                    Toast.makeText(this, R.string.provider_app_missing, Toast.LENGTH_LONG).show();
                }

                break;
            case android.R.id.home:
                navigateUp();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        navigateUp();
    }

    public void navigateUp() {
        final ComponentName callingActivity = getCallingActivity(); // if MapsActivity was calling, then we don't want to rebuild the Backstack
        if (callingActivity == null) {
            final Intent upIntent = NavUtils.getParentActivityIntent(this);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
                Log.v(TAG, "Recreate back stack");
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
            }
        }

        finish();
    }

    public void showStationInfo(final View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        final LayoutInflater inflater = this.getLayoutInflater();
        final StationInfoBinding stationInfoBinding = StationInfoBinding.inflate(getLayoutInflater());
        stationInfoBinding.id.setText(station != null ? station.getId() : "");

        final double lat = station != null ? station.getLat() : latitude;
        final double lon = station != null ? station.getLon() : longitude;
        stationInfoBinding.coordinates.setText(String.format(Locale.US, getResources().getString(R.string.coordinates), lat, lon));

        stationInfoBinding.active.setText(station != null && station.isActive() ? R.string.active : R.string.inactive);
        stationInfoBinding.owner.setText(station != null && station.getPhotographer() != null ? station.getPhotographer() : "");

        builder.setTitle(binding.details.etbahnhofname.getText())
                .setView(stationInfoBinding.getRoot())
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void uploadPhoto() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        final UploadBinding uploadBinding = UploadBinding.inflate(getLayoutInflater());
        if (upload != null) {
            uploadBinding.etComment.setText(upload.getComment());
        }

        uploadBinding.spActive.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.active_flag_options)));
        if (station != null) {
            uploadBinding.spActive.setVisibility(View.GONE);
        } else {
            uploadBinding.spActive.setSelection(0);
        }

        if (station != null) {
            uploadBinding.spCountries.setVisibility(View.GONE);
        } else {
            final List<Country> countryList = baseApplication.getDbAdapter().getAllCountries();
            final KeyValueSpinnerItem[] items = new KeyValueSpinnerItem[countryList.size() + 1];
            items[0] = new KeyValueSpinnerItem(getString(R.string.chooseCountry), "");

            for (int i = 0; i < countryList.size(); i++) {
                final Country country = countryList.get(i);
                items[i+1] = new KeyValueSpinnerItem(country.getName(), country.getCode());
            }
            final ArrayAdapter<KeyValueSpinnerItem> countryAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, items);
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            uploadBinding.spCountries.setAdapter(countryAdapter);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            uploadBinding.txtPanorama.setText(Html.fromHtml(getString(R.string.panorama_info),Html.FROM_HTML_MODE_COMPACT));
        } else {
            uploadBinding.txtPanorama.setText(Html.fromHtml(getString(R.string.panorama_info)));
        }
        uploadBinding.txtPanorama.setMovementMethod(LinkMovementMethod.getInstance());
        uploadBinding.txtPanorama.setLinkTextColor(Color.parseColor("#c71c4d"));

        String overrideLicense = null;
        if (station != null) {
            final Country country = Country.getCountryByCode(countries, station.getCountry());
            overrideLicense = country.getOverrideLicense();
        }
        if (overrideLicense != null) {
            uploadBinding.cbSpecialLicense.setText(getString(R.string.special_license, overrideLicense));
        }
        uploadBinding.cbSpecialLicense.setVisibility(overrideLicense == null ? View.GONE : View.VISIBLE);

        builder.setTitle(R.string.photo_upload)
                .setView(uploadBinding.getRoot())
                .setIcon(R.drawable.ic_bullhorn_48px)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (uploadBinding.cbSpecialLicense.getText().length() > 0 && !uploadBinding.cbSpecialLicense.isChecked()) {
                Toast.makeText(this, R.string.special_license_confirm, Toast.LENGTH_LONG).show();
                return;
            }
            if (station == null) {
                if (uploadBinding.spActive.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, R.string.active_flag_choose, Toast.LENGTH_LONG).show();
                    return;
                }
                final KeyValueSpinnerItem selectedCountry = (KeyValueSpinnerItem) uploadBinding.spCountries.getSelectedItem();
                upload.setCountry(selectedCountry.getValue());
            }
            alertDialog.dismiss();

            final ProgressDialog progress = new ProgressDialog(DetailsActivity.this);
            progress.setMessage(getResources().getString(R.string.send));
            progress.setTitle(getResources().getString(R.string.app_name));
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();

            String stationTitle = binding.details.etbahnhofname.getText().toString();
            String comment = uploadBinding.etComment.getText().toString();
            upload.setTitle(stationTitle);
            upload.setComment(comment);

            try {
                stationTitle = URLEncoder.encode(binding.details.etbahnhofname.getText().toString(), "UTF-8");
                comment = URLEncoder.encode(comment, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding station title or comment", e);
            }
            baseApplication.getDbAdapter().updateUpload(upload);
            final Boolean active = uploadBinding.spActive.getSelectedItemPosition() == 1;

            final File mediaFile = getStoredMediaFile(upload);
            final RequestBody file = RequestBody.create(mediaFile, MediaType.parse(URLConnection.guessContentTypeFromName(mediaFile.getName())));
            rsapi.photoUpload(RSAPI.Helper.getAuthorizationHeader(email, password), bahnhofId, station != null ? station.getCountry() : upload.getCountry(),
                    stationTitle, latitude, longitude, comment, active, file).enqueue(new Callback<InboxResponse>() {
                @Override
                public void onResponse(final Call<InboxResponse> call, final Response<InboxResponse> response) {
                    progress.dismiss();
                    InboxResponse inboxResponse = null;
                    if (response.isSuccessful()) {
                        inboxResponse = response.body();
                    } else if (response.code() == 401) {
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.authorization_failed);
                        return;
                    } else {
                        final Gson gson = new Gson();
                        inboxResponse = gson.fromJson(response.errorBody().charStream(),InboxResponse.class);
                    }

                    upload.setRemoteId(inboxResponse.getId());
                    upload.setInboxUrl(inboxResponse.getInboxUrl());
                    upload.setUploadState(inboxResponse.getState().getUploadState());
                    baseApplication.getDbAdapter().updateUpload(upload);
                    new SimpleDialogs().confirm(DetailsActivity.this, inboxResponse.getState().getMessageId());
                }

                @Override
                public void onFailure(final Call<InboxResponse> call, final Throwable t) {
                    Log.e(TAG, "Error uploading photo", t);
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    new SimpleDialogs().confirm(DetailsActivity.this,
                            String.format(getText(R.string.upload_failed).toString(), t.getMessage()));
                }
            });
        });
    }

    private Intent createFotoSendIntent() {
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        final File file = getStoredMediaFile(upload);
        if (file != null && file.canRead()) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                    BuildConfig.APPLICATION_ID + ".fileprovider", file));
        } else if (publicBitmap != null) {
            final File newFile = FileUtils.getImageCacheFile(getApplicationContext(), String.valueOf(System.currentTimeMillis()));
            try {
                saveScaledBitmap(newFile, publicBitmap);
                sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileprovider", newFile));
            } catch (final FileNotFoundException e) {
                Log.e(TAG, "Error saving cached bitmap", e);
            }
        }
        return sendIntent;
    }

    private void startNavigation(final Context context) {
        final NavItem[] items = {
                new NavItem("   " + getString(R.string.nav_oepnv), R.drawable.ic_directions_bus_gray_24px),
                new NavItem("   " + getString(R.string.nav_car), R.drawable.ic_directions_car_gray_24px),
                new NavItem("   " + getString(R.string.nav_bike), R.drawable.ic_directions_bike_gray_24px),
                new NavItem("   " + getString(R.string.nav_walk), R.drawable.ic_directions_walk_gray_24px),
                new NavItem("   " + getString(R.string.nav_show), R.drawable.ic_info_gray_24px),
                new NavItem("   " + getString(R.string.nav_show_on_map), R.drawable.ic_map_gray_24px)
        };


        final ListAdapter adapter = new ArrayAdapter<NavItem>(
                this,
                android.R.layout.select_dialog_item,
                android.R.id.text1,
                items) {
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                //Use super class to create the View
                final View v = super.getView(position, convertView, parent);
                final TextView tv = v.findViewById(android.R.id.text1);

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

                //Add margin between image and text (support various screen densities)
                final int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                final int dp7 = (int) (20 * getResources().getDisplayMetrics().density);
                tv.setCompoundDrawablePadding(dp5);
                tv.setPadding(dp7, 0, 0, 0);

                return v;
            }
        };

        final double lat = station != null ? station.getLat() : latitude;
        final double lon = station != null ? station.getLon() : longitude;
        final AlertDialog.Builder navBuilder = new AlertDialog.Builder(this);
        navBuilder.setIcon(R.mipmap.ic_launcher);
        navBuilder.setTitle(R.string.navMethod);
        navBuilder.setAdapter(adapter, (dialog, navItem) -> {
            String dlocation = "";
            Intent intent = null;
            switch (navItem) {
                case 0:
                    dlocation = String.format("google.navigation:ll=%s,%s&mode=Transit", lat, lon);
                    Log.d(TAG, "findnavigation case 0: " + dlocation);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                    break;
                case 1:
                    dlocation = String.format("google.navigation:ll=%s,%s&mode=d", lat, lon);
                    Log.d(TAG,"findnavigation case 1: " + dlocation);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                    break;

                case 2:
                    dlocation = String.format("google.navigation:ll=%s,%s&mode=b",
                            lat, lon);
                    Log.d(TAG,"findnavigation case 2: " + dlocation);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                    break;
                case 3:
                    dlocation = String.format("google.navigation:ll=%s,%s&mode=w", lat, lon);
                    Log.d(TAG,"findnavigation case 3: " + dlocation);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                    break;
                case 4:
                    dlocation = String.format("geo:0,0?q=%s,%s(%s)", lat, lon, binding.details.etbahnhofname.getText());
                    Log.d(TAG,"findnavigation case 4: " + dlocation);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                    break;
                case 5:
                    intent = new Intent(DetailsActivity.this, MapsActivity.class);
                    intent.putExtra(MapsActivity.EXTRAS_LATITUDE, lat);
                    intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, lon);
                    Log.d(TAG,"findnavigation case 5: " + lat + "," + lon);
                    break;

            }
            try {
                startActivity(intent);
            } catch (final Exception e) {
                final Toast toast = Toast.makeText(context, R.string.activitynotfound, Toast.LENGTH_LONG);
                toast.show();
            }
        }).show();

    }

    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmapFromCache the fetched Bitmap for the notification. May be null
     */
    @Override
    public void onBitmapAvailable(final @Nullable Bitmap bitmapFromCache) {
        runOnUiThread(()-> {
            localFotoUsed = false;
            binding.details.buttonUpload.setEnabled(false);
            publicBitmap = bitmapFromCache;
            if (publicBitmap == null) {
                // keine Bitmap ausgelesen
                // switch off image and license view until we actually have a foto
                // @todo broken image anzeigen
                binding.details.imageview.setVisibility(View.INVISIBLE);
                binding.details.licenseTag.setVisibility(View.INVISIBLE);
                return;
            }

            // Lizenzinfo aufbauen und einblenden
            binding.details.licenseTag.setVisibility(View.VISIBLE);
            if (station != null && station.getLicense() != null) {
                final boolean photographerUrlAvailable = station.getPhotographerUrl() != null && !station.getPhotographerUrl().isEmpty();
                final boolean licenseUrlAvailable = station.getLicenseUrl() != null && !station.getLicenseUrl().isEmpty();

                final String photographerText;
                if (photographerUrlAvailable) {
                    photographerText = String.format(
                            LINK_FORMAT,
                            station.getPhotographerUrl(),
                            station.getPhotographer());
                } else {
                    photographerText = station.getPhotographer();
                }

                final String licenseText;
                if (licenseUrlAvailable) {
                    licenseText = String.format(
                            LINK_FORMAT,
                            station.getLicenseUrl(),
                            station.getLicense());
                } else {
                    licenseText = station.getLicense();
                }
                binding.details.licenseTag.setText(
                        Html.fromHtml(
                            String.format(
                                    getText(R.string.license_tag).toString(),
                                    photographerText,
                                    licenseText)
                        )
                );
            } else {
                binding.details.licenseTag.setText(R.string.license_info_not_readable);
            }

            setBitmap(publicBitmap);
        });
    }

    /**
     * Fetch bitmap from device local location, if it exists, and set the photo view.
     */
    private void setLocalBitmap(final Upload upload) {
        setPictureButtonsEnabled(true);

        final Bitmap showBitmap = checkForLocalPhoto(upload);
        setButtonEnabled(binding.details.buttonUpload, false);
        if (showBitmap == null) {
            // there is no local bitmap
            localFotoUsed = false;
            binding.details.imageview.setVisibility(View.INVISIBLE);
            binding.details.licenseTag.setVisibility(View.INVISIBLE);
        } else {
            setBitmap(showBitmap);
            fetchUploadStatus(upload);
        }
    }

    private void fetchUploadStatus(final Upload upload) {
        if (upload == null) {
            return;
        }
        setButtonEnabled(binding.details.buttonUpload, true);
        if (!upload.isUploaded()) {
            return;
        }
        final List<InboxStateQuery> stateQueries = new ArrayList<>();
        stateQueries.add(new InboxStateQuery(upload.getRemoteId()));

        baseApplication.getRSAPI().queryUploadState(RSAPI.Helper.getAuthorizationHeader(baseApplication.getEmail(), baseApplication.getPassword()), stateQueries).enqueue(new Callback<List<InboxStateQuery>>() {
            @Override
            public void onResponse(final Call<List<InboxStateQuery>> call, final Response<List<InboxStateQuery>> response) {
                final List<InboxStateQuery> stateQueries = response.body();
                if (stateQueries != null && !stateQueries.isEmpty()) {
                    final InboxStateQuery stateQuery = stateQueries.get(0);
                    binding.details.licenseTag.setText(getString(R.string.upload_state, getString(stateQuery.getState().getTextId())));
                    binding.details.licenseTag.setTextColor(getResources().getColor(stateQuery.getState().getColorId()));
                    binding.details.licenseTag.setVisibility(View.VISIBLE);
                    baseApplication.getDbAdapter().updateUploadStates(stateQueries);
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(final Call<List<InboxStateQuery>> call, final Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(DetailsActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void setPictureButtonsEnabled(final boolean enabled) {
        setButtonEnabled(binding.details.buttonTakePicture, enabled);
        setButtonEnabled(binding.details.buttonSelectPicture, enabled);
    }

    private void setButtonEnabled(final ImageButton imageButton, final boolean enabled) {
        imageButton.setEnabled(enabled);
        imageButton.setImageAlpha(enabled ? 255 : 125);
    }

    private void setBitmap(final Bitmap showBitmap) {
        final Display display = getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final int targetWidth = size.x;

        if (showBitmap.getWidth() != targetWidth) {
            final Bitmap scaledBitmap = Bitmap.createScaledBitmap(showBitmap,
                    targetWidth,
                    (int) (((long) showBitmap.getHeight() * (long) targetWidth) / showBitmap.getWidth()),
                    true);
            binding.details.imageview.setImageBitmap(scaledBitmap);
        } else {
            binding.details.imageview.setImageBitmap(showBitmap);
        }
        binding.details.imageview.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
    }

    private class AnimationUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
            final float alpha = (float) animation.getAnimatedValue();
            binding.details.header.setAlpha(alpha);
            binding.details.licenseTag.setAlpha(alpha);
            binding.details.buttons.setAlpha(alpha);
            if (binding.details.rotateInfo != null) {
                binding.details.rotateInfo.setAlpha(alpha);
            }
        }
    }

    /**
     * Check if there's a local photo file for this station.
     *
     * @return the Bitmap of the photo, or null if none exists.
     */
    @Nullable
    private Bitmap checkForLocalPhoto(final Upload upload) {
        // show the image
        final File localFile = getStoredMediaFile(upload);
        Log.d(TAG, "File: " + localFile);
        if (localFile != null && localFile.canRead()) {
            Log.d(TAG, "FileGetPath: " + localFile.getPath());
            final Bitmap scaledScreen = BitmapFactory.decodeFile(localFile.getPath());
            Log.d(TAG, "img width " + scaledScreen.getWidth());
            Log.d(TAG, "img height " + scaledScreen.getHeight());
            localFotoUsed = true;
            setButtonEnabled(binding.details.buttonUpload, true);
            return scaledScreen;
        } else {
            localFotoUsed = false;
            setButtonEnabled(binding.details.buttonUpload, false);
            Log.e(TAG, String.format("Media file not available for station %s", bahnhofId));
            return null;
        }
    }

    public void onPictureClicked() {
        if (!fullscreen) {
            final ValueAnimator animation = ValueAnimator.ofFloat(binding.details.header.getAlpha(), 0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            binding.details.contentDetails.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
            final ActionBar bar = getActionBar();
            if (bar != null) {
                bar.hide();
            }
            final androidx.appcompat.app.ActionBar sbar = getSupportActionBar();
            if (sbar != null) {
                sbar.hide();
            }
            fullscreen = true;
        } else {
            final ValueAnimator animation = ValueAnimator.ofFloat(binding.details.header.getAlpha(), 1.0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            binding.details.contentDetails.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            final ActionBar bar = getActionBar();
            if (bar != null) {
                bar.show();
            }
            final androidx.appcompat.app.ActionBar sbar = getSupportActionBar();
            if (sbar != null) {
                sbar.show();
            }
            fullscreen = false;
        }

    }

}
