package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
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

    private LinearLayout buttons;
    private ImageButton takePictureButton;
    private ImageButton selectPictureButton;
    private ImageButton reportProblemButton;
    private ImageButton uploadPhotoButton;

    private RelativeLayout header;
    private ImageView imageView;
    private EditText etBahnhofName;
    private TextView licenseTagView;
    private ViewGroup detailsLayout;
    private ImageView marker;

    private Upload upload;
    private Station station;
    private Set<Country> countries;
    private boolean localFotoUsed = false;
    private String nickname;
    private String email;
    private String password;
    private Set<String> countryCodes;
    private boolean fullscreen;
    private Bitmap publicBitmap;
    private Double latitude;
    private Double longitude;
    private String bahnhofId;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();
        final DbAdapter dbAdapter = baseApplication.getDbAdapter();
        countryCodes = baseApplication.getCountryCodes();
        countries = dbAdapter.fetchCountriesWithProviderApps(countryCodes);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        detailsLayout = findViewById(R.id.content_details);
        header = findViewById(R.id.header);
        marker = findViewById(R.id.marker);
        etBahnhofName = findViewById(R.id.etbahnhofname);
        imageView = findViewById(R.id.imageview);
        imageView.setOnClickListener(v -> onPictureClicked());
        takePictureButton = findViewById(R.id.button_take_picture);
        takePictureButton.setOnClickListener(
                v -> takePicture()
        );
        selectPictureButton = findViewById(R.id.button_select_picture);
        selectPictureButton.setOnClickListener(
                v -> selectPicture()
        );
        reportProblemButton = findViewById(R.id.button_report_problem);
        reportProblemButton.setOnClickListener(
                v -> reportProblem()
        );

        uploadPhotoButton = findViewById(R.id.button_upload);
        uploadPhotoButton.setOnClickListener(v -> {
                if (!isLoggedIn()) {
                    Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(etBahnhofName.getText())) {
                        Toast.makeText(DetailsActivity.this, R.string.station_title_needed, Toast.LENGTH_LONG).show();
                } else {
                    uploadPhoto();
                }
            }
        );

        licenseTagView = findViewById(R.id.license_tag);
        licenseTagView.setMovementMethod(LinkMovementMethod.getInstance());

        buttons = findViewById(R.id.buttons);

        // switch off image and license view until we actually have a foto
        imageView.setVisibility(View.INVISIBLE);
        licenseTagView.setVisibility(View.INVISIBLE);
        setPictureButtonsEnabled(false);
        setButtonEnabled(reportProblemButton, false);
        setButtonEnabled(uploadPhotoButton, false);

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
            setButtonEnabled(reportProblemButton, station != null);

            directPicture = intent.getBooleanExtra(EXTRA_TAKE_FOTO, false);
            if (station != null) {
                bahnhofId = station.getId();
                etBahnhofName.setText(station.getTitle());
                etBahnhofName.setInputType(EditorInfo.TYPE_NULL);
                etBahnhofName.setSingleLine(false);

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
                        new SimpleDialogs().confirm(this, R.string.local_photo_exists, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                setPictureButtonsEnabled(true);
                                setLocalBitmap(upload);
                            }
                        });
                    }
                } else {
                    markerRes = station.isActive() ? R.drawable.marker_red : R.drawable.marker_red_inactive;
                    setLocalBitmap(upload);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    marker.setImageDrawable(getDrawable(markerRes));
                } else {
                    marker.setImageDrawable(getResources().getDrawable(markerRes));
                }
            } else {
                if (upload == null) {
                    upload = baseApplication.getDbAdapter().getPendingUploadForCoordinates(latitude, longitude);
                }

                etBahnhofName.setInputType(EditorInfo.TYPE_CLASS_TEXT);
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
            intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, etBahnhofName.getText());
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
        final View dialogView = getLayoutInflater().inflate(R.layout.report_problem, null);
        final EditText etComment = dialogView.findViewById(R.id.et_problem_comment);
        etComment.setHint(R.string.report_problem_comment_hint);

        final Spinner problemType = dialogView.findViewById(R.id.problem_type);
        final List<String> problemTypes = new ArrayList<>();
        problemTypes.add(getString(R.string.problem_please_specify));
        for (final ProblemType type : ProblemType.values()) {
            problemTypes.add(getString(type.getMessageId()));
        }
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, problemTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        problemType.setAdapter(adapter);

        builder.setTitle(R.string.report_problem)
                .setView(dialogView)
                .setIcon(R.drawable.ic_bullhorn_48px)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final int selectedType = problemType.getSelectedItemPosition();
            if (selectedType == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.problem_please_specify), Toast.LENGTH_LONG).show();
                return;
            }
            final ProblemType type = ProblemType.values()[selectedType];
            final String comment = etComment.getText().toString();
            if (StringUtils.isBlank(comment)) {
                Toast.makeText(getApplicationContext(), getString(R.string.problem_please_comment), Toast.LENGTH_LONG).show();
                return;
            }
            alertDialog.dismiss();
            rsapi.reportProblem(RSAPI.Helper.getAuthorizationHeader(email, password), new ProblemReport(station.getCountry(), bahnhofId, comment, type)).enqueue(new Callback<InboxResponse>() {
                @Override
                public void onResponse(final Call<InboxResponse> call, final Response<InboxResponse> response) {
                    InboxResponse inboxResponse = null;
                    if (response.isSuccessful()) {
                        inboxResponse = response.body();
                    } else {
                        final Gson gson = new Gson();
                        inboxResponse = gson.fromJson(response.errorBody().charStream(),InboxResponse.class);
                    }

                    Upload upload = new Upload();
                    upload.setCountry(station.getCountry());
                    upload.setStationId(station.getId());
                    upload.setRemoteId(inboxResponse.getId());
                    upload.setProblemType(type);
                    upload.setComment(comment);
                    upload.setUploadState(inboxResponse.getState().getUploadState());
                    DetailsActivity.this.upload = baseApplication.getDbAdapter().insertUpload(upload);
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
        if (upload == null || !upload.isPendingPhotoUpload()) {
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
                shareIntent.putExtra(Intent.EXTRA_TEXT, Country.getCountryByCode(countries, station != null ? station.getCountry() : null).getTwitterTags() + " " + etBahnhofName.getText());
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
                    new SimpleDialogs().simpleSelect(this, getResources().getString(R.string.choose_provider_app), appNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            if (which >= 0 && providerApps.size() > which) {
                                providerApps.get(which).openAppOrPlayStore(DetailsActivity.this);
                            }
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
        final View dialogView = inflater.inflate(R.layout.station_info, null);
        final TextView title = dialogView.findViewById(R.id.title);
        title.setText(etBahnhofName.getText());
        final TextView id = dialogView.findViewById(R.id.id);
        id.setText(station != null ? station.getId() : "");

        final TextView coordinates = dialogView.findViewById(R.id.coordinates);
        final double lat = station != null ? station.getLat() : latitude;
        final double lon = station != null ? station.getLon() : longitude;
        coordinates.setText(String.format(Locale.US, getResources().getString(R.string.coordinates), lat, lon));

        final TextView active = dialogView.findViewById(R.id.active);
        active.setText(station != null && station.isActive() ? R.string.active : R.string.inactive);
        final TextView owner = dialogView.findViewById(R.id.owner);
        owner.setText(station != null && station.getPhotographer() != null ? station.getPhotographer() : "");

        builder.setTitle(R.string.station_info)
                .setView(dialogView)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void uploadPhoto() {
        new SimpleDialogs().prompt(this, R.string.comment, EditorInfo.TYPE_CLASS_TEXT, R.string.comment_hint, null, v -> {
            final ProgressDialog progress = new ProgressDialog(DetailsActivity.this);
            progress.setMessage(getResources().getString(R.string.send));
            progress.setTitle(getResources().getString(R.string.app_name));
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();

            String stationTitle = etBahnhofName.getText().toString();
            String comment = v;

            try {
                stationTitle = URLEncoder.encode(etBahnhofName.getText().toString(), "UTF-8");
                comment = URLEncoder.encode(v, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding station title or comment", e);
            }

            final File mediaFile = getStoredMediaFile(upload);
            final RequestBody file = RequestBody.create(mediaFile, MediaType.parse(URLConnection.guessContentTypeFromName(mediaFile.getName())));
            rsapi.photoUpload(RSAPI.Helper.getAuthorizationHeader(email, password), bahnhofId, station != null ? station.getCountry() : null,
                    stationTitle, latitude, longitude, comment, file).enqueue(new Callback<InboxResponse>() {
                @Override
                public void onResponse(final Call<InboxResponse> call, final Response<InboxResponse> response) {
                    progress.dismiss();
                    InboxResponse inboxResponse = null;
                    if (response.isSuccessful()) {
                        response.body();
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
                    progress.dismiss();
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
                    "de.bahnhoefe.deutschlands.bahnhofsfotos.fileprovider", file));
        } else if (publicBitmap != null) {
            final File newFile = FileUtils.getImageCacheFile(getApplicationContext(), String.valueOf(System.currentTimeMillis()));
            try {
                saveScaledBitmap(newFile, publicBitmap);
                sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                        "de.bahnhoefe.deutschlands.bahnhofsfotos.fileprovider", newFile));
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
                final TextView tv = (TextView) v.findViewById(android.R.id.text1);

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
        navBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int navItem) {
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
                        dlocation = String.format("geo:0,0?q=%s,%s(%s)", lat, lon, etBahnhofName.getText());
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
        localFotoUsed = false;
        uploadPhotoButton.setEnabled(false);
        publicBitmap = bitmapFromCache;
        if (publicBitmap == null) {
            // keine Bitmap ausgelesen
            // switch off image and license view until we actually have a foto
            // @todo broken image anzeigen
            imageView.setVisibility(View.INVISIBLE);
            licenseTagView.setVisibility(View.INVISIBLE);
            return;
        }

        // Lizenzinfo aufbauen und einblenden
        licenseTagView.setVisibility(View.VISIBLE);
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
            licenseTagView.setText(
                    Html.fromHtml(
                        String.format(
                                getText(R.string.license_tag).toString(),
                                photographerText,
                                licenseText)
                    )
            );
        } else {
            licenseTagView.setText(R.string.license_info_not_readable);
        }

        setBitmap(publicBitmap);
    }

    /**
     * Fetch bitmap from device local location, if it exists, and set the photo view.
     */
    private void setLocalBitmap(final Upload upload) {
        setPictureButtonsEnabled(true);

        final Bitmap showBitmap = checkForLocalPhoto(upload);
        setButtonEnabled(uploadPhotoButton, false);
        if (showBitmap == null) {
            // lokal keine Bitmap
            localFotoUsed = false;
            // switch off image and license view until we actually have a foto
            imageView.setVisibility(View.INVISIBLE);
            licenseTagView.setVisibility(View.INVISIBLE);
        } else {
            setBitmap(showBitmap);
            fetchUploadStatus(upload);
        }
    }

    private void fetchUploadStatus(final Upload upload) {
        if (upload == null) {
            return;
        }
        setButtonEnabled(uploadPhotoButton, true);
        final List<LocalPhoto> localPhotos = new ArrayList<>();
        final LocalPhoto localPhoto = new LocalPhoto(getStoredMediaFile(upload));
        if (station != null) {
            localPhoto.setCountryCode(station.getCountry());
            localPhoto.setId(station.getId());
        } else {
            localPhoto.setLat(latitude);
            localPhoto.setLon(longitude);
        }
        localPhotos.add(localPhoto);

        baseApplication.getRSAPI().queryUploadState(RSAPI.Helper.getAuthorizationHeader(baseApplication.getEmail(), baseApplication.getPassword()), localPhotos).enqueue(new Callback<List<UploadStateQuery>>() {
            @Override
            public void onResponse(final Call<List<UploadStateQuery>> call, final Response<List<UploadStateQuery>> response) {
                final List<UploadStateQuery> stateQueries = response.body();
                if (stateQueries != null && stateQueries.size() == 1) {
                    final UploadStateQuery stateQuery = stateQueries.get(0);
                    licenseTagView.setText(getString(R.string.upload_state, getString(stateQuery.getState().getTextId())));
                    licenseTagView.setTextColor(getResources().getColor(stateQuery.getState().getColorId()));
                    licenseTagView.setVisibility(View.VISIBLE);
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(final Call<List<UploadStateQuery>> call, final Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(DetailsActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void setPictureButtonsEnabled(final boolean enabled) {
        setButtonEnabled(takePictureButton, enabled);
        setButtonEnabled(selectPictureButton, enabled);
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
            imageView.setImageBitmap(scaledBitmap);
        } else {
            imageView.setImageBitmap(showBitmap);
        }
        imageView.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
    }

    private class AnimationUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
            final float alpha = (float) animation.getAnimatedValue();
            if (header == null) {
                etBahnhofName.setAlpha(alpha);
            } else {
                header.setAlpha(alpha);
            }
            licenseTagView.setAlpha(alpha);
            buttons.setAlpha(alpha);
        }
    }

    /**
     * Check if there's a local photo file for this station.
     *
     * @return the Bitmap of the photo, or null if none exists.
     */
    @Nullable
    private Bitmap checkForLocalPhoto(Upload upload) {
        // show the image
        final File localFile = getStoredMediaFile(upload);
        Log.d(TAG, "File: " + localFile);
        if (localFile != null && localFile.canRead()) {
            Log.d(TAG, "FileGetPath: " + localFile.getPath());
            final Bitmap scaledScreen = BitmapFactory.decodeFile(localFile.getPath());
            Log.d(TAG, "img width " + scaledScreen.getWidth());
            Log.d(TAG, "img height " + scaledScreen.getHeight());
            localFotoUsed = true;
            setButtonEnabled(uploadPhotoButton, true);
            return scaledScreen;
        } else {
            localFotoUsed = false;
            setButtonEnabled(uploadPhotoButton, false);
            Log.e(TAG, String.format("Media file not available for station %s", bahnhofId));
            return null;
        }
    }

    public void onPictureClicked() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !fullscreen) {
            final ValueAnimator animation = ValueAnimator.ofFloat(header.getAlpha(), 0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            detailsLayout.setSystemUiVisibility(
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
            final ValueAnimator animation = ValueAnimator.ofFloat(header == null? etBahnhofName.getAlpha() : header.getAlpha(), 1.0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            detailsLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
