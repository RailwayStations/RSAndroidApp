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
import android.os.AsyncTask;
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
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static android.content.Intent.createChooser;
import static android.graphics.Color.WHITE;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.MyDataDialogFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Linking;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoOwner;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.NavItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable;

public class DetailsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, BitmapAvailableHandler {

    // Names of Extras that this class reacts to
    public static final String EXTRA_TAKE_FOTO = "DetailsActivityTakeFoto";
    public static final String EXTRA_BAHNHOF = "bahnhof";
    public static final int STORED_FOTO_WIDTH = 1920;
    public static final int STORED_FOTO_QUALITY = 95;

    private static final String TAG = DetailsActivity.class.getSimpleName();
    private static final int REQUEST_TAKE_PICTURE_PERMISSION = 0;
    private static final int REQUEST_SELECT_PICTURE_PERMISSION = 1;
    private static final int REQUEST_TAKE_PICTURE = 2;
    private static final int REQUEST_SELECT_PICTURE = 3;
    private static final int alpha = 128;

    private ImageButton takePictureButton;
    private ImageButton selectPictureButton;
    private ImageView imageView;
    private Bahnhof bahnhof;
    private Country country;
    private TextView tvBahnhofName;
    private boolean localFotoUsed = false;
    private License license;
    private PhotoOwner photoOwner;
    private Linking linking;
    private String link;
    private String nickname;
    private String email;
    private String token;
    private String countryShortCode;
    private TextView licenseTagView;
    private TextView coordinates;
    private ViewGroup detailsLayout;
    private boolean fullscreen;
    private BaseApplication baseApplication;
    private LinearLayout header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        baseApplication = (BaseApplication) getApplication();
        BahnhofsDbAdapter dbAdapter = baseApplication.getDbAdapter();
        countryShortCode = baseApplication.getCountryShortCode();
        country = dbAdapter.fetchCountryByCountryShortCode(countryShortCode);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        detailsLayout = findViewById(R.id.content_details);
        header = findViewById(R.id.header);
        tvBahnhofName = findViewById(R.id.tvbahnhofname);
        coordinates = findViewById(R.id.coordinates);
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

        licenseTagView = findViewById(R.id.license_tag);
        licenseTagView.setMovementMethod(LinkMovementMethod.getInstance());

        // switch off image and license view until we actually have a foto
        imageView.setVisibility(View.INVISIBLE);
        licenseTagView.setVisibility(View.INVISIBLE);
        takePictureButton.setVisibility(View.INVISIBLE);
        selectPictureButton.setVisibility(View.INVISIBLE);

        fullscreen = false;

        readPreferences();
        Intent intent = getIntent();
        boolean directPicture = false;
        if (intent != null) {
            bahnhof = (Bahnhof) intent.getSerializableExtra(EXTRA_BAHNHOF);
            if (bahnhof == null) {
                Log.w(TAG, "EXTRA_BAHNHOF in intent data missing");
                Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            directPicture = intent.getBooleanExtra(EXTRA_TAKE_FOTO, false);
            tvBahnhofName.setText(bahnhof.getTitle() + " (" + bahnhof.getId() + ")");
            coordinates.setText(bahnhof.getLat() + ", " + bahnhof.getLon());

            if (bahnhof.hasPhoto()) {
                if (ConnectionUtil.checkInternetConnection(this)) {
                    BitmapCache.getInstance().getFoto(this, bahnhof.getPhotoUrl());
                }
            } else {
                takePictureButton.setVisibility(View.VISIBLE);
                selectPictureButton.setVisibility(View.VISIBLE);
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
        linking = baseApplication.getLinking();
        link = baseApplication.getPhotographerLink();
        nickname = baseApplication.getNickname();
        email = baseApplication.getEmail();
        token = baseApplication.getUploadToken();
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
     * Method to request permission for selecting picture
     */
    private void requestPermissionAndSelectPicture() {
        // Write permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SELECT_PICTURE_PERMISSION);
    }

    public void selectPicture() {
        if (bahnhof.hasPhoto()) {
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

    public void takePicture() {
        if (bahnhof.hasPhoto()) {
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
                intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, bahnhof.getTitle());
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_TAKE_PICTURE);
            } else {
                Toast.makeText(this, R.string.unable_to_create_folder_structure, Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isMyDataIncomplete() {
        return TextUtils.isEmpty(nickname) || license == License.UNKNOWN || photoOwner == PhotoOwner.UNKNOWN;
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
                requestPermissionAndSelectPicture();
            } else {
                selectPicture();
            }
        } else {
            selectPicture();
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

        Log.d(TAG, "BahnhofNrAbfrage: " + bahnhof.getId());
        File file = new File(mediaStorageDir, String.format("%s-%s.jpg", nickname, bahnhof.getId()));
        Log.d("FilePfad", file.toString());

        return file;

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

        Log.d(TAG, "Temporary BahnhofNrAbfrage: " + bahnhof.getId());
        File file = new File(temporaryStorageDir, String.format("%s-%s.jpg", nickname, bahnhof.getId()));
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
            enableMenuItem(menu, R.id.send_email);
            enableMenuItem(menu, R.id.share_photo);
            enableMenuItem(menu, R.id.photo_upload);
        } else {
            disableMenuItem(menu, R.id.send_email);
            disableMenuItem(menu, R.id.share_photo);
            disableMenuItem(menu, R.id.photo_upload);
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
        menuItem.getIcon().setAlpha(alpha);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_to_station:
                startNavigation(DetailsActivity.this);
                break;
            case R.id.timetable:
                final Intent timetableIntent = new Timetable().createTimetableIntent(country, bahnhof);
                if (timetableIntent != null) {
                    startActivity(timetableIntent);
                } else {
                    Toast.makeText(this, R.string.timetable_missing, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.send_email:
                if (isMyDataIncomplete()) {
                    checkMyData();
                } else {
                    Intent emailIntent = createFotoSendIntent();
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{country.getEmail()});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bahnhofsfoto: " + bahnhof.getTitle());
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Lizenz: " + license
                            + "\n selbst fotografiert: " + photoOwner
                            + "\n Nickname: " + nickname
                            + "\n Verlinken bitte mit: " + linking
                            + "\n Link zum Account: " + link);
                    emailIntent.setType("multipart/byteranges");
                    startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.send_email)));
                }
                break;
            case R.id.photo_upload:
                if (isMyDataIncomplete()) {
                    checkMyData();
                } else {
                    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(token)) {
                        Toast.makeText(this, R.string.registration_needed, Toast.LENGTH_LONG).show();
                    } else {
                        if (ConnectionUtil.checkInternetConnection(this)) {
                            new PhotoUploadTask(countryShortCode.toLowerCase(), nickname, email, bahnhof.getId(), getStoredMediaFile(), getString(R.string.rs_api_key), token).execute();
                        }
                    }
                }
                break;
            case R.id.share_photo:
                Intent shareIntent = createFotoSendIntent();
                shareIntent.putExtra(Intent.EXTRA_TEXT, country.getTwitterTags() + " " + bahnhof.getTitle());
                shareIntent.setType("image/jpeg");
                startActivity(createChooser(shareIntent, "send"));
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


    private Intent createFotoSendIntent() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        File file = getStoredMediaFile();
        if (file != null) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                    "de.bahnhoefe.deutschlands.bahnhofsfotos.fileprovider", file));
        }
        return sendIntent;
    }

    private void startNavigation(final Context context) {
        final NavItem[] items = {
                new NavItem("   " + getString(R.string.nav_oepnv), R.drawable.ic_directions_bus_gray_24px),
                new NavItem("   " + getString(R.string.nav_car), R.drawable.ic_directions_car_gray_24px),
                new NavItem("   " + getString(R.string.nav_bike), R.drawable.ic_directions_bike_gray_24px),
                new NavItem("   " + getString(R.string.nav_walk), R.drawable.ic_directions_walk_gray_24px),
                new NavItem("   " + getString(R.string.nav_show), R.drawable.ic_info_gray_24px)
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

        AlertDialog.Builder navBuilder = new AlertDialog.Builder(this);
        navBuilder.setIcon(R.mipmap.ic_launcher);
        navBuilder.setTitle(R.string.navMethod);
        navBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int navItem) {
                String dlocation = "";
                Intent intent = null;
                switch (navItem) {
                    case 0:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=Transit", bahnhof.getLat(), bahnhof.getLon());
                        Log.d("findnavigation case 0", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;
                    case 1:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=d", bahnhof.getLat(), bahnhof.getLon());
                        Log.d("findnavigation case 1", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;

                    case 2:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=b",
                                bahnhof.getLat(), bahnhof.getLon());
                        Log.d("findnavigation case 2", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;
                    case 3:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=w", bahnhof.getLat(), bahnhof.getLon());
                        Log.d("findnavigation case 3", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;
                    case 4:
                        dlocation = String.format("geo:0,0?q=%s,%s(%s)", bahnhof.getLat(), bahnhof.getLon(), bahnhof.getTitle());
                        Log.d("findnavigation case 4", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
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
     * @param publicBitmap the fetched Bitmap for the notification. May be null
     */
    @Override
    public void onBitmapAvailable(final @Nullable Bitmap publicBitmap) {
        localFotoUsed = false;
        takePictureButton.setVisibility(View.INVISIBLE);
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
        if (bahnhof.getLicense() != null) {
            licenseTagView.setText(
                    String.format(
                            getText(R.string.license_tag).toString(),
                            bahnhof.getPhotographer(),
                            bahnhof.getLicense())
            );
            licenseTagView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Build an intent for an action to view the author if URL is provided
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                            String photographerUrl = bahnhof.getPhotographerUrl();
                            if (photographerUrl != null && !photographerUrl.isEmpty()) {
                                mapIntent.setData(Uri.parse(photographerUrl));
                                startActivity(mapIntent);
                            }
                        }
                    }
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
        takePictureButton.setVisibility(View.VISIBLE);
        selectPictureButton.setVisibility(View.VISIBLE);

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
                tvBahnhofName.setAlpha(alpha);
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
                            bahnhof.getId(),
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
            ValueAnimator animation = ValueAnimator.ofFloat(header == null? tvBahnhofName.getAlpha() : header.getAlpha(), 1.0f);
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

    public class PhotoUploadTask extends AsyncTask<Void, String, Integer> {

        private final String countryCode;
        private final String nickname;
        private final String stationId;
        private final File file;
        private final String apiKey;
        private final String token;
        private final String email;
        private ProgressDialog progressDialog;

        public PhotoUploadTask(String countryCode, String nickname, String email, String stationId, File file, String apiKey, String token) {
            this.countryCode = countryCode;
            this.nickname = nickname;
            this.email = email;
            this.stationId = stationId;
            this.file = file;
            this.apiKey = apiKey;
            this.token = token;
        }


        @Override
        protected Integer doInBackground(Void... params) {
            HttpURLConnection conn = null;
            DataOutputStream wr = null;
            FileInputStream is = null;
            int status = -1;

            publishProgress(getString(R.string.connecting));
            try {
                URL url = new URL(String.format("%s/photoUpload", Constants.API_START_URL));
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", URLConnection.guessContentTypeFromName(file.getName()));
                conn.setRequestProperty( "Content-Length", String.valueOf(file.length()));
                conn.setRequestProperty( "API-Key", apiKey);
                conn.setRequestProperty( "Nickname", nickname);
                conn.setRequestProperty( "Email", email);
                conn.setRequestProperty( "Upload-Token", token);
                conn.setRequestProperty( "Station-Id", stationId);
                conn.setRequestProperty( "Country", countryCode);
                conn.setUseCaches( false );

                wr = new DataOutputStream( conn.getOutputStream());
                is = new FileInputStream(file);
                byte[] buffer = new byte[8196];
                int bytesRead = 0;
                while ((bytesRead = is.read(buffer)) > 0) {
                    publishProgress(getString(R.string.sending));
                    wr.write( buffer, 0, bytesRead );
                }
                wr.flush();

                status = conn.getResponseCode();
                Log.i(TAG, "Upload photo response: " + status);
            } catch ( Exception e) {
                status = -2;
                Log.e(TAG, "Could upload photo", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    if (wr != null) {
                        wr.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close stream", e);
                }
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (DetailsActivity.this.isDestroyed()) {
                return;
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (result == 202) {
                new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_completed);
            } else if (result == 401) {
                new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_token_invalid);
            } else if (result == 409) {
                new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_conflict);
            } else {
                new SimpleDialogs().confirm(DetailsActivity.this,
                        String.format(getText(R.string.upload_failed).toString(), result));
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(DetailsActivity.this);
            progressDialog.setIndeterminate(false);

            // show it
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(getString(R.string.send_data) + values[0]);

        }

    }

}
