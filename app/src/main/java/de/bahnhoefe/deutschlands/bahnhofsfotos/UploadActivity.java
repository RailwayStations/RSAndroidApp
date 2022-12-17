package de.bahnhoefe.deutschlands.bahnhofsfotos;

import static android.content.Intent.createChooser;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityUploadBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.KeyValueSpinnerItem;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = UploadActivity.class.getSimpleName();

    // Names of Extras that this class reacts to
    public static final String EXTRA_UPLOAD = "EXTRA_UPLOAD";
    public static final String EXTRA_STATION = "EXTRA_STATION";
    public static final String EXTRA_LATITUDE = "EXTRA_LATITUDE";
    public static final String EXTRA_LONGITUDE = "EXTRA_LONGITUDE";

    private BaseApplication baseApplication;
    private RSAPIClient rsapiClient;

    private ActivityUploadBinding binding;

    private Upload upload;
    private Station station;
    private List<Country> countries;
    private Double latitude;
    private Double longitude;
    private String bahnhofId;
    private Long crc32 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        baseApplication = (BaseApplication) getApplication();
        rsapiClient = baseApplication.getRsapiClient();
        countries = new ArrayList<>(baseApplication.getDbAdapter().getAllCountries());

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

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
                Log.w(TAG, "EXTRA_STATION and EXTRA_LATITUDE or EXTRA_LONGITUDE in intent data missing");
                Toast.makeText(this, R.string.station_or_coords_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (station != null) {
                bahnhofId = station.getId();
                binding.upload.etStationTitle.setText(station.getTitle());
                binding.upload.etStationTitle.setInputType(EditorInfo.TYPE_NULL);
                binding.upload.etStationTitle.setSingleLine(false);

                if (upload == null) {
                    upload = baseApplication.getDbAdapter().getPendingUploadForStation(station);
                }

                setLocalBitmap(upload);

                binding.upload.spActive.setVisibility(View.GONE);
                binding.upload.spCountries.setVisibility(View.GONE);

                String country = station.getCountry();
                updateOverrideLicense(country);
            } else {
                if (upload == null) {
                    upload = baseApplication.getDbAdapter().getPendingUploadForCoordinates(latitude, longitude);
                }

                binding.upload.etStationTitle.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                binding.upload.spActive.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.active_flag_options)));
                if (upload != null) {
                    binding.upload.etStationTitle.setText(upload.getTitle());
                    setLocalBitmap(upload);

                    if (upload.getActive() == null) {
                        binding.upload.spActive.setSelection(0);
                    } else if (upload.getActive()) {
                        binding.upload.spActive.setSelection(1);
                    } else {
                        binding.upload.spActive.setSelection(2);
                    }
                } else {
                    binding.upload.spActive.setSelection(0);
                }

                var items = new KeyValueSpinnerItem[countries.size() + 1];
                items[0] = new KeyValueSpinnerItem(getString(R.string.chooseCountry), "");
                int selected = 0;

                for (int i = 0; i < countries.size(); i++) {
                    var country = countries.get(i);
                    items[i + 1] = new KeyValueSpinnerItem(country.getName(), country.getCode());
                    if (upload != null && country.getCode().equals(upload.getCountry())) {
                        selected = i + 1;
                    }
                }
                var countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
                countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.upload.spCountries.setAdapter(countryAdapter);
                binding.upload.spCountries.setSelection(selected);
                binding.upload.spCountries.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                        var selectedCountry = (KeyValueSpinnerItem) parent.getItemAtPosition(position);
                        updateOverrideLicense(selectedCountry.getValue());
                    }

                    @Override
                    public void onNothingSelected(final AdapterView<?> parent) {
                        updateOverrideLicense(null);
                    }
                });
            }

            if (upload != null) {
                binding.upload.etComment.setText(upload.getComment());
            }

            binding.upload.txtPanorama.setText(Html.fromHtml(getString(R.string.panorama_info), Html.FROM_HTML_MODE_COMPACT));
            binding.upload.txtPanorama.setMovementMethod(LinkMovementMethod.getInstance());
            binding.upload.txtPanorama.setLinkTextColor(Color.parseColor("#c71c4d"));

        }
    }

    private void updateOverrideLicense(final String country) {
        var overrideLicense = Country.getCountryByCode(countries, country).map(Country::getOverrideLicense).orElse(null);
        if (overrideLicense != null) {
            binding.upload.cbSpecialLicense.setText(getString(R.string.special_license, overrideLicense));
        }
        binding.upload.cbSpecialLicense.setVisibility(overrideLicense == null ? View.GONE : View.VISIBLE);
    }

    private boolean isNotLoggedIn() {
        return !rsapiClient.hasCredentials();
    }

    private final ActivityResultLauncher<Intent> imageCaptureResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        assertCurrentPhotoUploadExists();
                        var cameraTempFile = getCameraTempFile();
                        var options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true; // just query the image size in the first step
                        BitmapFactory.decodeFile(cameraTempFile.getPath(), options);

                        int sampling = options.outWidth / Constants.STORED_PHOTO_WIDTH;
                        if (sampling > 1) {
                            options.inSampleSize = sampling;
                        }
                        options.inJustDecodeBounds = false;

                        storeBitmapToLocalFile(getStoredMediaFile(upload), BitmapFactory.decodeFile(cameraTempFile.getPath(), options));
                        FileUtils.deleteQuietly(cameraTempFile);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing photo", e);
                        Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

    public void takePicture(View view) {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return;
        }

        if (isNotLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            startActivity(new Intent(UploadActivity.this, MyDataActivity.class));
            finish();
            return;
        }

        assertCurrentPhotoUploadExists();
        var photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", getCameraTempFile());
        var intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, getResources().getString(R.string.app_name));
        intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, binding.upload.etStationTitle.getText());
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            imageCaptureResultLauncher.launch(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.no_image_capture_app_found, Toast.LENGTH_LONG).show();
        }
    }

    private final ActivityResultLauncher<String> selectPictureResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                try (var parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")) {
                    if (parcelFileDescriptor == null) {
                        return;
                    }
                    var fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    assertCurrentPhotoUploadExists();

                    var bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    int sampling = bitmap.getWidth() / Constants.STORED_PHOTO_WIDTH;
                    var scaledScreen = bitmap;
                    if (sampling > 1) {
                        scaledScreen = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / sampling, bitmap.getHeight() / sampling, false);
                    }

                    storeBitmapToLocalFile(getStoredMediaFile(upload), scaledScreen);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing photo", e);
                    Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

    public void selectPicture(View view) {
        if (isNotLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            startActivity(new Intent(UploadActivity.this, MyDataActivity.class));
            finish();
            return;
        }

        selectPictureResultLauncher.launch("image/*");
    }

    private void assertCurrentPhotoUploadExists() {
        if (upload == null || upload.isProblemReport() || upload.isUploaded()) {
            upload = Upload.builder()
                    .country(station != null ? station.getCountry() : null)
                    .stationId(station != null ? station.getId() : null)
                    .lat(latitude)
                    .lon(longitude)
                    .build();
            upload = baseApplication.getDbAdapter().insertUpload(upload);
        }
    }

    private void storeBitmapToLocalFile(File file, Bitmap bitmap) throws IOException {
        if (bitmap == null) {
            throw new RuntimeException(getString(R.string.error_scaling_photo));
        }
        Log.i(TAG, "Save photo with width=" + bitmap.getWidth() + " and height=" + bitmap.getHeight() + " to: " + file);
        try (var cos = new CheckedOutputStream(new FileOutputStream(file), new CRC32())) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.STORED_PHOTO_QUALITY, cos);
            crc32 = cos.getChecksum().getValue();
            setLocalBitmap(upload);
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.share_photo) {
            var twitterTags = station != null ? Country.getCountryByCode(countries, station.getCountry()).map(Country::getTwitterTags).orElse("") : "";
            var shareIntent = createPhotoSendIntent();
            if (shareIntent != null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, binding.upload.etStationTitle.getText() + " " + twitterTags);
                shareIntent.setType("image/jpeg");
                startActivity(createChooser(shareIntent, getString(R.string.share_photo)));
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
        var upIntent = NavUtils.getParentActivityIntent(this);
        if (callingActivity == null && upIntent != null) {
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
                Log.v(TAG, "Recreate back stack");
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
            }
        }

        finish();
    }

    public void upload(View view) {
        if (isNotLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            startActivity(new Intent(UploadActivity.this, MyDataActivity.class));
            finish();
            return;
        } else if (TextUtils.isEmpty(binding.upload.etStationTitle.getText())) {
            Toast.makeText(this, R.string.station_title_needed, Toast.LENGTH_LONG).show();
            return;
        }

        assertCurrentPhotoUploadExists();

        var mediaFile = getStoredMediaFile(upload);
        assert mediaFile != null;

        if (!mediaFile.exists()) {
            if (station != null) {
                Toast.makeText(this, R.string.please_take_photo, Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (binding.upload.cbSpecialLicense.getText().length() > 0 && !binding.upload.cbSpecialLicense.isChecked()) {
            Toast.makeText(this, R.string.special_license_confirm, Toast.LENGTH_LONG).show();
            return;
        }
        if (crc32 != null && upload != null && crc32.equals(upload.getCrc32()) && !binding.upload.cbChecksum.isChecked()) {
            Toast.makeText(this, R.string.photo_checksum, Toast.LENGTH_LONG).show();
            return;
        }
        if (station == null) {
            if (binding.upload.spActive.getSelectedItemPosition() == 0) {
                Toast.makeText(this, R.string.active_flag_choose, Toast.LENGTH_LONG).show();
                return;
            }
            var selectedCountry = (KeyValueSpinnerItem) binding.upload.spCountries.getSelectedItem();
            upload.setCountry(selectedCountry.getValue());
        }

        SimpleDialogs.confirm(this, station != null ? R.string.photo_upload : R.string.report_missing_station, (dialog, which) -> {
            binding.upload.progressBar.setVisibility(View.VISIBLE);

            var stationTitle = binding.upload.etStationTitle.getText().toString();
            var comment = binding.upload.etComment.getText().toString();
            upload.setTitle(stationTitle);
            upload.setComment(comment);

            try {
                stationTitle = URLEncoder.encode(binding.upload.etStationTitle.getText().toString(), String.valueOf(StandardCharsets.UTF_8));
                comment = URLEncoder.encode(comment, String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding station title or comment", e);
            }
            upload.setActive(binding.upload.spActive.getSelectedItemPosition() == 1);
            baseApplication.getDbAdapter().updateUpload(upload);

            var file = mediaFile.exists() ? RequestBody.create(mediaFile, MediaType.parse(URLConnection.guessContentTypeFromName(mediaFile.getName()))) : RequestBody.create(new byte[]{}, MediaType.parse("application/octet-stream"));
            rsapiClient.photoUpload(bahnhofId, station != null ? station.getCountry() : upload.getCountry(),
                    stationTitle, latitude, longitude, comment, upload.getActive(), file).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<InboxResponse> call, @NonNull Response<InboxResponse> response) {
                    binding.upload.progressBar.setVisibility(View.GONE);
                    InboxResponse inboxResponse;
                    if (response.isSuccessful()) {
                        inboxResponse = response.body();
                    } else if (response.code() == 401) {
                        Toast.makeText(UploadActivity.this, R.string.authorization_failed, Toast.LENGTH_LONG).show();
                        startActivity(new Intent(UploadActivity.this, MyDataActivity.class));
                        return;
                    } else {
                        assert response.errorBody() != null;
                        var gson = new Gson();
                        inboxResponse = gson.fromJson(response.errorBody().charStream(), InboxResponse.class);
                    }

                    assert inboxResponse != null;
                    upload.setRemoteId(inboxResponse.getId());
                    upload.setInboxUrl(inboxResponse.getInboxUrl());
                    upload.setUploadState(inboxResponse.getState().getUploadState());
                    upload.setCrc32(inboxResponse.getCrc32());
                    baseApplication.getDbAdapter().updateUpload(upload);
                    SimpleDialogs.confirm(UploadActivity.this, inboxResponse.getState().getMessageId());
                }

                @Override
                public void onFailure(@NonNull Call<InboxResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error uploading photo", t);
                    binding.upload.progressBar.setVisibility(View.GONE);

                    SimpleDialogs.confirm(UploadActivity.this,
                            String.format(getText(InboxResponse.InboxResponseState.ERROR.getMessageId()).toString(), t.getMessage()));
                    fetchUploadStatus(upload); // try to get the upload state again
                }
            });
        });
    }

    private Intent createPhotoSendIntent() {
        var file = getStoredMediaFile(upload);
        if (file != null && file.canRead()) {
            var sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(UploadActivity.this,
                    BuildConfig.APPLICATION_ID + ".fileprovider", file));
            return sendIntent;
        }
        return null;
    }

    /**
     * Fetch bitmap from device local location, if it exists, and set the photo view.
     */
    private void setLocalBitmap(Upload upload) {
        var localPhoto = checkForLocalPhoto(upload);
        if (localPhoto != null) {
            binding.upload.imageview.setImageBitmap(localPhoto);
            fetchUploadStatus(upload);
        }
    }

    private void fetchUploadStatus(Upload upload) {
        if (upload == null) {
            return;
        }
        var stateQuery = InboxStateQuery.builder()
                .id(upload.getRemoteId())
                .countryCode(upload.getCountry())
                .stationId(upload.getStationId())
                .build();

        rsapiClient.queryUploadState(List.of(stateQuery)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<InboxStateQuery>> call, @NonNull Response<List<InboxStateQuery>> response) {
                var stateQueries = response.body();
                if (stateQueries != null && !stateQueries.isEmpty()) {
                    var stateQuery = stateQueries.get(0);
                    binding.upload.uploadStatus.setText(getString(R.string.upload_state, getString(stateQuery.getState().getTextId())));
                    binding.upload.uploadStatus.setTextColor(getResources().getColor(stateQuery.getState().getColorId(), null));
                    binding.upload.uploadStatus.setVisibility(View.VISIBLE);
                    upload.setUploadState(stateQuery.getState());
                    upload.setRejectReason(stateQuery.getRejectedReason());
                    upload.setCrc32(stateQuery.getCrc32());
                    upload.setRemoteId(stateQuery.getId());
                    baseApplication.getDbAdapter().updateUpload(upload);
                    updateCrc32Checkbox();
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<InboxStateQuery>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
            }
        });

    }

    private void updateCrc32Checkbox() {
        if (crc32 != null && upload != null) {
            var sameChecksum = crc32.equals(upload.getCrc32());
            binding.upload.cbChecksum.setVisibility(sameChecksum ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Check if there's a local photo file for this station.
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
                updateCrc32Checkbox();
                return scaledScreen;
            } catch (Exception e) {
                Log.e(TAG, String.format("Error reading media file for station %s", bahnhofId), e);
            }
        }
        return null;
    }

}
