package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
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
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import static android.content.Intent.createChooser;
import static android.graphics.Color.WHITE;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.MyDataDialogFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.NavItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, BitmapAvailableHandler {

    // Names of Extras that this class reacts to
    public static final String EXTRA_TAKE_FOTO = "DetailsActivityTakeFoto";
    public static final String EXTRA_BAHNHOF = "bahnhof";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final int STORED_FOTO_WIDTH = 1920;
    public static final int STORED_FOTO_QUALITY = 95;

    private static final String TAG = DetailsActivity.class.getSimpleName();
    private static final int REQUEST_TAKE_PICTURE_PERMISSION = 0;
    private static final int REQUEST_SELECT_PICTURE_PERMISSION = 1;
    private static final int REQUEST_TAKE_PICTURE = 2;
    private static final int REQUEST_SELECT_PICTURE = 3;
    private static final int REQUEST_REPORT_GHOST_PERMISSION = 4;
    private static final int ALPHA = 128;

    private static final String LINK_FORMAT = "<b><a href=\"%s\">%s</a></b>";

    private ImageButton takePictureButton;
    private ImageButton selectPictureButton;
    private ImageButton reportGhostStationButton;
    private ImageView imageView;
    private Bahnhof bahnhof;
    private Set<Country> countries;
    private EditText etBahnhofName;
    private EditText etComment;
    private boolean localFotoUsed = false;
    private License license;
    private boolean photoOwner;
    private String nickname;
    private String email;
    private String password;
    private Set<String> countryCodes;
    private TextView licenseTagView;
    private TextView coordinates;
    private ViewGroup detailsLayout;
    private boolean fullscreen;
    private BaseApplication baseApplication;
    private LinearLayout header;
    private Bitmap publicBitmap;
    private RSAPI rsapi;
    private Double latitude;
    private Double longitude;
    private String bahnhofId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();
        BahnhofsDbAdapter dbAdapter = baseApplication.getDbAdapter();
        countryCodes = baseApplication.getCountryCodes();
        countries = dbAdapter.fetchCountriesWithProviderApps(countryCodes);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        detailsLayout = findViewById(R.id.content_details);
        header = findViewById(R.id.header);
        etBahnhofName = findViewById(R.id.etbahnhofname);
        etComment = findViewById(R.id.etComment);
        coordinates = findViewById(R.id.coordinates);
        TextView isInactive = findViewById(R.id.isinactive);
        imageView = findViewById(R.id.imageview);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPictureClicked();
            }
        });
        takePictureButton = findViewById(R.id.button_take_picture);
        takePictureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePictureWithPermissionCheck();
                    }
                }
        );
        selectPictureButton = findViewById(R.id.button_select_picture);
        selectPictureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectPictureWithPermissionCheck();
                    }
                }
        );
        reportGhostStationButton = findViewById(R.id.button_remove_station);
        reportGhostStationButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reportGhostStationWithPermissionCheck();
                    }
                }
        );

        licenseTagView = findViewById(R.id.license_tag);
        licenseTagView.setMovementMethod(LinkMovementMethod.getInstance());

        // switch off image and license view until we actually have a foto
        imageView.setVisibility(View.INVISIBLE);
        licenseTagView.setVisibility(View.INVISIBLE);
        setPictureButtonsVisibility(View.INVISIBLE);

        fullscreen = false;

        readPreferences();
        Intent intent = getIntent();
        boolean directPicture = false;
        if (intent != null) {
            bahnhof = (Bahnhof) intent.getSerializableExtra(EXTRA_BAHNHOF);
            latitude = (Double) intent.getSerializableExtra(EXTRA_LATITUDE);
            longitude = (Double) intent.getSerializableExtra(EXTRA_LONGITUDE);
            if (bahnhof == null && (latitude == null || longitude == null)) {
                Log.w(TAG, "EXTRA_BAHNHOF and EXTRA_LATITUDE or EXTRA_LONGITUDE in intent data missing");
                Toast.makeText(this, R.string.station_or_coords_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            directPicture = intent.getBooleanExtra(EXTRA_TAKE_FOTO, false);
            if (bahnhof != null) {
                bahnhofId = bahnhof.getId();
                etBahnhofName.setText(bahnhof.getTitle() + " (" + bahnhofId + ")");
                etBahnhofName.setEnabled(false);
                coordinates.setText(bahnhof.getLat() + ", " + bahnhof.getLon());
            if (!bahnhof.isActive()) {
                isInactive.setVisibility(View.VISIBLE);
            }

                if (bahnhof.hasPhoto()) {
                    if (ConnectionUtil.checkInternetConnection(this)) {
                        BitmapCache.getInstance().getFoto(this, bahnhof.getPhotoUrl());
                    }
                    if (canSetPhoto()) {
                        setPictureButtonsVisibility(View.VISIBLE);
                    } else {
                        setPictureButtonsVisibility(View.INVISIBLE);
                        etComment.setVisibility(View.INVISIBLE);
                    }
                } else {
                    setLocalBitmap();
                }
            } else {
                etBahnhofName.setEnabled(true);
                coordinates.setText(latitude + ", " + longitude);
                bahnhofId = latitude + "_" + longitude;
                setPictureButtonsVisibility(View.VISIBLE);
                setLocalBitmap();
            }

        }

        if (directPicture) {
            takePictureWithPermissionCheck();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readPreferences();
    }

    private void readPreferences() {
        license = baseApplication.getLicense();
        photoOwner = baseApplication.getPhotoOwner();
        nickname = baseApplication.getNickname();
        email = baseApplication.getEmail();
        password = baseApplication.getPassword();
    }

    private void checkMyData() {
        MyDataDialogFragment myDataDialog = new MyDataDialogFragment();
        myDataDialog.show(getFragmentManager(), "mydata_dialog");
    }

    /**
     * Method to request permission for taking picture
     */
    private void requestPermissionAndTakePicture() {
        // Camera and Write permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_TAKE_PICTURE_PERMISSION);
    }

    /**
     * Method to request permission for specific action
     */
    private void requestStoragePermission(int requestCode) {
        // Write permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
    }

    public void selectPicture() {
        if (!canSetPhoto()) {
            return;
        }

        if (isMyDataIncomplete()) {
            checkMyData();
        } else {
            File file = getCameraMediaFile();
            if (file != null) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), REQUEST_SELECT_PICTURE);
            } else {
                Toast.makeText(this, R.string.unable_to_create_folder_structure, Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean canSetPhoto() {
        return bahnhof == null || !bahnhof.hasPhoto() || TextUtils.equals(nickname, bahnhof.getPhotographer());
    }

    public void takePicture() {
        if (!canSetPhoto()) {
            return;
        }

        if (isMyDataIncomplete()) {
            checkMyData();
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = getCameraMediaFile();
            if (file != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "de.bahnhoefe.deutschlands.bahnhofsfotos.fileprovider", file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, getResources().getString(R.string.app_name));
                intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, etBahnhofName.getText());
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_TAKE_PICTURE);
            } else {
                Toast.makeText(this, R.string.unable_to_create_folder_structure, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void reportGhostStationWithConfirmation() {
        if (!canSetPhoto()) {
            return;
        }

        if (isMyDataIncomplete()) {
            checkMyData();
        } else {
            if (localFotoUsed || (bahnhof != null && bahnhof.hasPhoto())) {
                new SimpleDialogs().confirm(this, R.string.confirm_replace_with_ghost, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reportGhostStation();
                    }
                });
            } else {
                reportGhostStation();
            }
        }
    }

    public void reportGhostStation() {
        File file = getStoredMediaFile();
        if (file != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ghost_station);
                int sampling = bitmap.getWidth() / STORED_FOTO_WIDTH;
                Bitmap scaledScreen = bitmap;
                if (sampling > 1) {
                    scaledScreen = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / sampling, bitmap.getHeight() / sampling, false);
                }

                saveScaledBitmap(file, scaledScreen);
                Toast.makeText(getApplicationContext(), getString(R.string.report_ghost_station), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Error processing photo", e);
                Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.unable_to_create_folder_structure, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isMyDataIncomplete() {
        return TextUtils.isEmpty(nickname) || license == License.UNKNOWN || !photoOwner;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_TAKE_PICTURE_PERMISSION) {
            Log.i(TAG, "Received response for Camera permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Camera and Write permission has been granted, preview can be displayed
                takePicture();
            } else {
                //Permission not granted
                Toast.makeText(DetailsActivity.this, R.string.grant_camera_permission, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_SELECT_PICTURE_PERMISSION) {
            Log.i(TAG, "Received response for select image permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Write permission has been granted, preview can be displayed
                selectPicture();
            } else {
                //Permission not granted
                Toast.makeText(DetailsActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_REPORT_GHOST_PERMISSION) {
            Log.i(TAG, "Received response for report ghost permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Write permission has been granted, preview can be displayed
                reportGhostStationWithConfirmation();
            } else {
                //Permission not granted
                Toast.makeText(DetailsActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Method to check permission before taking a picture
     */
    void takePictureWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Camera permission has not been granted.
                requestPermissionAndTakePicture();
            } else {
                takePicture();
            }
        } else {
            takePicture();
        }
    }

    /**
     * Method to check permission before selecting a picture
     */
    void selectPictureWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Write permission has not been granted.
                requestStoragePermission(REQUEST_SELECT_PICTURE_PERMISSION);
            } else {
                selectPicture();
            }
        } else {
            selectPicture();
        }
    }

    /**
     * Method to check permission before selecting a picture
     */
    void reportGhostStationWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Write permission has not been granted.
                requestStoragePermission(REQUEST_REPORT_GHOST_PERMISSION);
            } else {
                reportGhostStationWithConfirmation();
            }
        } else {
            reportGhostStationWithConfirmation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        try {
            File storagePictureFile = getStoredMediaFile();
            if (requestCode == REQUEST_TAKE_PICTURE) {
                // die Kamera-App sollte auf temporären Cache-Speicher schreiben, wir laden das Bild von
                // dort und schreiben es in Standard-Größe in den permanenten Speicher
                File cameraRawPictureFile = getCameraMediaFile();
                if (cameraRawPictureFile == null || storagePictureFile == null) {
                    throw new RuntimeException(getString(R.string.photofile_not_existing));
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true; // just query the image size in the first step
                BitmapFactory.decodeFile(
                        cameraRawPictureFile.getPath(),
                        options);

                int sampling = options.outWidth / STORED_FOTO_WIDTH;
                if (sampling > 1) {
                    options.inSampleSize = sampling;
                }
                options.inJustDecodeBounds = false;

                Bitmap scaledScreen = BitmapFactory.decodeFile(
                        cameraRawPictureFile.getPath(),
                        options);

                saveScaledBitmap(storagePictureFile, scaledScreen);
                // temp file begone!
                cameraRawPictureFile.delete();
            } else if (requestCode == REQUEST_SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                Bitmap bitmap = getBitmapFromUri(selectedImageUri);

                int sampling = bitmap.getWidth() / STORED_FOTO_WIDTH;
                Bitmap scaledScreen = bitmap;
                if (sampling > 1) {
                    scaledScreen = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / sampling, bitmap.getHeight() / sampling, false);
                }

                saveScaledBitmap(storagePictureFile, scaledScreen);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing photo", e);
            Toast.makeText(getApplicationContext(), getString(R.string.error_processing_photo) + e.getMessage(), Toast.LENGTH_LONG).show();
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

        scaledScreen.compress(Bitmap.CompressFormat.JPEG, STORED_FOTO_QUALITY, new FileOutputStream(storagePictureFile));
        setLocalBitmap();
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    /**
     * Get the base directory for storing fotos
     *
     * @return the File denoting the base directory.
     */
    @Nullable
    private File getMediaStorageDir() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), Constants.PHOTO_DIRECTORY);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Cannot create directory structure " + mediaStorageDir.getAbsolutePath());
                return null;
            }
        }
        return mediaStorageDir;
    }

    /**
     * Get the file path for storing this station's foto
     *
     * @return the File
     */
    @Nullable
    public File getStoredMediaFile() {
        File mediaStorageDir = getMediaStorageDir();
        if (mediaStorageDir == null) {
            return null;
        }

        File storeMediaFile = new File(mediaStorageDir, String.format("%s_%s.jpg", nickname, bahnhofId));
        Log.d(TAG, storeMediaFile.toString());

        return storeMediaFile;

    }

    /**
     * Get the file path for the Camera app to store the unprocessed foto to.
     *
     * @return the File
     */
    private File getCameraMediaFile() {
        File temporaryStorageDir = new File(getMediaStorageDir(), ".temp");
        if (!temporaryStorageDir.exists()) {
            if (!temporaryStorageDir.mkdirs()) {
                Log.e(TAG, "Cannot create directory structure " + temporaryStorageDir.getAbsolutePath());
                return null;
            }
        }

        Log.d(TAG, "Temporary BahnhofNrAbfrage: " + bahnhofId);
        File file = new File(temporaryStorageDir, String.format("%s_%s.jpg", nickname, bahnhofId));
        Log.d("FilePfad", file.toString());

        return file;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.details, menu);
        MenuItem navToStation = menu.findItem(R.id.nav_to_station);
        navToStation.getIcon().mutate();
        navToStation.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);

        if (localFotoUsed) {
            enableMenuItem(menu, R.id.photo_upload);
        } else {
            disableMenuItem(menu, R.id.photo_upload);
        }

        if (localFotoUsed || (bahnhof != null && bahnhof.hasPhoto())) {
            enableMenuItem(menu, R.id.share_photo);
        } else {
            disableMenuItem(menu, R.id.share_photo);
        }

        if (bahnhof != null && Country.getCountryByCode(countries, bahnhof.getCountry()).hasTimetableUrlTemplate()) {
            enableMenuItem(menu, R.id.timetable);
        } else {
            disableMenuItem(menu, R.id.timetable);
        }

        boolean hasProviderApps = false;
        if (bahnhof != null) {
            Country countryByCode = Country.getCountryByCode(countries, bahnhof.getCountry());
            hasProviderApps = countryByCode.hasCompatibleProviderApps();
        }
        if (hasProviderApps) {
            enableMenuItem(menu, R.id.provider_android_app);
        } else {
            disableMenuItem(menu, R.id.provider_android_app);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void enableMenuItem(Menu menu, int id) {
        MenuItem menuItem = menu.findItem(id).setEnabled(true);
        menuItem.getIcon().mutate();
        menuItem.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);
    }

    private void disableMenuItem(Menu menu, int id) {
        MenuItem menuItem = menu.findItem(id).setEnabled(false);
        menuItem.getIcon().mutate();
        menuItem.getIcon().setColorFilter(WHITE, PorterDuff.Mode.SRC_ATOP);
        menuItem.getIcon().setAlpha(ALPHA);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_to_station:
                startNavigation(DetailsActivity.this);
                break;
            case R.id.timetable:
                final Intent timetableIntent = new Timetable().createTimetableIntent(Country.getCountryByCode(countries, bahnhof.getCountry()), bahnhof);
                if (timetableIntent != null) {
                    startActivity(timetableIntent);
                } else {
                    Toast.makeText(this, R.string.timetable_missing, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.photo_upload:
                if (isMyDataIncomplete()) {
                    checkMyData();
                } else {
                    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                        Toast.makeText(this, R.string.registration_needed, Toast.LENGTH_LONG).show();
                    } else if (TextUtils.isEmpty(etBahnhofName.getText())) {
                        Toast.makeText(this, R.string.station_title_needed, Toast.LENGTH_LONG).show();
                    } else {
                        uploadPhoto();
                    }
                }
                break;
            case R.id.share_photo:
                Intent shareIntent = createFotoSendIntent();
                shareIntent.putExtra(Intent.EXTRA_TEXT, Country.getCountryByCode(countries, bahnhof != null ? bahnhof.getCountry() : null).getTwitterTags() + " " + etBahnhofName.getText());
                shareIntent.setType("image/jpeg");
                startActivity(createChooser(shareIntent, "send"));
                break;
            case R.id.provider_android_app:
                Country country = Country.getCountryByCode(countries, bahnhof.getCountry());
                final List<ProviderApp> providerApps = country.getCompatibleProviderApps();
                if (providerApps.size() == 1) {
                    providerApps.get(0).openAppOrPlayStore(this);
                } else if (providerApps.size() > 1) {
                    CharSequence[] appNames = new CharSequence[providerApps.size()];
                    for (int i = 0; i < providerApps.size(); i++) {
                        appNames[i] = providerApps.get(i).getName();
                    }
                    new SimpleDialogs().simpleSelect(this, getResources().getString(R.string.choose_provider_app), appNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
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
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                } else {
                    onBackPressed();
                }

                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void uploadPhoto() {
        final ProgressDialog progress = new ProgressDialog(DetailsActivity.this);
        progress.setMessage(getResources().getString(R.string.send));
        progress.setTitle(getResources().getString(R.string.app_name));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();

        String stationTitle = etBahnhofName.getText().toString();

        try {
            stationTitle = URLEncoder.encode(etBahnhofName.getText().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding station title", e);
        }

        final File mediaFile = getStoredMediaFile();
        RequestBody file = RequestBody.create(MediaType.parse(URLConnection.guessContentTypeFromName(mediaFile.getName())), mediaFile);
        rsapi.photoUpload(RSAPI.Helper.getAuthorizationHeader(email, password), bahnhofId, bahnhof != null ? bahnhof.getCountry() : null,
                stationTitle, latitude, longitude, etComment.getText().toString(), file).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progress.dismiss();
                switch (response.code()) {
                    case 202 :
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_completed);
                        break;
                    case 400 :
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_bad_request);
                        break;
                    case 401 :
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.authorization_failed);
                        break;
                    case 409 :
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_conflict);
                        break;
                    case 413 :
                        new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_too_big);
                        break;
                    default :
                        new SimpleDialogs().confirm(DetailsActivity.this,
                            String.format(getText(R.string.upload_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error uploading photo", t);
                progress.dismiss();
                new SimpleDialogs().confirm(DetailsActivity.this,
                        String.format(getText(R.string.upload_failed).toString(), t.getMessage()));
            }
        });
    }


    private Intent createFotoSendIntent() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        File file = getStoredMediaFile();
        if (file != null && file.canRead()) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                    "de.bahnhoefe.deutschlands.bahnhofsfotos.fileprovider", file));
        } else if (publicBitmap != null) {
            File imagePath = new File(getApplicationContext().getCacheDir(), "images");
            imagePath.mkdirs();
            File newFile = new File(imagePath, bahnhofId + ".jpg");
            try {
                saveScaledBitmap(newFile, publicBitmap);
                sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                        "de.bahnhoefe.deutschlands.bahnhofsfotos.fileprovider", newFile));
            } catch (FileNotFoundException e) {
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


        ListAdapter adapter = new ArrayAdapter<NavItem>(
                this,
                android.R.layout.select_dialog_item,
                android.R.id.text1,
                items) {
            public View getView(int position, View convertView, ViewGroup parent) {
                //Use super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

                //Add margin between image and text (support various screen densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                int dp7 = (int) (20 * getResources().getDisplayMetrics().density);
                tv.setCompoundDrawablePadding(dp5);
                tv.setPadding(dp7, 0, 0, 0);

                return v;
            }
        };

        final double lat = bahnhof != null ? bahnhof.getLat() : latitude;
        final double lon = bahnhof != null ? bahnhof.getLon() : longitude;
        AlertDialog.Builder navBuilder = new AlertDialog.Builder(this);
        navBuilder.setIcon(R.mipmap.ic_launcher);
        navBuilder.setTitle(R.string.navMethod);
        navBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int navItem) {
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
                                bahnhof.getLat(), bahnhof.getLon());
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
                        intent.putExtra(MapsActivity.EXTRAS_LATITUDE, bahnhof.getLat());
                        intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, bahnhof.getLon());
                        Log.d(TAG,"findnavigation case 5: " + bahnhof.getLat() + "," + bahnhof.getLon());
                        break;

                }
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast toast = Toast.makeText(context, R.string.activitynotfound, Toast.LENGTH_LONG);
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
        if (bahnhof != null && bahnhof.getLicense() != null) {
            final boolean photographerUrlAvailable = bahnhof.getPhotographerUrl() != null && !bahnhof.getPhotographerUrl().isEmpty();
            final boolean licenseUrlAvailable = bahnhof.getLicenseUrl() != null && !bahnhof.getLicenseUrl().isEmpty();

            final String photographerText;
            if (photographerUrlAvailable) {
                photographerText = String.format(
                        LINK_FORMAT,
                        bahnhof.getPhotographerUrl(),
                        bahnhof.getPhotographer());
            } else {
                photographerText = bahnhof.getPhotographer();
            }

            final String licenseText;
            if (licenseUrlAvailable) {
                licenseText = String.format(
                        LINK_FORMAT,
                        bahnhof.getLicenseUrl(),
                        bahnhof.getLicense());
            } else {
                licenseText = bahnhof.getLicense();
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
     * Fetch bitmap from device local location, if  it exists, and set the foto view.
     */
    private void setLocalBitmap() {
        setPictureButtonsVisibility(View.VISIBLE);

        Bitmap showBitmap = checkForLocalPhoto();
        if (showBitmap == null) {
            // lokal keine Bitmap
            localFotoUsed = false;
            // switch off image and license view until we actually have a foto
            imageView.setVisibility(View.INVISIBLE);
            licenseTagView.setVisibility(View.INVISIBLE);
        } else {
            setBitmap(showBitmap);
        }
    }

    private void setPictureButtonsVisibility(int visible) {
        takePictureButton.setVisibility(visible);
        selectPictureButton.setVisibility(visible);
        reportGhostStationButton.setVisibility(visible);
    }

    private void setBitmap(final Bitmap showBitmap) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int targetWidth = size.x;

        if (showBitmap.getWidth() != targetWidth) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(showBitmap,
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
        public void onAnimationUpdate(ValueAnimator animation) {
            float alpha = (float) animation.getAnimatedValue();
            if (header == null) {
                etBahnhofName.setAlpha(alpha);
            } else {
                header.setAlpha(alpha);
            }
            licenseTagView.setAlpha(alpha);
        }
    }

    /**
     * Check if there's a local photo file for this station.
     *
     * @return the Bitmap of the photo, or null if none exists.
     */
    @Nullable
    private Bitmap checkForLocalPhoto() {
        // show the image
        Bitmap scaledScreen = null;
        File localFile = getStoredMediaFile();

        if (localFile != null && localFile.canRead()) {
            Log.d(TAG, "File: " + localFile);
            Log.d(TAG, "FileGetPath: " + localFile.getPath().toString());

            scaledScreen = BitmapFactory.decodeFile(
                    localFile.getPath());
            Log.d(TAG, "img width " + scaledScreen.getWidth());
            Log.d(TAG, "img height " + scaledScreen.getHeight());
            localFotoUsed = true;
            return scaledScreen;
        } else {
            localFotoUsed = false;
            Log.e(TAG,
                    String.format("Media file not available for station %s and nickname %s ",
                            bahnhofId,
                            nickname
                    )
            );
            return null;
        }

    }

    public void onPictureClicked() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !fullscreen) {
            ValueAnimator animation = ValueAnimator.ofFloat(header.getAlpha(), 0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            detailsLayout.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
            ActionBar bar = getActionBar();
            if (bar != null)
                bar.hide();
            android.support.v7.app.ActionBar sbar = getSupportActionBar();
            if (sbar != null)
                sbar.hide();
            fullscreen = true;
        } else {
            ValueAnimator animation = ValueAnimator.ofFloat(header == null? etBahnhofName.getAlpha() : header.getAlpha(), 1.0f);
            animation.setDuration(500);
            animation.addUpdateListener(new AnimationUpdateListener());
            animation.start();
            detailsLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            ActionBar bar = getActionBar();
            if (bar != null)
                bar.show();
            android.support.v7.app.ActionBar sbar = getSupportActionBar();
            if (sbar != null)
                sbar.show();
            fullscreen = false;
        }

    }

}
