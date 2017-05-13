package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
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
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.AppInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CustomAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import static java.lang.Integer.parseInt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DIALOG_TAG = "App Info Dialog";
    public final String TAG = "Bahnhoefe";
    private BahnhofsDbAdapter dbAdapter;
    private String lastUpdateDate;
    private NavigationView navigationView;
    private Boolean firstAppStart;

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

        BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        firstAppStart = baseApplication.getFirstAppStart();


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

        if(firstAppStart==false){
            Intent introSliderIntent = new Intent(MainActivity.this,IntroSliderActivity.class);
            startActivity(introSliderIntent);
            finish();
        }

        try {
            lastUpdateDate = loadUpdateDateFromFile("updatedate.txt");
        } catch (Exception e) {
            Log.e(TAG, "Cannot load last update", e);
        }
        if (!lastUpdateDate.equals("")) {
            tvUpdate.setText("Letzte Aktualisierung am: " + lastUpdateDate);
        }else {
            disableNavItem();
            tvUpdate.setText(R.string.no_stations_in_database);
            //runMultipleAsyncTask();
        }

        cursor = dbAdapter.getStationsList(false);
        customAdapter = new CustomAdapter(this, cursor,0);
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
                finish();
            }
        });

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();


        Intent searchIntent = getIntent();
        if(Intent.ACTION_SEARCH.equals(searchIntent.getAction())){
            String query = searchIntent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(MainActivity.this,query,Toast.LENGTH_SHORT).show();
        }

        bindToStatus();

    }


    private void handleGalleryNavItem() {

        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Bahnhofsfotos");
        Log.d(TAG, file.toString());

        Menu menuNav=navigationView.getMenu();
        MenuItem nav_itemGallery = menuNav.findItem(R.id.nav_your_own_station_photos);

        if (file.isDirectory()) {
            String[] files = file.list();
            if (files == null) {
                //directory is empty
                nav_itemGallery.setEnabled(false);
            }else{
                nav_itemGallery.setEnabled(true);
            }
        }else{
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
        MenuItem item = menu.findItem(R.id.search);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
            search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));

            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                @Override
                public boolean onQueryTextSubmit(String s) {
                    Log.d(TAG, "onQueryTextSubmit ");
                    try {
                        cursor = dbAdapter.getBahnhofsListByKeyword(s, false);
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
                        cursor = dbAdapter.getBahnhofsListByKeyword(s, false);
                        if (cursor != null) {
                            customAdapter.swapCursor(cursor);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e);
                    }
                    return false;
                }

            });

        }

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
        if (statusBinder != null) {
            MenuItem item = menu.findItem(R.id.notify);
            boolean active = statusBinder.isNotificationTrackingActive();
            item.setChecked(active);
            item.setIcon(active ? R.drawable.ic_notifications_active_white_24px : R.drawable.ic_notifications_off_white_24px);

        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id==R.id.countrySelection){
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, CountryActivity.class);
            startActivity(intent);
            item.setIcon(R.drawable.ic_language_white_24px);
        } else if (id == R.id.notify) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, NearbyNotificationService.class);
            boolean active = statusBinder != null ? statusBinder.isNotificationTrackingActive() : false;
            if (!active) {
                startService(intent);
                item.setChecked(true);
                item.setIcon(R.drawable.ic_notifications_active_white_24px);
            } else {
                stopService(intent);
                item.setChecked(false);
                item.setIcon(R.drawable.ic_notifications_off_white_24px);
            }
        }

        return super.onOptionsItemSelected(item);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if(id==R.id.nav_slideshow){
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, IntroSliderActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_your_data) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, MyDataActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_update_photos) {
            runMultipleAsyncTask();
        } else if (id == R.id.nav_highscore) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, HighScoreActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_your_own_station_photos) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_stations_without_photo) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, MapsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_all_stations_without_photo) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, MapsAllActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_web_site) {
            Intent intent = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, RailwayStationsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_app_info) {
            AppInfoFragment appInfoFragment = new AppInfoFragment();
            appInfoFragment.show(getSupportFragmentManager(),DIALOG_TAG);
        } else if (id == R.id.nav_user_register) {
            if(mFirebaseAuth.getCurrentUser() == null){
                Intent intentSignIn = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, SignInActivity.class);
                startActivity(intentSignIn);
            }else {
                Intent intentAuth = new Intent(de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity.this, AuthActivity.class);
                startActivity(intentAuth);
            }

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void enableNavItem() {
        // after loading stations enable menu to start MapsActivity
        Menu menuNav=navigationView.getMenu();
        MenuItem nav_item2 = menuNav.findItem(R.id.nav_stations_without_photo);
        MenuItem nav_item3 = menuNav.findItem(R.id.nav_all_stations_without_photo);
        nav_item3.setEnabled(true);
        nav_item2.setEnabled(true);
    }

    private void disableNavItem(){
        // if there are no stations available, disable the menu to start MapsActivity
        Menu menuNav = navigationView.getMenu();
        MenuItem nav_item2 = menuNav.findItem(R.id.nav_stations_without_photo);
        MenuItem nav_item3 = menuNav.findItem(R.id.nav_all_stations_without_photo);
        nav_item3.setEnabled(false);
        nav_item2.setEnabled(false);
    }

    public class JSONTask extends AsyncTask<Void, String, List<Bahnhof>>{

        private final String countryCode;
        private ProgressDialog progressDialog;
        private Date lastUpdateDate;

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

        protected JSONTask(@Nullable final String lastUpdateDate, final String countryCode) {
            enableHttpResponseCache();
            this.lastUpdateDate = null;
            this.countryCode = countryCode;
            if (lastUpdateDate != null) {
                try {
                    new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(lastUpdateDate);
                } catch (ParseException e) {
                    Log.e(TAG, "Unparsable update date: " + lastUpdateDate);
                }
            }
        }

        @Override
        protected List<Bahnhof> doInBackground(Void ...params) {
            dbAdapter.deleteBahnhoefe();
            dbAdapter.deleteCountries();
            List<Bahnhof> ohne = loadBatch(true);
            List<Bahnhof> mit = loadBatch(false);
            if (ohne != null && mit != null)
            {
                ohne.addAll(mit);
            }
            return ohne;
        }

        protected List<Bahnhof> loadBatch(boolean withPhotos) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            Date date = new Date();
            long aktuellesDatum = date.getTime();
            int count = 0;

            publishProgress("Verbinde...");
            try {
                URL url = new URL(String.format("%s/%s/%s%s", Constants.API_START_URL, countryCode.toLowerCase(), Constants.BAHNHOEFE_END_URL, withPhotos));
                connection = (HttpURLConnection)url.openConnection();
                connection.connect();
                long resourceDate = connection.getHeaderFieldDate("Last-Modified", aktuellesDatum);
                if (lastUpdateDate == null || resourceDate > lastUpdateDate.getTime()) {
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
                    try {
                        JSONArray bahnhofList = new JSONArray(finalJson);
                        count = bahnhofList.length();
                        Log.i(TAG, "Parsed " + count + " stations with" + (withPhotos ? "" : "out") + " a photo");
                        List<Bahnhof> bahnhoefe = new ArrayList<Bahnhof>(count);

                        for (int i = 0; i < bahnhofList.length(); i++) {
                            publishProgress("Verarbeite " + i + "/" + count);
                            JSONObject jsonObj = (JSONObject) bahnhofList.get(i);

                            String title = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_TITLE);
                            String id = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_ID);
                            String lat = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_LAT);
                            String lon = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_LON);

                            Bahnhof bahnhof = new Bahnhof();
                            bahnhof.setTitle(title);
                            bahnhof.setId(parseInt(id));
                            bahnhof.setLat(Float.parseFloat(lat));
                            bahnhof.setLon(Float.parseFloat(lon));
                            bahnhof.setDatum(aktuellesDatum);
                            bahnhof.setPhotoflag(withPhotos ? "x" : null);

                            bahnhoefe.add(bahnhof);
                            Log.d("DatenbankInsertOk ...", bahnhof.toString());
                        }
                        publishProgress("Schreibe in Datenbank");
                        dbAdapter.insertBahnhoefe(bahnhoefe);
                        publishProgress("Datenbank " + (withPhotos ? "mit" : "ohne") + " Photos aktualisiert");
                        return bahnhoefe;

                    } catch (JSONException e) {
                        Log.e(TAG, "Mal formatted Json", e);
                    }
                } // Online-Resource ist  neuer als unsere Daten
            } catch (IOException e) {
                Log.e(TAG, "Could not read json files", e);
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
                try {
                    if(reader != null){
                        reader.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close reader", e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Bahnhof> result) {
            if (MainActivity.this.isDestroyed()) {
                return;
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            writeUpdateDateInFile();
            enableNavItem();
            TextView tvUpdate = (TextView) findViewById(R.id.tvUpdate);
            try {
                tvUpdate.setText("Letzte Aktualisierung am: " + loadUpdateDateFromFile("updatedate.txt") );
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD){
                customAdapter.swapCursor(dbAdapter.getStationsList(false));
            } else {
                customAdapter.changeCursor(dbAdapter.getStationsList(false));
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

        private ProgressDialog progressDialog;

        protected List<Country> doInBackground(Void... params) {
            URL url = null;
            HttpURLConnection laenderConnection = null;
            BufferedReader reader = null;
            int count = 0;

            try {
                url = new URL(Constants.LAENDERDATEN_URL);
                laenderConnection = (HttpURLConnection) url.openConnection();
                laenderConnection.connect();
                InputStream stream = laenderConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
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
                List<Country> countries = new ArrayList<Country>(count);

                for (int i = 0; i < countryList.length(); i++) {
                    publishProgress("Verarbeite " + i + "/" + count);
                    JSONObject jsonObj = (JSONObject) countryList.get(i);

                    String countryShortCode = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE);
                    String countryName = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME);
                    String email = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_EMAIL);
                    String twitterTags = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS);

                    Country country = new Country();
                    country.setCountryShortCode(countryShortCode);
                    country.setCountryName(countryName);
                    country.setEmail(email);
                    country.setTwitterTags(twitterTags);

                    countries.add(country);
                    Log.d("DatenbankInsertLdOk ...", country.toString());
                }
                publishProgress("Schreibe in Datenbank");
                dbAdapter.insertCountries(countries);
                publishProgress("Datenbank " + countries + " Ländern aktualisiert");
                return countries;
            } catch (final Exception e) {
                Log.e(TAG, "Error loading countries", e);
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List<Country> countries) {
            recreate();
        }
    }

    /**
     * Run Multiple Async Tasks
     *
     * from http://blogs.innovationm.com/multiple-asynctask-in-android/
     */
    private void runMultipleAsyncTask() {
        // First Task
        new JSONTask(lastUpdateDate, ((BaseApplication)getApplication()).getCountryShortCode()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        // Second Task
        new JSONLaenderTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

    }


    @Override
    public void onResume() {
        super.onResume();
        handleGalleryNavItem();
        if (lastUpdateDate.equals("")) {
            disableNavItem();
            //tvUpdate.setText(R.string.no_stations_in_database);
            runMultipleAsyncTask();
        }

    }

    @Nullable
    private String writeUpdateDateInFile() {

        try {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            final String lastUpdateDate = df.format(c.getTime());
            FileOutputStream updateDate = openFileOutput("updatedate.txt",MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(updateDate);
            try {
                osw.write(lastUpdateDate);
                osw.flush();
                osw.close();
                Toast.makeText(getBaseContext(),"Aktualisierungsdatum gespeichert", Toast.LENGTH_LONG).show();
                return lastUpdateDate;
            } catch (IOException ioe) {
                Log.e(TAG, ioe.toString());
            }
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG,fnfe.toString());
        }
        return null;
    }

    public String loadUpdateDateFromFile(String filename) throws Exception{
        String retString = "";
        BufferedReader reader = null;
        try{
            FileInputStream in = this.openFileInput(filename);
            reader = new BufferedReader(new InputStreamReader(in));
            String zeile;
            while ((zeile = reader.readLine()) != null){
                retString += zeile;
            }
            reader.close();

        }catch (FileNotFoundException fnfe){
            Log.e(TAG,fnfe.toString());
        }
        return retString;
    }

    private void bindToStatus() {
        Intent intent = new Intent(this, NearbyNotificationService.class);
        intent.setAction(NearbyNotificationService.STATUS_INTERFACE);
        if (!this.getApplicationContext().bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Bound to status service of NearbyNotificationService");
                statusBinder = (NearbyNotificationService.StatusBinder)service;
                invalidateOptionsMenu();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Unbound from status service of NearbyNotificationService");
                statusBinder = null;
            }
        }, 0))
            Log.e(TAG, "Bind request to statistics interface failed");
    }


}
