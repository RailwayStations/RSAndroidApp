package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.AppInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CustomAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import static java.lang.Integer.parseInt;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DIALOG_TAG = "App Info Dialog";
    private static final long CHECK_UPDATE_INTERVAL = 10 * 60 * 1000; // 10 minutes
    public final String TAG = "Bahnhoefe";
    private BaseApplication baseApplication;
    private BahnhofsDbAdapter dbAdapter;
    private NavigationView navigationView;
    private MenuItem photoFilterMenuItem;

    private CustomAdapter customAdapter;
    private Cursor cursor;

    private FirebaseAuth mFirebaseAuth;

    private NearbyNotificationService.StatusBinder statusBinder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.fab_email)));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        handleGalleryNavItem();

        View header = navigationView.getHeaderView(0);
        TextView tvUpdate = (TextView) header.findViewById(R.id.tvUpdate);

        if (!baseApplication.getFirstAppStart()) {
            startActivity(new Intent(this, IntroSliderActivity.class));
            finish();
        }

        long lastUpdateDate = baseApplication.getLastUpdate();
        if (lastUpdateDate > 0) {
            tvUpdate.setText("Letzte Aktualisierung am: " + SimpleDateFormat.getDateTimeInstance().format(lastUpdateDate));
        } else {
            tvUpdate.setText(R.string.no_stations_in_database);
        }

        cursor = dbAdapter.getStationsList(baseApplication.getPhotoFilter());
        customAdapter = new CustomAdapter(this, cursor, 0);
        ListView listView = (ListView) findViewById(R.id.lstStations);
        assert listView != null;
        listView.setAdapter(customAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listview, View view, int position, long id) {
                Bahnhof bahnhof = dbAdapter.fetchBahnhofByRowId(id);
                Class cls = DetailsActivity.class;
                Intent intentDetails = new Intent(MainActivity.this, cls);
                intentDetails.putExtra(DetailsActivity.EXTRA_BAHNHOF, bahnhof);
                startActivity(intentDetails);
            }
        });

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();

        Intent searchIntent = getIntent();
        if (Intent.ACTION_SEARCH.equals(searchIntent.getAction())) {
            String query = searchIntent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(MainActivity.this, query, Toast.LENGTH_SHORT).show();
        }

        bindToStatus();
    }


    private void handleGalleryNavItem() {
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Bahnhofsfotos");
        Log.d(TAG, file.toString());

        Menu menuNav = navigationView.getMenu();
        MenuItem nav_itemGallery = menuNav.findItem(R.id.nav_your_own_station_photos);

        if (file.isDirectory()) {
            String[] files = file.list();
            if (files == null) {
                nav_itemGallery.setEnabled(false);
            } else {
                nav_itemGallery.setEnabled(true);
            }
        } else {
            nav_itemGallery.setEnabled(false);
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit ");
                try {
                    cursor = dbAdapter.getBahnhofsListByKeyword(s, baseApplication.getPhotoFilter());
                    if (cursor == null) {
                        Toast.makeText(MainActivity.this, "No records found!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, cursor.getCount() + " records found!", Toast.LENGTH_LONG).show();
                    }
                    customAdapter.swapCursor(cursor);
                } catch (Exception e) {
                    Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "onQueryTextChange ");
                try {
                    cursor = dbAdapter.getBahnhofsListByKeyword(s, baseApplication.getPhotoFilter());
                    if (cursor != null) {
                        customAdapter.swapCursor(cursor);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e);
                }
                return false;
            }

        });

        photoFilterMenuItem = menu.findItem(R.id.menu_toggle_photo);
        photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());

        initNotificationMenuItem(menu.findItem(R.id.notify), false);

        return true;
    }

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     * <p>
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.notify);
        initNotificationMenuItem(item, statusBinder != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.countrySelection) {
            final Intent intent = new Intent(MainActivity.this, CountryActivity.class);
            startActivity(intent);
            item.setIcon(R.drawable.ic_language_white_24px);
        } else if (id == R.id.menu_toggle_photo) {
            PhotoFilter photoFilter = baseApplication.getPhotoFilter().getNextFilter();
            item.setIcon(photoFilter.getIcon());
            baseApplication.setPhotoFilter(photoFilter);
            cursor = dbAdapter.getStationsList(photoFilter);
            customAdapter.swapCursor(cursor);
        } else if (id == R.id.notify) {
            final Intent intent = new Intent(MainActivity.this, NearbyNotificationService.class);
            if (statusBinder == null) {
                startService(intent);
                bindToStatus();
            } else {
                stopService(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void initNotificationMenuItem(MenuItem item, boolean active) {
        item.setChecked(active);
        item.setIcon(active ? R.drawable.ic_notifications_active_white_24px : R.drawable.ic_notifications_off_white_24px);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_slideshow) {
            Intent intent = new Intent(MainActivity.this, IntroSliderActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_your_data) {
            Intent intent = new Intent(MainActivity.this, MyDataActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_update_photos) {
            runUpdateTasks();
        } else if (id == R.id.nav_highscore) {
            Intent intent = new Intent(MainActivity.this, HighScoreActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_your_own_station_photos) {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_stations_map) {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_web_site) {
            Intent intent = new Intent(MainActivity.this, RailwayStationsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_app_info) {
            AppInfoFragment appInfoFragment = new AppInfoFragment();
            appInfoFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        } else if (id == R.id.nav_user_register) {
            if (mFirebaseAuth.getCurrentUser() == null) {
                Intent intentSignIn = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intentSignIn);
            } else {
                Intent intentAuth = new Intent(MainActivity.this, AuthActivity.class);
                startActivity(intentAuth);
            }

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public class JSONTask extends AsyncTask<Void, String, List<Bahnhof>> {

        private final String countryCode;
        private ProgressDialog progressDialog;
        private Exception exception;

        // from https://developer.android.com/training/efficient-downloads/redundant_redundant.html
        private void enableHttpResponseCache() {
            try {
                long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                File httpCacheDir = new File(getCacheDir(), "http");
                Class.forName("android.net.http.HttpResponseCache")
                        .getMethod("install", File.class, long.class)
                        .invoke(null, httpCacheDir, httpCacheSize);
            } catch (Exception httpResponseCacheNotAvailable) {
                Log.i(TAG, "HTTP response cache is unavailable.");
            }
        }

        protected JSONTask(final String countryCode) {
            enableHttpResponseCache();
            this.countryCode = countryCode;
        }

        @Override
        protected List<Bahnhof> doInBackground(Void... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            Date date = new Date();
            long aktuellesDatum = date.getTime();
            int count = 0;

            publishProgress("Verbinde...");
            List<Bahnhof> bahnhoefe = new ArrayList<>(count);
            try {
                URL url = new URL(String.format("%s/%s/stations", Constants.API_START_URL, countryCode.toLowerCase()));
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                publishProgress("Lese...");
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString();

                publishProgress("Verarbeite...");
                JSONArray bahnhofList = new JSONArray(finalJson);
                count = bahnhofList.length();
                Log.i(TAG, "Parsed " + count + " stations");

                for (int i = 0; i < bahnhofList.length(); i++) {
                    publishProgress("Verarbeite " + i + "/" + count);
                    JSONObject jsonObj = (JSONObject) bahnhofList.get(i);

                    String title = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_TITLE);
                    String id = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_ID);
                    String lat = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_LAT);
                    String lon = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_LON);
                    String photoUrl = getNullableString(jsonObj, Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL);
                    String photographer = getNullableString(jsonObj, Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER);
                    String photographerUrl = getNullableString(jsonObj, Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER_URL);
                    String license = getNullableString(jsonObj, Constants.DB_JSON_CONSTANTS.KEY_LICENSE);
                    String ds100 = getNullableString(jsonObj, Constants.DB_JSON_CONSTANTS.KEY_DS100);

                    Bahnhof bahnhof = new Bahnhof();
                    bahnhof.setTitle(title);
                    bahnhof.setId(parseInt(id));
                    bahnhof.setLat(Float.parseFloat(lat));
                    bahnhof.setLon(Float.parseFloat(lon));
                    bahnhof.setDatum(aktuellesDatum);
                    bahnhof.setPhotoUrl(photoUrl);
                    bahnhof.setPhotographer(photographer);
                    bahnhof.setPhotographerUrl(photographerUrl);
                    bahnhof.setLicense(license);
                    bahnhof.setDS100(ds100);

                    bahnhoefe.add(bahnhof);
                    Log.d("DatenbankInsertOk ...", bahnhof.toString());
                }
                publishProgress("Schreibe in Datenbank");

                dbAdapter.insertBahnhoefe(bahnhoefe);
                publishProgress("Bahnhöfe Datenbank aktualisiert");
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing stations", e);
                exception = e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close reader", e);
                }
            }
            return bahnhoefe;
        }

        private String getNullableString(JSONObject jsonObj, String name) {
            if (jsonObj.isNull(name)) {
                return null;
            }
            return jsonObj.optString(name, null);
        }

        @Override
        protected void onPostExecute(List<Bahnhof> result) {
            if (MainActivity.this.isDestroyed()) {
                return;
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (exception != null) {
                Toast.makeText(getBaseContext(), "Fehler beim Aktualisieren der Bahnhöfe: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                baseApplication.setLastUpdate(System.currentTimeMillis());
                TextView tvUpdate = (TextView) findViewById(R.id.tvUpdate);
                try {
                    tvUpdate.setText("Letzte Aktualisierung am: " + SimpleDateFormat.getDateTimeInstance().format(baseApplication.getLastUpdate()));
                } catch (Exception e) {
                    Log.e(TAG, "Error writing updatedate.txt", e);
                }
                customAdapter.swapCursor(dbAdapter.getStationsList(baseApplication.getPhotoFilter()));
            }

            unlockScreenOrientation();
        }

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setIndeterminate(false);

            // show it
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage("Lade Daten ... " + values[0]);

        }

        private void lockScreenOrientation() {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        private void unlockScreenOrientation() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    public class JSONLaenderTask extends AsyncTask<Void, String, List<Country>> {

        private Exception exception;

        protected List<Country> doInBackground(Void... params) {
            int count = 0;

            List<Country> countries = new ArrayList<>(count);
            try {
                URL url = new URL(Constants.API_START_URL + "/countries");
                HttpURLConnection laenderConnection = (HttpURLConnection) url.openConnection();
                laenderConnection.connect();
                InputStream stream = laenderConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString();

                publishProgress("Verarbeite Länder...");
                JSONArray countryList = new JSONArray(finalJson);
                count = countryList.length();
                Log.i(TAG, "Parsed " + count + " countries");

                for (int i = 0; i < countryList.length(); i++) {
                    publishProgress("Verarbeite " + i + "/" + count);
                    JSONObject jsonObj = (JSONObject) countryList.get(i);

                    String countryShortCode = jsonObj.getString("code").toUpperCase();
                    String countryName = jsonObj.getString("name");
                    String email = jsonObj.getString("email");
                    String twitterTags = jsonObj.getString("twitterTags");
                    String timetableUrlTemplate = jsonObj.getString("timetableUrlTemplate");

                    Country country = new Country();
                    country.setCountryShortCode(countryShortCode);
                    country.setCountryName(countryName);
                    country.setEmail(email);
                    country.setTwitterTags(twitterTags);
                    country.setTimetableUrlTemplate(timetableUrlTemplate);

                    countries.add(country);
                    Log.d("DatenbankInsertLdOk ...", country.toString());
                }
                publishProgress("Schreibe in Datenbank");
                dbAdapter.insertCountries(countries);
                publishProgress("Datenbank " + countries + " Ländern aktualisiert");
            } catch (final Exception e) {
                Log.e(TAG, "Error loading countries", e);
                exception = e;
            }

            return countries;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List<Country> countries) {
            recreate();
            if (exception != null) {
                Toast.makeText(getBaseContext(), "Fehler beim Aktualisieren der Länderdaten: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public class JSONStatisticTask extends AsyncTask<Void, String, Statistic> {

        private final String countryCode;

        protected JSONStatisticTask(final String countryCode) {
            this.countryCode = countryCode;
        }

        protected Statistic doInBackground(Void... params) {
            try {
                URL url = new URL(Constants.API_START_URL + "/" + countryCode.toLowerCase() + "/stats");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString();

                publishProgress("Verarbeite Statistik...");
                JSONObject statsJson = new JSONObject(finalJson);
                Statistic statistic = new Statistic(statsJson.getInt("total"),
                                            statsJson.getInt("withPhoto"),
                                            statsJson.getInt("withoutPhoto"),
                                            statsJson.getInt("photographers"));

                Log.i(TAG, "Stat: " + statistic);
                return statistic;
            } catch (final Exception e) {
                Log.e(TAG, "Error loading countries", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Statistic statistic) {
            checkForUpdates(statistic);
        }

    }

    /**
     * Run Multiple Async Tasks
     * <p>
     * from http://blogs.innovationm.com/multiple-asynctask-in-android/
     */
    private void runUpdateTasks() {
        if (ConnectionUtil.checkInternetConnection(this)) {
            // First Task
            new JSONTask(baseApplication.getCountryShortCode()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

            // Second Task
            new JSONLaenderTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        handleGalleryNavItem();
        if (baseApplication.getLastUpdate() == 0) {
            runUpdateTasks();
        } else if (System.currentTimeMillis() - baseApplication.getLastUpdate() > CHECK_UPDATE_INTERVAL) {
            baseApplication.setLastUpdate(System.currentTimeMillis());
            if (baseApplication.getUpdatePolicy() != UpdatePolicy.MANUAL) {
                new JSONStatisticTask(baseApplication.getCountryShortCode()).execute();
            }
        }

        if (photoFilterMenuItem != null) {
            photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());
        }
        if (customAdapter != null) {
            cursor = dbAdapter.getStationsList(baseApplication.getPhotoFilter());
            customAdapter.swapCursor(cursor);
        }
    }

    private void checkForUpdates(Statistic apiStat) {
        if (apiStat == null) {
            return;
        }

        Statistic dbStat = dbAdapter.getStatistic();
        Log.d(TAG, "DbStat: " + dbStat);
        if (apiStat.getTotal() != dbStat.getTotal() || apiStat.getWithPhoto() != dbStat.getWithPhoto() || apiStat.getWithoutPhoto() != dbStat.getWithoutPhoto()) {
            if (baseApplication.getUpdatePolicy() == UpdatePolicy.AUTOMATIC) {
                runUpdateTasks();
            } else {
                new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.update_available)
                        .setCancelable(true)
                        .setPositiveButton(R.string.button_ok_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                runUpdateTasks();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.button_cancel_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
            }
        }
    }

    private void bindToStatus() {
        Intent intent = new Intent(this, NearbyNotificationService.class);
        intent.setAction(NearbyNotificationService.STATUS_INTERFACE);
        if (!this.getApplicationContext().bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Bound to status service of NearbyNotificationService");
                statusBinder = (NearbyNotificationService.StatusBinder) service;
                invalidateOptionsMenu();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Unbound from status service of NearbyNotificationService");
                statusBinder = null;
                invalidateOptionsMenu();
            }
        }, 0))

        Log.e(TAG, "Bind request to statistics interface failed");
    }

}
