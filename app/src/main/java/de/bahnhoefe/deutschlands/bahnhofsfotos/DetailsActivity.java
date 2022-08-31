package de.bahnhoefe.deutschlands.bahnhofsfotos;

import static android.content.Intent.createChooser;
import static android.graphics.Color.WHITE;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityDetailsBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ReportProblemBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.StationInfoBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.UploadBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
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

public class DetailsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, BitmapAvailableHandler {

    private static final String TAG = DetailsActivity.class.getSimpleName();

    // Names of Extras that this class reacts to
    public static final String EXTRA_TAKE_FOTO = "DetailsActivityTakeFoto";
    public static final String EXTRA_UPLOAD = "upload";
    public static final String EXTRA_STATION = "station";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private static final int ALPHA = 128;

    private static final String LINK_FORMAT = "<b><a href=\"%s\">%s</a></b>";

    private BaseApplication baseApplication;
    private RSAPIClient rsapiClient;

    private ActivityDetailsBinding binding;

    private Upload upload;
    private Station station;
    private Set<Country> countries;
    private boolean localFotoUsed = false;
    private String nickname;
    private boolean fullscreen;
    private Bitmap publicBitmap;
    private Double latitude;
    private Double longitude;
    private String bahnhofId;
    private Long crc32 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        baseApplication = (BaseApplication) getApplication();
        rsapiClient = baseApplication.getRsapiClient();
        var dbAdapter = baseApplication.getDbAdapter();
        var countryCodes = baseApplication.getCountryCodes();
        countries = dbAdapter.fetchCountriesWithProviderApps(countryCodes);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

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
                    if (isNotLoggedIn()) {
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
    protected void onNewIntent(Intent intent) {
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

            binding.details.marker.setImageDrawable(ContextCompat.getDrawable(this, getMarkerRes()));

            directPicture = intent.getBooleanExtra(EXTRA_TAKE_FOTO, false);
            if (station != null) {
                bahnhofId = station.getId();
                binding.details.etbahnhofname.setText(station.getTitle());
                binding.details.etbahnhofname.setInputType(EditorInfo.TYPE_NULL);
                binding.details.etbahnhofname.setSingleLine(false);

                if (upload == null) {
                    upload = baseApplication.getDbAdapter().getPendingUploadForStation(station);
                }

                if (station.hasPhoto()) {
                    if (ConnectionUtil.checkInternetConnection(this)) {
                        BitmapCache.getInstance().getFoto(this, station.getPhotoUrl());
                    }
                    setPictureButtonsEnabled(canSetPhoto());

                    // check for local photo
                    var localFile = getStoredMediaFile(upload);
                    if (localFile != null && localFile.canRead()) {
                        SimpleDialogs.confirm(this, R.string.local_photo_exists, (dialog, which) -> {
                            setPictureButtonsEnabled(true);
                            setLocalBitmap(upload);
                        });
                    }
                } else if (upload != null && upload.isPendingPhotoUpload()) {
                    setLocalBitmap(upload);
                } else {
                    setLocalBitmap(upload);
                }
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

    private int getMarkerRes() {
        if (station == null) {
            return R.drawable.marker_missing;
        }
        if (station.hasPhoto()) {
            if (isOwner()) {
                return station.isActive() ? R.drawable.marker_violet : R.drawable.marker_violet_inactive;
            } else {
                return station.isActive() ? R.drawable.marker_green : R.drawable.marker_green_inactive;
            }
        } else if (upload != null && upload.isPendingPhotoUpload()) {
            return R.drawable.marker_yellow;
        } else {
            return station.isActive() ? R.drawable.marker_red : R.drawable.marker_red_inactive;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readPreferences();
    }

    private void readPreferences() {
        nickname = baseApplication.getNickname();
    }

    private boolean canSetPhoto() {
        return station == null || !station.hasPhoto() || isOwner();
    }

    private boolean isOwner() {
        return station != null && TextUtils.equals(nickname, station.getPhotographer());
    }

    private boolean isNotLoggedIn() {
        return !rsapiClient.hasCredentials();
    }

    private final ActivityResultLauncher<Intent> imageCaptureResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        verifyCurrentPhotoUploadExists();
                        var storagePictureFile = getStoredMediaFile(upload);
                        // die Kamera-App sollte auf temporären Cache-Speicher schreiben, wir laden das Bild von
                        // dort und schreiben es in Standard-Größe in den permanenten Speicher
                        var cameraRawPictureFile = getCameraTempFile();
                        var options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true; // just query the image size in the first step
                        BitmapFactory.decodeFile(cameraRawPictureFile.getPath(), options);

                        int sampling = options.outWidth / Constants.STORED_PHOTO_WIDTH;
                        if (sampling > 1) {
                            options.inSampleSize = sampling;
                        }
                        options.inJustDecodeBounds = false;

                        saveScaledBitmap(storagePictureFile, BitmapFactory.decodeFile(cameraRawPictureFile.getPath(), options));
                        setLocalBitmap(upload);
                        FileUtils.deleteQuietly(cameraRawPictureFile);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing photo", e);
                        Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

    void takePicture() {
        if (!canSetPhoto() || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return;
        }

        if (isNotLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
        }

        verifyCurrentPhotoUploadExists();
        var photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", getCameraTempFile());
        var intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, getResources().getString(R.string.app_name));
        intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, binding.details.etbahnhofname.getText());
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            imageCaptureResultLauncher.launch(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.no_image_capture_app_found, Toast.LENGTH_LONG).show();
        }
    }

    private final ActivityResultLauncher<String> selectPictureResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                try {
                    verifyCurrentPhotoUploadExists();
                    var bitmap = getBitmapFromUri(uri);
                    int sampling = bitmap.getWidth() / Constants.STORED_PHOTO_WIDTH;
                    var scaledScreen = bitmap;
                    if (sampling > 1) {
                        scaledScreen = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / sampling, bitmap.getHeight() / sampling, false);
                    }

                    saveScaledBitmap(getStoredMediaFile(upload), scaledScreen);
                    setLocalBitmap(upload);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing photo", e);
                    Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });


    void selectPicture() {
        if (!canSetPhoto()) {
            return;
        }

        if (isNotLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            return;
        }

        selectPictureResultLauncher.launch("image/*");
    }

    void reportProblem() {
        if (isNotLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            return;
        }

        var reportProblemBinding = ReportProblemBinding.inflate(getLayoutInflater());
        if (upload != null && upload.isProblemReport()) {
            reportProblemBinding.etProblemComment.setText(upload.getComment());
        }

        var problemTypes = new ArrayList<String>();
        problemTypes.add(getString(R.string.problem_please_specify));
        int selected = -1;
        for (var type : ProblemType.values()) {
            problemTypes.add(getString(type.getMessageId()));
            if (upload != null && upload.getProblemType() == type) {
                selected = problemTypes.size() - 1;
            }
        }
        var adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, problemTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportProblemBinding.problemType.setAdapter(adapter);
        if (selected > -1) {
            reportProblemBinding.problemType.setSelection(selected);
        }

        var builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.report_problem)
                .setView(reportProblemBinding.getRoot())
                .setIcon(R.drawable.ic_bullhorn_48px)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        var alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int selectedType = reportProblemBinding.problemType.getSelectedItemPosition();
            if (selectedType == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.problem_please_specify), Toast.LENGTH_LONG).show();
                return;
            }
            var type = ProblemType.values()[selectedType - 1];
            var comment = reportProblemBinding.etProblemComment.getText().toString();
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

            rsapiClient.reportProblem(new ProblemReport(station.getCountry(), bahnhofId, comment, type)).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<InboxResponse> call, @NonNull Response<InboxResponse> response) {
                    InboxResponse inboxResponse;
                    if (response.isSuccessful()) {
                        inboxResponse = response.body();
                    } else if (response.code() == 401) {
                        SimpleDialogs.confirm(DetailsActivity.this, R.string.authorization_failed);
                        return;
                    } else {
                        Gson gson = new Gson();
                        inboxResponse = gson.fromJson(response.errorBody().charStream(), InboxResponse.class);
                        if (inboxResponse.getState() == null) {
                            inboxResponse.setState(InboxResponse.InboxResponseState.ERROR);
                        }
                    }

                    upload.setRemoteId(inboxResponse.getId());
                    upload.setUploadState(inboxResponse.getState().getUploadState());
                    baseApplication.getDbAdapter().updateUpload(upload);
                    if (inboxResponse.getState() == InboxResponse.InboxResponseState.ERROR) {
                        SimpleDialogs.confirm(DetailsActivity.this,
                                String.format(getText(R.string.problem_report_failed).toString(), response.message()));
                        fetchUploadStatus(upload); // try to get the upload state again
                    } else {
                        SimpleDialogs.confirm(DetailsActivity.this, inboxResponse.getState().getMessageId());
                    }

                }

                @Override
                public void onFailure(@NonNull Call<InboxResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error reporting problem", t);
                    SimpleDialogs.confirm(DetailsActivity.this,
                            String.format(getText(R.string.problem_report_failed).toString(), t.getMessage()));
                }
            });
        });
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

    private void saveScaledBitmap(File storagePictureFile, Bitmap scaledScreen) throws FileNotFoundException {
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

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        Bitmap image;
        try (ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")) {
            image = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor.getFileDescriptor());
        }
        return image;
    }

    /**
     * Get the file path for storing this stations foto
     *
     * @return the File
     */
    @Nullable
    public File getStoredMediaFile(Upload upload) {
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.details, menu);
        var navToStation = menu.findItem(R.id.nav_to_station);
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
            hasProviderApps = Country.getCountryByCode(countries, station.getCountry()).hasCompatibleProviderApps();
        }
        if (hasProviderApps) {
            enableMenuItem(menu, R.id.provider_android_app);
        } else {
            disableMenuItem(menu, R.id.provider_android_app);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void enableMenuItem(Menu menu, int id) {
        var menuItem = menu.findItem(id).setEnabled(true);
        menuItem.getIcon().mutate();
        menuItem.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);
    }

    private void disableMenuItem(Menu menu, int id) {
        var menuItem = menu.findItem(id).setEnabled(false);
        menuItem.getIcon().mutate();
        menuItem.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);
        menuItem.getIcon().setAlpha(ALPHA);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_to_station) {
            startNavigation(DetailsActivity.this);
        } else if (itemId == R.id.timetable) {
            var timetableIntent = new Timetable().createTimetableIntent(Country.getCountryByCode(countries, station.getCountry()), station);
            if (timetableIntent != null) {
                startActivity(timetableIntent);
            } else {
                Toast.makeText(this, R.string.timetable_missing, Toast.LENGTH_LONG).show();
            }
        } else if (itemId == R.id.share_photo) {
            var shareIntent = createFotoSendIntent();
            shareIntent.putExtra(Intent.EXTRA_TEXT, Country.getCountryByCode(countries, station != null ? station.getCountry() : null).getTwitterTags() + " " + binding.details.etbahnhofname.getText());
            shareIntent.setType("image/jpeg");
            startActivity(createChooser(shareIntent, "send"));
        } else if (itemId == R.id.station_info) {
            showStationInfo(null);
        } else if (itemId == R.id.provider_android_app) {
            var providerApps = Country.getCountryByCode(countries, station.getCountry()).getCompatibleProviderApps();
            if (providerApps.size() == 1) {
                providerApps.get(0).openAppOrPlayStore(this);
            } else if (providerApps.size() > 1) {
                var appNames = providerApps.stream()
                        .map(ProviderApp::getName).toArray(CharSequence[]::new);
                SimpleDialogs.simpleSelect(this, getResources().getString(R.string.choose_provider_app), appNames, (dialog, which) -> {
                    if (which >= 0 && providerApps.size() > which) {
                        providerApps.get(which).openAppOrPlayStore(DetailsActivity.this);
                    }
                });
            } else {
                Toast.makeText(this, R.string.provider_app_missing, Toast.LENGTH_LONG).show();
            }
        } else if (itemId == android.R.id.home) {
            navigateUp();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        navigateUp();
    }

    public void navigateUp() {
        var callingActivity = getCallingActivity(); // if MapsActivity was calling, then we don't want to rebuild the Backstack
        if (callingActivity == null) {
            var upIntent = NavUtils.getParentActivityIntent(this);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
                Log.v(TAG, "Recreate back stack");
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
            }
        }

        finish();
    }

    public void showStationInfo(View view) {
        var stationInfoBinding = StationInfoBinding.inflate(getLayoutInflater());
        stationInfoBinding.id.setText(station != null ? station.getId() : "");

        double lat = station != null ? station.getLat() : latitude;
        double lon = station != null ? station.getLon() : longitude;
        stationInfoBinding.coordinates.setText(String.format(Locale.US, getResources().getString(R.string.coordinates), lat, lon));

        stationInfoBinding.active.setText(station != null && station.isActive() ? R.string.active : R.string.inactive);
        stationInfoBinding.owner.setText(station != null && station.getPhotographer() != null ? station.getPhotographer() : "");
        if (station.isOutdated()) {
            stationInfoBinding.outdatedLabel.setVisibility(View.VISIBLE);
        }

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                .setTitle(binding.details.etbahnhofname.getText())
                .setView(stationInfoBinding.getRoot())
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private void uploadPhoto() {
        var uploadBinding = UploadBinding.inflate(getLayoutInflater());
        uploadBinding.etComment.setText(upload.getComment());

        uploadBinding.spActive.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.active_flag_options)));
        if (station != null) {
            uploadBinding.spActive.setVisibility(View.GONE);
        } else {
            if (upload.getActive() == null) {
                uploadBinding.spActive.setSelection(0);
            } else if (upload.getActive()) {
                uploadBinding.spActive.setSelection(1);
            } else {
                uploadBinding.spActive.setSelection(2);
            }
        }

        var sameChecksum = crc32 != null && crc32.equals(upload.getCrc32());
        uploadBinding.cbChecksum.setVisibility(sameChecksum ? View.VISIBLE : View.GONE);

        if (station != null) {
            uploadBinding.spCountries.setVisibility(View.GONE);
        } else {
            var countryList = baseApplication.getDbAdapter().getAllCountries();
            var items = new KeyValueSpinnerItem[countryList.size() + 1];
            items[0] = new KeyValueSpinnerItem(getString(R.string.chooseCountry), "");
            int selected = 0;

            for (int i = 0; i < countryList.size(); i++) {
                var country = countryList.get(i);
                items[i + 1] = new KeyValueSpinnerItem(country.getName(), country.getCode());
                if (country.getCode().equals(upload.getCountry())) {
                    selected = i + 1;
                }
            }
            var countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            uploadBinding.spCountries.setAdapter(countryAdapter);
            uploadBinding.spCountries.setSelection(selected);
        }

        uploadBinding.txtPanorama.setText(Html.fromHtml(getString(R.string.panorama_info), Html.FROM_HTML_MODE_COMPACT));
        uploadBinding.txtPanorama.setMovementMethod(LinkMovementMethod.getInstance());
        uploadBinding.txtPanorama.setLinkTextColor(Color.parseColor("#c71c4d"));

        String overrideLicense = null;
        if (station != null) {
            var country = Country.getCountryByCode(countries, station.getCountry());
            overrideLicense = country.getOverrideLicense();
        }
        if (overrideLicense != null) {
            uploadBinding.cbSpecialLicense.setText(getString(R.string.special_license, overrideLicense));
        }
        uploadBinding.cbSpecialLicense.setVisibility(overrideLicense == null ? View.GONE : View.VISIBLE);

        var builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.photo_upload)
                .setView(uploadBinding.getRoot())
                .setIcon(R.drawable.ic_bullhorn_48px)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        var alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (uploadBinding.cbSpecialLicense.getText().length() > 0 && !uploadBinding.cbSpecialLicense.isChecked()) {
                Toast.makeText(this, R.string.special_license_confirm, Toast.LENGTH_LONG).show();
                return;
            }
            if (sameChecksum && !uploadBinding.cbChecksum.isChecked()) {
                Toast.makeText(this, R.string.photo_checksum, Toast.LENGTH_LONG).show();
                return;
            }
            if (station == null) {
                if (uploadBinding.spActive.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, R.string.active_flag_choose, Toast.LENGTH_LONG).show();
                    return;
                }
                var selectedCountry = (KeyValueSpinnerItem) uploadBinding.spCountries.getSelectedItem();
                upload.setCountry(selectedCountry.getValue());
            }
            alertDialog.dismiss();

            binding.details.progressBar.setVisibility(View.VISIBLE);

            var stationTitle = binding.details.etbahnhofname.getText().toString();
            var comment = uploadBinding.etComment.getText().toString();
            upload.setTitle(stationTitle);
            upload.setComment(comment);

            try {
                stationTitle = URLEncoder.encode(binding.details.etbahnhofname.getText().toString(), String.valueOf(StandardCharsets.UTF_8));
                comment = URLEncoder.encode(comment, String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding station title or comment", e);
            }
            upload.setActive(uploadBinding.spActive.getSelectedItemPosition() == 1);
            baseApplication.getDbAdapter().updateUpload(upload);

            var mediaFile = getStoredMediaFile(upload);
            assert mediaFile != null;
            var file = RequestBody.create(mediaFile, MediaType.parse(URLConnection.guessContentTypeFromName(mediaFile.getName())));
            rsapiClient.photoUpload(bahnhofId, station != null ? station.getCountry() : upload.getCountry(),
                    stationTitle, latitude, longitude, comment, upload.getActive(), file).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<InboxResponse> call, @NonNull Response<InboxResponse> response) {
                    binding.details.progressBar.setVisibility(View.GONE);
                    InboxResponse inboxResponse;
                    if (response.isSuccessful()) {
                        inboxResponse = response.body();
                    } else if (response.code() == 401) {
                        SimpleDialogs.confirm(DetailsActivity.this, R.string.authorization_failed);
                        return;
                    } else {
                        assert response.errorBody() != null;
                        var gson = new Gson();
                        inboxResponse = gson.fromJson(response.errorBody().charStream(), InboxResponse.class);
                        if (inboxResponse.getState() == null) {
                            inboxResponse.setState(InboxResponse.InboxResponseState.ERROR);
                        }
                    }

                    assert inboxResponse != null;
                    upload.setRemoteId(inboxResponse.getId());
                    upload.setInboxUrl(inboxResponse.getInboxUrl());
                    upload.setUploadState(inboxResponse.getState().getUploadState());
                    upload.setCrc32(inboxResponse.getCrc32());
                    baseApplication.getDbAdapter().updateUpload(upload);
                    if (inboxResponse.getState() == InboxResponse.InboxResponseState.ERROR) {
                        SimpleDialogs.confirm(DetailsActivity.this,
                                String.format(getText(InboxResponse.InboxResponseState.ERROR.getMessageId()).toString(), response.message()));
                        fetchUploadStatus(upload); // try to get the upload state again
                    } else {
                        SimpleDialogs.confirm(DetailsActivity.this, inboxResponse.getState().getMessageId());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<InboxResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error uploading photo", t);
                    binding.details.progressBar.setVisibility(View.GONE);

                    SimpleDialogs.confirm(DetailsActivity.this,
                            String.format(getText(InboxResponse.InboxResponseState.ERROR.getMessageId()).toString(), t.getMessage()));
                    fetchUploadStatus(upload); // try to get the upload state again
                }
            });
        });
    }

    private Intent createFotoSendIntent() {
        var sendIntent = new Intent(Intent.ACTION_SEND);
        var file = getStoredMediaFile(upload);
        if (file != null && file.canRead()) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                    BuildConfig.APPLICATION_ID + ".fileprovider", file));
        } else if (publicBitmap != null) {
            var newFile = FileUtils.getImageCacheFile(getApplicationContext(), String.valueOf(System.currentTimeMillis()));
            try {
                saveScaledBitmap(newFile, publicBitmap);
                sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileprovider", newFile));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error saving cached bitmap", e);
            }
        }
        return sendIntent;
    }

    private void startNavigation(Context context) {
        var adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item,
                android.R.id.text1, NavItem.values()) {
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                var item = getItem(position);
                assert item != null;

                var v = super.getView(position, convertView, parent);
                TextView tv = v.findViewById(android.R.id.text1);

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(item.getIconRes(), 0, 0, 0);
                tv.setText(getString(item.getTextRes()));

                //Add margin between image and text (support various screen densities)
                int dp5 = (int) (20 * getResources().getDisplayMetrics().density + 0.5f);
                int dp7 = (int) (20 * getResources().getDisplayMetrics().density);
                tv.setCompoundDrawablePadding(dp5);
                tv.setPadding(dp7, 0, 0, 0);

                return v;
            }
        };

        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.navMethod)
                .setAdapter(adapter, (dialog, position) -> {
                    var item = adapter.getItem(position);
                    assert item != null;
                    var lat = station != null ? station.getLat() : latitude;
                    var lon = station != null ? station.getLon() : longitude;
                    var intent = item.createIntent(DetailsActivity.this, lat, lon, binding.details.etbahnhofname.getText().toString(), getMarkerRes());
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, R.string.activitynotfound, Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmapFromCache the fetched Bitmap for the notification. May be null
     */
    @Override
    public void onBitmapAvailable(@Nullable Bitmap bitmapFromCache) {
        runOnUiThread(() -> {
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
                boolean photographerUrlAvailable = station.getPhotographerUrl() != null && !station.getPhotographerUrl().isEmpty();
                boolean licenseUrlAvailable = station.getLicenseUrl() != null && !station.getLicenseUrl().isEmpty();

                String photographerText;
                if (photographerUrlAvailable) {
                    photographerText = String.format(
                            LINK_FORMAT,
                            station.getPhotographerUrl(),
                            station.getPhotographer());
                } else {
                    photographerText = station.getPhotographer();
                }

                String licenseText;
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
                                        licenseText), Html.FROM_HTML_MODE_LEGACY

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
    private void setLocalBitmap(Upload upload) {
        setPictureButtonsEnabled(true);

        var showBitmap = checkForLocalPhoto(upload);
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

    private void fetchUploadStatus(Upload upload) {
        if (upload == null) {
            return;
        }
        setButtonEnabled(binding.details.buttonUpload, true);
        var stateQueries = new ArrayList<InboxStateQuery>();
        stateQueries.add(new InboxStateQuery(upload.getRemoteId(), upload.getCountry(), upload.getStationId()));

        rsapiClient.queryUploadState(stateQueries).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<InboxStateQuery>> call, @NonNull Response<List<InboxStateQuery>> response) {
                var stateQueries = response.body();
                if (stateQueries != null && !stateQueries.isEmpty()) {
                    var stateQuery = stateQueries.get(0);
                    binding.details.licenseTag.setText(getString(R.string.upload_state, getString(stateQuery.getState().getTextId())));
                    binding.details.licenseTag.setTextColor(getResources().getColor(stateQuery.getState().getColorId(), null));
                    binding.details.licenseTag.setVisibility(View.VISIBLE);
                    upload.setUploadState(stateQuery.getState());
                    upload.setRejectReason(stateQuery.getRejectedReason());
                    upload.setCrc32(stateQuery.getCrc32());
                    upload.setRemoteId(stateQuery.getId());
                    baseApplication.getDbAdapter().updateUpload(upload);
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<InboxStateQuery>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(DetailsActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void setPictureButtonsEnabled(boolean enabled) {
        setButtonEnabled(binding.details.buttonTakePicture, enabled);
        setButtonEnabled(binding.details.buttonSelectPicture, enabled);
    }

    private void setButtonEnabled(ImageButton imageButton, boolean enabled) {
        imageButton.setEnabled(enabled);
        imageButton.setImageAlpha(enabled ? 255 : 125);
    }

    private void setBitmap(Bitmap showBitmap) {
        var size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int targetWidth = size.x;

        if (showBitmap.getWidth() != targetWidth) {
            var scaledBitmap = Bitmap.createScaledBitmap(showBitmap,
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
        public void onAnimationUpdate(ValueAnimator animation) {
            float alpha = (float) animation.getAnimatedValue();
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
    private Bitmap checkForLocalPhoto(Upload upload) {
        // show the image
        var localFile = getStoredMediaFile(upload);
        Log.d(TAG, "File: " + localFile);
        crc32 = null;
        if (localFile != null && localFile.canRead()) {
            Log.d(TAG, "FileGetPath: " + localFile.getPath());
            try (var cis = new CheckedInputStream(new FileInputStream(localFile), new CRC32())) {
                var scaledScreen = BitmapFactory.decodeStream(cis);
                crc32 = cis.getChecksum().getValue();
                Log.d(TAG, "img width " + scaledScreen.getWidth() + ", height " + scaledScreen.getHeight() + ", crc32 " + crc32);
                localFotoUsed = true;
                setButtonEnabled(binding.details.buttonUpload, true);
                return scaledScreen;
            } catch (Exception e) {
                Log.e(TAG, String.format("Error reading media file for station %s", bahnhofId), e);
            }
        } else {
            localFotoUsed = false;
            setButtonEnabled(binding.details.buttonUpload, false);
            Log.e(TAG, String.format("Media file not available for station %s", bahnhofId));
        }
        return null;
    }

    public void onPictureClicked() {
        if (!fullscreen) {
            var animation = ValueAnimator.ofFloat(binding.details.header.getAlpha(), 0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            binding.details.contentDetails.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
            var bar = getActionBar();
            if (bar != null) {
                bar.hide();
            }
            var sbar = getSupportActionBar();
            if (sbar != null) {
                sbar.hide();
            }
            fullscreen = true;
        } else {
            var animation = ValueAnimator.ofFloat(binding.details.header.getAlpha(), 1.0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            binding.details.contentDetails.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            var bar = getActionBar();
            if (bar != null) {
                bar.show();
            }
            var sbar = getSupportActionBar();
            if (sbar != null) {
                sbar.show();
            }
            fullscreen = false;
        }

    }

}
