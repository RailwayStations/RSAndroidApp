package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
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
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
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
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BahnhofsFotoFetchTask;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.NavItem;

public class DetailsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, BitmapAvailableHandler {
    // Names of Extras that this class reacts to
    public static final String EXTRA_TAKE_FOTO = "DetailsActivityTakeFoto";
    public static final String EXTRA_BAHNHOF = "bahnhof";
    private static final String TAG = DetailsActivity.class.getSimpleName();
    public static final int STORED_FOTO_WIDTH = 1920;
    public static final int STORED_FOTO_QUALITY = 95;

    private ImageButton takePictureButton;
    private ImageView imageView;
    private Bahnhof bahnhof;
    private Country country;
    private TextView tvBahnhofName;
    private boolean localFotoUsed = false;
    private static final String DEFAULT = "default";
    private String licence, photoOwner, linking, link, nickname, email, token, countryShortCode;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int alpha = 128;

    /**
     * Id to identify a camera permission request.
     */
    private static final int REQUEST_CAMERA = 0;
    private TextView licenseTagView;

    private BahnhofsFotoFetchTask fetchTask;
    private ViewGroup detailsLayout;
    private boolean fullscreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        BaseApplication baseApplication = (BaseApplication) getApplication();
        BahnhofsDbAdapter dbAdapter = baseApplication.getDbAdapter();
        countryShortCode = baseApplication.getCountryShortCode();
        country = dbAdapter.fetchCountryByCountryShortCode(countryShortCode);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        detailsLayout = (ViewGroup) findViewById(R.id.content_details);
        tvBahnhofName = (TextView) findViewById(R.id.tvbahnhofname);
        imageView = (ImageView) findViewById(R.id.imageview);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPictureClicked();
            }
        });
        takePictureButton = (ImageButton) findViewById(R.id.button_image);
        enablePictureButton(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        licenseTagView = (TextView) findViewById(R.id.license_tag);
        licenseTagView.setMovementMethod(LinkMovementMethod.getInstance());

        // switch off image and license view until we actually have a foto
        imageView.setVisibility(View.INVISIBLE);
        licenseTagView.setVisibility(View.INVISIBLE);
        takePictureButton.setVisibility(View.INVISIBLE);

        fullscreen = false;

        readPreferences();
        Intent intent = getIntent();
        boolean directPicture = false;
        if (intent != null) {
            bahnhof = (Bahnhof) intent.getSerializableExtra(EXTRA_BAHNHOF);
            if (bahnhof == null) {
                Log.w(TAG, "EXTRA_BAHNHOF in intent data missing");
                Toast.makeText(this, "Bahnhof nicht gefunden", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            directPicture = intent.getBooleanExtra(EXTRA_TAKE_FOTO, false);
            tvBahnhofName.setText(bahnhof.getTitle() + " (" + bahnhof.getId() + ")");

            if (bahnhof.getPhotoflag() != null) {
                if (ConnectionUtil.checkInternetConnection(this)) {
                    fetchTask = new BahnhofsFotoFetchTask(this, getApplicationContext());
                    fetchTask.execute(bahnhof.getId());
                }
            } else {
                takePictureButton.setVisibility(View.VISIBLE);
                setLocalBitmap();
            }
        }

        if (directPicture) {
            checkCameraPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readPreferences();
    }

    private void readPreferences() {
        // Load sharedPreferences for filling the E-Mail and variables for Filename to send
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);

        licence = sharedPreferences.getString(getString(R.string.LICENCE), DEFAULT);
        photoOwner = sharedPreferences.getString(getString(R.string.PHOTO_OWNER), DEFAULT);
        linking = sharedPreferences.getString(getString(R.string.LINKING), DEFAULT);
        link = sharedPreferences.getString(getString(R.string.LINK_TO_PHOTOGRAPHER), DEFAULT);
        nickname = sharedPreferences.getString(getString(R.string.NICKNAME), DEFAULT);
        email = sharedPreferences.getString(getString(R.string.EMAIL), DEFAULT);
        token = sharedPreferences.getString(getString(R.string.UPLOAD_TOKEN), DEFAULT);
    }

    private void enablePictureButton(boolean enabled) {
        // first, the button should look enabled or disabled
        takePictureButton.setImageAlpha(enabled ? 255 : 100);
        // then we associate clickListener which either does work or displays a helpful message
        if (enabled) {
            takePictureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            checkCameraPermission();
                        }
                    }
            );
        } else {
            takePictureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(v.getContext(), R.string.picture_landscape_only, Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
            );

        }
    }

    private void checkMyData() {
        MyDataDialogFragment myDataDialog = new MyDataDialogFragment();
        myDataDialog.show(getFragmentManager(), "mydata_dialog");
    }

    /**
     * Method to request permission for camera
     */
    private void requestCameraPermission() {
        // Camera and Write permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA);
    }

    public void takePicture() {
        if (bahnhof.getPhotoflag() != null) {
            return;
        }

        if (isMyDataIncomplete()) {
            checkMyData();
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = getCameraMediaFile();
            if (file != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "Deutschlands Bahnhöfe");
                intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, bahnhof.getTitle());
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "Kann keine Verzeichnisstruktur anlegen", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isMyDataIncomplete() {
        return DEFAULT.equals(nickname) || TextUtils.isEmpty(nickname);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.
            Log.i(TAG, "Received response for Camera permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Camera and Write permission has been granted, preview can be displayed
                enablePictureButton(true);
                takePicture();
            } else {
                //Permission not granted
                Toast.makeText(DetailsActivity.this, "You need to grant camera permission to use camera", Toast.LENGTH_LONG).show();
            }

        }
    }

    /**
     * Method to check permission
     */
    void checkCameraPermission() {
        boolean isGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Camera permission has not been granted.
                requestCameraPermission();
            } else {
                takePicture();
            }
        } else {
            takePicture();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // die Kamera-App sollte auf temporären Cache-Speicher schreiben, wir laden das Bild von
            // dort und schreiben es in Standard-Größe in den permanenten Speicher
            File cameraRawPictureFile = getCameraMediaFile();
            File storagePictureFile = getStoredMediaFile();
            if (cameraRawPictureFile == null || storagePictureFile == null) {
                Log.wtf(TAG, "Camera made a foto, but we're unable to reproduce where it should have gone");
                return;
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
            Log.d(TAG, "img width " + scaledScreen.getWidth());
            Log.d(TAG, "img height " + scaledScreen.getHeight());

            try {
                scaledScreen.compress(Bitmap.CompressFormat.JPEG, STORED_FOTO_QUALITY, new FileOutputStream(storagePictureFile));
                // temp file begone!
                cameraRawPictureFile.delete();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not write picture to destination file", e);
                Toast.makeText(getApplicationContext(), "Konnte das skalierte Bild nicht schreiben", Toast.LENGTH_LONG).show();
            }

            // , also provozieren wir das
            // Laden des Bildes von dort
            setLocalBitmap();
        }
    }


    /**
     * Get the base directory for storing fotos
     *
     * @return the File denoting the base directory.
     */
    @Nullable
    private File getMediaStorageDir() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Bahnhofsfotos");

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
        File file = new File(mediaStorageDir, String.format("%s-%d.jpg", nickname, bahnhof.getId()));
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
        File file = new File(temporaryStorageDir, String.format("%s-%d.jpg", nickname, bahnhof.getId()));
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
        String navLocation = "";
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
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
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }

                onBackPressed();
                break;
            case R.id.nav_to_station:
                startNavigation(DetailsActivity.this);
                //startNavigation(DetailsActivity.this);
                break;
            // action with ID action_settings was selected
            case R.id.send_email:
                Intent emailIntent = createFotoSendIntent();
                //emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "bahnhofsfotos@deutschlands-bahnhoefe.de" });
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{country.getEmail()});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bahnhofsfoto: " + bahnhof.getTitle());
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Lizenz: " + licence
                        + "\n selbst fotografiert: " + photoOwner
                        + "\n Nickname: " + nickname
                        + "\n Verlinken bitte mit: " + linking
                        + "\n Link zum Account: " + link);
                emailIntent.setType("multipart/byteranges");
                startActivity(Intent.createChooser(emailIntent, "Mail versenden"));
                break;
            case R.id.photo_upload:
                if (DEFAULT.equals(email) || DEFAULT.equals(token)) {
                    Toast.makeText(this, R.string.registration_needed, Toast.LENGTH_LONG).show();
                } else {
                    if (ConnectionUtil.checkInternetConnection(this)) {
                        new PhotoUploadTask(countryShortCode.toLowerCase(), nickname, email, bahnhof.getId(), getStoredMediaFile(), getString(R.string.rs_api_key), token).execute();
                    }
                }
                break;
            case R.id.share_photo:
                Intent shareIntent = createFotoSendIntent();
                shareIntent.putExtra(Intent.EXTRA_TEXT, country.getTwitterTags() + " " + bahnhof.getTitle());
                shareIntent.setType("image/jpeg");
                startActivity(createChooser(shareIntent, "send"));
                break;
            default:
                break;
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
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=Transit", bahnhof.getPosition().latitude, bahnhof.getPosition().longitude);
                        Log.d("findnavigation case 0", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;
                    case 1:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=d", bahnhof.getPosition().latitude, bahnhof.getPosition().longitude);
                        Log.d("findnavigation case 1", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;

                    case 2:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=b",
                                bahnhof.getPosition().latitude,
                                bahnhof.getPosition().longitude);
                        Log.d("findnavigation case 2", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;
                    case 3:
                        dlocation = String.format("google.navigation:ll=%s,%s&mode=w", bahnhof.getPosition().latitude, bahnhof.getPosition().longitude);
                        Log.d("findnavigation case 3", dlocation);
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dlocation));
                        break;
                    case 4:
                        dlocation = String.format("geo:%s,%s?q=%s", bahnhof.getPosition().latitude, bahnhof.getPosition().longitude, bahnhof.getTitle());
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
        if (fetchTask != null && fetchTask.getLicense() != null) {
            licenseTagView.setText(
                    String.format(
                            getText(R.string.license_tag).toString(),
                            fetchTask.getAuthor())
            );
            licenseTagView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Build an intent for an action to view the author
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                            Uri authorUri = fetchTask.getAuthorReference();
                            mapIntent.setData(authorUri);
                            startActivity(mapIntent);
                        }
                    }
            );
        } else {
            licenseTagView.setText("Lizenzinfo aktuell nicht lesbar");
        }

        setBitmap(publicBitmap);
    }

    /**
     * Fetch bitmap from device local location, if  it exists, and set the foto view.
     */
    private void setLocalBitmap() {
        takePictureButton.setVisibility(View.VISIBLE);

        Bitmap showBitmap = checkForLocalPhoto();
        if (showBitmap == null) {
            // lokal keine Bitmap
            localFotoUsed = false;
            // switch off image and license view until we actually have a foto
            imageView.setVisibility(View.INVISIBLE);
            licenseTagView.setVisibility(View.INVISIBLE);
            return;
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
            tvBahnhofName.setAlpha(alpha);
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
                    String.format("Media file not available for station %d and nickname %s ",
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
            ValueAnimator animation = ValueAnimator.ofFloat(tvBahnhofName.getAlpha(), 0f);
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
            ValueAnimator animation = ValueAnimator.ofFloat(tvBahnhofName.getAlpha(), 1.0f);
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public class PhotoUploadTask extends AsyncTask<Void, String, Integer> {

        private final String countryCode;
        private final String nickname;
        private final int stationId;
        private final File file;
        private final String apiKey;
        private final String token;
        private final String email;
        private ProgressDialog progressDialog;

        public PhotoUploadTask(String countryCode, String nickname, String email, int stationId, File file, String apiKey, String token) {
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

            publishProgress("Verbinde...");
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
                conn.setRequestProperty( "Station-Id", String.valueOf(stationId));
                conn.setRequestProperty( "Country", countryCode);
                conn.setUseCaches( false );

                wr = new DataOutputStream( conn.getOutputStream());
                is = new FileInputStream(file);
                byte[] buffer = new byte[8196];
                int bytesRead = 0;
                while ((bytesRead = is.read(buffer)) > 0) {
                    publishProgress("Sende...");
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
            } else {
                new SimpleDialogs().confirm(DetailsActivity.this, R.string.upload_failed);
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
            progressDialog.setMessage("Sende Daten ... " + values[0]);

        }

    }

}
