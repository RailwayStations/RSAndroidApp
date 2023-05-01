package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMainBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.StationListAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.AppInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.StationFilterBar;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LocationListener, NavigationView.OnNavigationItemSelectedListener, StationFilterBar.OnChangeListener {

    private static final String DIALOG_TAG = "App Info Dialog";
    private static final long CHECK_UPDATE_INTERVAL = 10 * 60 * 1000; // 10 minutes
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_FINE_LOCATION = 1;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1000;
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 500; // minute

    private BaseApplication baseApplication;
    private DbAdapter dbAdapter;

    private ActivityMainBinding binding;

    private StationListAdapter stationListAdapter;
    private String searchString;

    private NearbyNotificationService.StatusBinder statusBinder;
    private RSAPIClient rsapiClient;
    private Location myPos;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        rsapiClient = baseApplication.getRsapiClient();

        var toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.appBarMain.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        var header = binding.navView.getHeaderView(0);
        TextView tvUpdate = header.findViewById(R.id.tvUpdate);

        if (!baseApplication.getFirstAppStart()) {
            startActivity(new Intent(this, IntroSliderActivity.class));
            finish();
        }

        var lastUpdateDate = baseApplication.getLastUpdate();
        if (lastUpdateDate > 0) {
            tvUpdate.setText(getString(R.string.last_update_at, SimpleDateFormat.getDateTimeInstance().format(lastUpdateDate)));
        } else {
            tvUpdate.setText(R.string.no_stations_in_database);
        }

        var searchIntent = getIntent();
        if (Intent.ACTION_SEARCH.equals(searchIntent.getAction())) {
            searchString = searchIntent.getStringExtra(SearchManager.QUERY);
        }

        myPos = baseApplication.getLastLocation();
        bindToStatus();

        binding.appBarMain.main.pullToRefresh.setOnRefreshListener(() -> {
            runUpdateCountriesAndStations();
            binding.appBarMain.main.pullToRefresh.setRefreshing(false);
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        var manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        var searchMenu = menu.findItem(R.id.search);
        var search = (SearchView) searchMenu.getActionView();
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

        var updatePolicy = baseApplication.getUpdatePolicy();
        menu.findItem(updatePolicy.getId()).setChecked(true);

        return true;
    }

    private void updateStationList() {
        try {
            var sortByDistance = baseApplication.getSortByDistance() && myPos != null;
            var stationCount = dbAdapter.countStations(baseApplication.getCountryCodes());
            var cursor = dbAdapter.getStationsListByKeyword(searchString, baseApplication.getStationFilter(), baseApplication.getCountryCodes(), sortByDistance, myPos);
            if (stationListAdapter != null) {
                stationListAdapter.swapCursor(cursor);
            } else {
                stationListAdapter = new StationListAdapter(this, cursor, 0);
                binding.appBarMain.main.lstStations.setAdapter(stationListAdapter);

                binding.appBarMain.main.lstStations.setOnItemClickListener((listview, view, position, id) -> {
                    var intentDetails = new Intent(MainActivity.this, DetailsActivity.class);
                    intentDetails.putExtra(DetailsActivity.EXTRA_STATION, dbAdapter.fetchStationByRowId(id));
                    startActivity(intentDetails);
                });
            }
            binding.appBarMain.main.filterResult.setText(getString(R.string.filter_result, stationListAdapter.getCount(), stationCount));
        } catch (Exception e) {
            Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // necessary for the update policy submenu
        item.setChecked(!item.isChecked());

        if (id == R.id.rb_update_manual) {
            baseApplication.setUpdatePolicy(UpdatePolicy.MANUAL);
        } else if (id == R.id.rb_update_automatic) {
            baseApplication.setUpdatePolicy(UpdatePolicy.AUTOMATIC);
        } else if (id == R.id.rb_update_notify) {
            baseApplication.setUpdatePolicy(UpdatePolicy.NOTIFY);
        } else if (id == R.id.apiUrl) {
            showApiUrlDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showApiUrlDialog() {
        SimpleDialogs.prompt(this, R.string.apiUrl, EditorInfo.TYPE_TEXT_VARIATION_URI, R.string.api_url_hint, baseApplication.getApiUrl(), v -> {
            baseApplication.setApiUrl(v);
            baseApplication.setLastUpdate(0);
            recreate();
        });
    }

    private void setNotificationIcon(boolean active) {
        var item = binding.navView.getMenu().findItem(R.id.nav_notification);
        item.setIcon(ContextCompat.getDrawable(this, active ? R.drawable.ic_notifications_active_gray_24px : R.drawable.ic_notifications_off_gray_24px));
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_slideshow) {
            startActivity(new Intent(this, IntroSliderActivity.class));
            finish();
        } else if (id == R.id.nav_your_data) {
            startActivity(new Intent(this, MyDataActivity.class));
        } else if (id == R.id.nav_update_photos) {
            runUpdateCountriesAndStations();
        } else if (id == R.id.nav_notification) {
            toggleNotification();
        } else if (id == R.id.nav_highscore) {
            startActivity(new Intent(this, HighScoreActivity.class));
        } else if (id == R.id.nav_outbox) {
            startActivity(new Intent(this, OutboxActivity.class));
        } else if (id == R.id.nav_inbox) {
            startActivity(new Intent(this, InboxActivity.class));
        } else if (id == R.id.nav_stations_map) {
            startActivity(new Intent(this, MapsActivity.class));
        } else if (id == R.id.nav_web_site) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://railway-stations.org")));
        } else if (id == R.id.nav_email) {
            var emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.fab_email)));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
        } else if (id == R.id.nav_app_info) {
            new AppInfoFragment().show(getSupportFragmentManager(), DIALOG_TAG);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void runUpdateCountriesAndStations() {
        binding.appBarMain.main.progressBar.setVisibility(View.VISIBLE);

        rsapiClient.runUpdateCountriesAndStations(this, baseApplication, success -> {
            if (success) {
                TextView tvUpdate = findViewById(R.id.tvUpdate);
                tvUpdate.setText(getString(R.string.last_update_at, SimpleDateFormat.getDateTimeInstance().format(baseApplication.getLastUpdate())));
                updateStationList();
            }
            binding.appBarMain.main.progressBar.setVisibility(View.GONE);
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterLocationManager();
    }

    @Override
    public void onResume() {
        super.onResume();

        for (int i = 0; i < binding.navView.getMenu().size(); i++) {
            binding.navView.getMenu().getItem(i).setChecked(false);
        }

        if (baseApplication.getLastUpdate() == 0) {
            runUpdateCountriesAndStations();
        } else if (System.currentTimeMillis() - baseApplication.getLastUpdate() > CHECK_UPDATE_INTERVAL) {
            baseApplication.setLastUpdate(System.currentTimeMillis());
            if (baseApplication.getUpdatePolicy() != UpdatePolicy.MANUAL) {
                for (var country : baseApplication.getCountryCodes()) {
                    rsapiClient.getStatistic(country).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Statistic> call, @NonNull Response<Statistic> response) {
                            if (response.isSuccessful()) {
                                checkForUpdates(response.body(), country);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Statistic> call, @NonNull Throwable t) {
                            Log.e(TAG, "Error loading country statistic", t);
                        }
                    });
                }
            }
        }

        if (baseApplication.getSortByDistance()) {
            registerLocationManager();
        }

        binding.appBarMain.main.stationFilterBar.setBaseApplication(baseApplication);
        binding.appBarMain.main.stationFilterBar.setOnChangeListener(this);
        updateStationList();
    }

    private void checkForUpdates(Statistic statistic, String country) {
        if (statistic == null) {
            return;
        }

        var dbStat = dbAdapter.getStatistic(country);
        Log.d(TAG, "DbStat: " + dbStat);
        if (statistic.getTotal() != dbStat.getTotal() || statistic.getWithPhoto() != dbStat.getWithPhoto() || statistic.getWithoutPhoto() != dbStat.getWithoutPhoto()) {
            if (baseApplication.getUpdatePolicy() == UpdatePolicy.AUTOMATIC) {
                runUpdateCountriesAndStations();
            } else {
                new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.update_available)
                        .setCancelable(true)
                        .setPositiveButton(R.string.button_ok_text, (dialog, which) -> {
                            runUpdateCountriesAndStations();
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.button_cancel_text, (dialog, which) -> dialog.dismiss())
                        .create().show();
            }
        }
    }

    private void bindToStatus() {
        var intent = new Intent(this, NearbyNotificationService.class);
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
        }, 0)) {
            Log.e(TAG, "Bind request to statistics interface failed");
        }
    }

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(MainActivity.this, R.string.notification_permission_needed, Toast.LENGTH_SHORT).show();
                }
            });

    public void toggleNotification() {
        if (statusBinder == null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        toggleNotificationWithPermissionGranted();
    }

    private void toggleNotificationWithPermissionGranted() {
        var intent = new Intent(MainActivity.this, NearbyNotificationService.class);
        if (statusBinder == null) {
            startService(intent);
            bindToStatus();
            setNotificationIcon(true);
        } else {
            stopService(intent);
            setNotificationIcon(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            Log.i(TAG, "Received response for location permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission has been granted
                registerLocationManager();
            } else {
                //Permission not granted
                baseApplication.setSortByDistance(false);
                binding.appBarMain.main.stationFilterBar.setSortOrder(false);
            }
        }
    }

    public void registerLocationManager() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
                return;
            }

            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            // getting GPS status
            var isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d(TAG, "GPS Enabled");
                if (locationManager != null) {
                    myPos = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            } else {
                // getting network status
                var isNetworkEnabled = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d(TAG, "Network Location enabled");
                    if (locationManager != null) {
                        myPos = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering LocationManager", e);
            var b = new Bundle();
            b.putString("error", "Error registering LocationManager: " + e);
            locationManager = null;
            baseApplication.setSortByDistance(false);
            binding.appBarMain.main.stationFilterBar.setSortOrder(false);
            return;
        }
        Log.i(TAG, "LocationManager registered");
        onLocationChanged(myPos);
    }

    private void unregisterLocationManager() {
        if (locationManager != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(this);
            }
            locationManager = null;
        }
        Log.i(TAG, "LocationManager unregistered");
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        myPos = location;
        updateStationList();
    }

    @Override
    public void stationFilterChanged(StationFilter stationFilter) {
        baseApplication.setStationFilter(stationFilter);
        updateStationList();
    }

    @Override
    public void sortOrderChanged(boolean sortByDistance) {
        if (sortByDistance) {
            registerLocationManager();
        }
        updateStationList();
    }

}
