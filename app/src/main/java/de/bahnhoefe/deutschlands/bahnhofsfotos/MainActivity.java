package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.AppInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CustomAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DIALOG_TAG = "App Info Dialog";
    private static final long CHECK_UPDATE_INTERVAL = 10 * 60 * 1000; // 10 minutes
    private static final int REQUEST_FILE_PERMISSION = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    private BaseApplication baseApplication;
    private BahnhofsDbAdapter dbAdapter;
    private NavigationView navigationView;
    private MenuItem photoFilterMenuItem;

    private CustomAdapter customAdapter;
    private Cursor cursor;
    private String searchString;
    private ProgressBar progressBar;

    private NearbyNotificationService.StatusBinder statusBinder;
    private RSAPI rsapi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        rsapi = baseApplication.getRSAPI();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.fab_email)));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        progressBar = findViewById(R.id.progressBar);

        View header = navigationView.getHeaderView(0);
        TextView tvUpdate = header.findViewById(R.id.tvUpdate);

        if (!baseApplication.getFirstAppStart()) {
            startActivity(new Intent(this, IntroSliderActivity.class));
            finish();
        }

        long lastUpdateDate = baseApplication.getLastUpdate();
        if (lastUpdateDate > 0) {
            tvUpdate.setText(getString(R.string.last_update_at) + SimpleDateFormat.getDateTimeInstance().format(lastUpdateDate));
        } else {
            tvUpdate.setText(R.string.no_stations_in_database);
        }

        Intent searchIntent = getIntent();
        if (Intent.ACTION_SEARCH.equals(searchIntent.getAction())) {
            searchString = searchIntent.getStringExtra(SearchManager.QUERY);
        }

        updateStationList();
        bindToStatus();
    }

    private void onGalleryNavItemWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Write permission has not been granted.
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FILE_PERMISSION);
            } else {
                onGalleryNavItem();
            }
        } else {
            onGalleryNavItem();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_FILE_PERMISSION) {
            Log.i(TAG, "Received response for file permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Write permission has been granted, preview can be displayed
                onGalleryNavItem();
            } else {
                //Permission not granted
                Toast.makeText(MainActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onGalleryNavItem() {
        Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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

        final SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenu = menu.findItem(R.id.search);
        final SearchView search = (SearchView) searchMenu.getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit: " + s);
                searchString = s;
                updateStationList();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "onQueryTextChange: " + s);
                searchString = s;
                updateStationList();
                return false;
            }

        });

        photoFilterMenuItem = menu.findItem(R.id.menu_toggle_photo);
        photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());

        initNotificationMenuItem(menu.findItem(R.id.notify), false);

        final UpdatePolicy updatePolicy = baseApplication.getUpdatePolicy();
        menu.findItem(updatePolicy.getId()).setChecked(true);

        return true;
    }

    private void updateStationList() {
        try {
            int stationCount = dbAdapter.countBahnhoefe();
            cursor = dbAdapter.getBahnhofsListByKeyword(searchString, baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
            if (customAdapter != null) {
                customAdapter.swapCursor(cursor);
            } else {
                cursor = dbAdapter.getStationsList(baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
                customAdapter = new CustomAdapter(this, cursor, 0);
                ListView listView = findViewById(R.id.lstStations);
                assert listView != null;
                listView.setAdapter(customAdapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> listview, View view, int position, long id) {
                        Intent intentDetails = new Intent(MainActivity.this, DetailsActivity.class);
                        intentDetails.putExtra(DetailsActivity.EXTRA_BAHNHOF, dbAdapter.fetchBahnhofByRowId(id));
                        startActivityForResult(intentDetails, 0);
                    }
                });
            }
            TextView filterResult = findViewById(R.id.filter_result);
            filterResult.setText(getString(R.string.filter_result, customAdapter.getCount(), stationCount));
        } catch (Exception e) {
            Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e);
        }
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // necessary for the update policy submenu
        if(item.isChecked()) {
            item.setChecked(false);
        } else {
            item.setChecked(true);
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.countrySelection) {
            final Intent intent = new Intent(MainActivity.this, CountryActivity.class);
            startActivity(intent);
            item.setIcon(R.drawable.ic_language_white_24px);
        } else if (id == R.id.menu_toggle_photo) {
            PhotoFilter photoFilter = baseApplication.getPhotoFilter().getNextFilter();
            item.setIcon(photoFilter.getIcon());
            baseApplication.setPhotoFilter(photoFilter);
            updateStationList();
        } else if (id == R.id.nicknameFilter) {
            int selectedNickname = -1;
            final String[] nicknames = dbAdapter.getPhotographerNicknames();
            String selectedNick = baseApplication.getNicknameFilter();
            if (nicknames.length == 0) {
                Toast.makeText(getBaseContext(), getString(R.string.no_nicknames_found), Toast.LENGTH_LONG).show();
                return true;
            }
            for (int i = 0; i < nicknames.length; i++) {
                if (nicknames[i].equals(selectedNick)) {
                    selectedNickname = i;
                }
            }
            new SimpleDialogs().select(this, getString(R.string.select_nickname), nicknames, selectedNickname, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                    int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0 && nicknames.length > selectedPosition) {
                        baseApplication.setNicknameFilter(nicknames[selectedPosition]);
                        PhotoFilter photoFilter = PhotoFilter.NICKNAME;
                        baseApplication.setPhotoFilter(photoFilter);
                        item.setIcon(photoFilter.getIcon());
                        updateStationList();
                    }
                }
            });
        } else if (id == R.id.notify) {
            final Intent intent = new Intent(MainActivity.this, NearbyNotificationService.class);
            if (statusBinder == null) {
                startService(intent);
                bindToStatus();
            } else {
                stopService(intent);
            }
        } else if (id == R.id.rb_update_manual) {
            baseApplication.setUpdatePolicy(UpdatePolicy.MANUAL);
        } else if (id == R.id.rb_update_automatic) {
            baseApplication.setUpdatePolicy(UpdatePolicy.AUTOMATIC);
        } else if (id == R.id.rb_update_notify) {
            baseApplication.setUpdatePolicy(UpdatePolicy.NOTIFY);
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
            onGalleryNavItemWithPermissionCheck();
        } else if (id == R.id.nav_stations_map) {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_web_site) {
            Uri mapUri = Uri.parse("https://railway-stations.org");
            Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
            startActivity(intent);
        } else if (id == R.id.nav_app_info) {
            AppInfoFragment appInfoFragment = new AppInfoFragment();
            appInfoFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Run Multiple Async Tasks
     * <p>
     * from http://blogs.innovationm.com/multiple-asynctask-in-android/
     */
    private void runUpdateTasks() {
        progressBar.setVisibility(View.VISIBLE);

        rsapi.getCountries().enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                if (response.isSuccessful()) {
                    dbAdapter.insertCountries(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Country>> call, Throwable t) {
                Log.e(TAG, "Error refreshing countries", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_updating_countries) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        rsapi.getStations(baseApplication.getCountryCodes().toArray(new String[0])).enqueue(new Callback<List<Bahnhof>>() {
            @Override
            public void onResponse(Call<List<Bahnhof>> call, Response<List<Bahnhof>> response) {
                if (response.isSuccessful()) {
                    dbAdapter.deleteBahnhoefe();
                    dbAdapter.insertBahnhoefe(response.body());

                    baseApplication.setLastUpdate(System.currentTimeMillis());
                    TextView tvUpdate = findViewById(R.id.tvUpdate);
                    tvUpdate.setText(getString(R.string.last_update_at) + SimpleDateFormat.getDateTimeInstance().format(baseApplication.getLastUpdate()));
                    updateStationList();
                    baseApplication.migrateLocalPhotos();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<List<Bahnhof>> call, Throwable t) {
                Log.e(TAG, "Error refreshing stations", t);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getBaseContext(), getString(R.string.station_update_failed) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();

        if (baseApplication.getLastUpdate() == 0) {
            runUpdateTasks();
        } else if (System.currentTimeMillis() - baseApplication.getLastUpdate() > CHECK_UPDATE_INTERVAL) {
            baseApplication.setLastUpdate(System.currentTimeMillis());
            if (baseApplication.getUpdatePolicy() != UpdatePolicy.MANUAL) {
                for (final String country : baseApplication.getCountryCodes()) {
                    rsapi.getStatistic(country).enqueue(new Callback<Statistic>() {
                        @Override
                        public void onResponse(Call<Statistic> call, Response<Statistic> response) {
                            if (response.isSuccessful()) {
                                checkForUpdates(response.body(), country);
                            }
                        }

                        @Override
                        public void onFailure(Call<Statistic> call, Throwable t) {
                            Log.e(TAG, "Error loading country statistic", t);
                        }
                    });
                }
            }
        }

        if (photoFilterMenuItem != null) {
            photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());
        }
        updateStationList();
    }

    private void checkForUpdates(Statistic apiStat, String country) {
        if (apiStat == null) {
            return;
        }

        Statistic dbStat = dbAdapter.getStatistic(country);
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
