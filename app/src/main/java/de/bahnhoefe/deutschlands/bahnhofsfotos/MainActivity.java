package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.AppInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.StationFilterBar;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMainBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.StationListAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
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
    private RSAPI rsapi;
    private Location myPos;
    private LocationManager locationManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        rsapi = baseApplication.getRSAPI();

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.appBarMain.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        final View header = binding.navView.getHeaderView(0);
        final TextView tvUpdate = header.findViewById(R.id.tvUpdate);

        if (!baseApplication.getFirstAppStart()) {
            startActivity(new Intent(this, IntroSliderActivity.class));
            finish();
        }

        final long lastUpdateDate = baseApplication.getLastUpdate();
        if (lastUpdateDate > 0) {
            tvUpdate.setText(getString(R.string.last_update_at) + SimpleDateFormat.getDateTimeInstance().format(lastUpdateDate));
        } else {
            tvUpdate.setText(R.string.no_stations_in_database);
        }

        final Intent searchIntent = getIntent();
        if (Intent.ACTION_SEARCH.equals(searchIntent.getAction())) {
            searchString = searchIntent.getStringExtra(SearchManager.QUERY);
        }

        myPos = baseApplication.getLastLocation();
        bindToStatus();
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
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        final SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenu = menu.findItem(R.id.search);
        final SearchView search = (SearchView) searchMenu.getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(final String s) {
                Log.d(TAG, "onQueryTextSubmit: " + s);
                searchString = s;
                updateStationList();
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String s) {
                Log.d(TAG, "onQueryTextChange: " + s);
                searchString = s;
                updateStationList();
                return false;
            }

        });

        final UpdatePolicy updatePolicy = baseApplication.getUpdatePolicy();
        menu.findItem(updatePolicy.getId()).setChecked(true);

        return true;
    }

    private void updateStationList() {
        try {
            final boolean sortByDistance = baseApplication.getSortByDistance() && myPos != null;
            final int stationCount = dbAdapter.countStations(baseApplication.getCountryCodes());
            final Cursor cursor = dbAdapter.getStationsListByKeyword(searchString, baseApplication.getStationFilter(), baseApplication.getCountryCodes(), sortByDistance, myPos);
            if (stationListAdapter != null) {
                stationListAdapter.swapCursor(cursor);
            } else {
                stationListAdapter = new StationListAdapter(this, cursor, 0);
                binding.appBarMain.main.lstStations.setAdapter(stationListAdapter);

                binding.appBarMain.main.lstStations.setOnItemClickListener((listview, view, position, id) -> {
                    final Intent intentDetails = new Intent(MainActivity.this, DetailsActivity.class);
                    intentDetails.putExtra(DetailsActivity.EXTRA_STATION, dbAdapter.fetchStationByRowId(id));
                    startActivityForResult(intentDetails, 0);
                });
            }
            binding.appBarMain.main.filterResult.setText(getString(R.string.filter_result, stationListAdapter.getCount(), stationCount));
        } catch (final Exception e) {
            Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        // necessary for the update policy submenu
        item.setChecked(!item.isChecked());

        //noinspection SimplifiableIfStatement
        if (id == R.id.rb_update_manual) {
            baseApplication.setUpdatePolicy(UpdatePolicy.MANUAL);
        } else if (id == R.id.rb_update_automatic) {
            baseApplication.setUpdatePolicy(UpdatePolicy.AUTOMATIC);
        } else if (id == R.id.rb_update_notify) {
            baseApplication.setUpdatePolicy(UpdatePolicy.NOTIFY);
        } else if (id == R.id.apiUrl) {
            new SimpleDialogs().prompt(this, R.string.apiUrl, EditorInfo.TYPE_TEXT_VARIATION_URI, R.string.api_url_hint, baseApplication.getApiUrl(), v -> {
                try {
                    if (StringUtils.isEmpty(v)) {
                        baseApplication.setApiUrl(null); // set to default
                        baseApplication.setLastUpdate(0);
                        recreate();
                    } else {
                        if (!Uri.parse(v).getScheme().matches("https?")) {
                            throw new IllegalArgumentException("Only http(s) URIs are allowed");
                        }
                        baseApplication.setApiUrl(v);
                        baseApplication.setLastUpdate(0);
                        recreate();
                    }
                } catch (final Exception e) {
                    Toast.makeText(getBaseContext(), getString(R.string.invalid_api_url), Toast.LENGTH_LONG).show();
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }

    private void setNotificationIcon(final boolean active) {
        final MenuItem item = binding.navView.getMenu().findItem(R.id.nav_notification);
        item.setIcon(ContextCompat.getDrawable(this, active ? R.drawable.ic_notifications_active_gray_24px : R.drawable.ic_notifications_off_gray_24px));
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        if (id == R.id.nav_slideshow) {
            startActivity(new Intent(this, IntroSliderActivity.class));
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
            final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.fab_email)));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
        } else if (id == R.id.nav_app_info) {
            final AppInfoFragment appInfoFragment = new AppInfoFragment();
            appInfoFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void runUpdateCountriesAndStations() {
        binding.appBarMain.main.progressBar.setVisibility(View.VISIBLE);

        RSAPI.Helper.runUpdateCountriesAndStations(this, baseApplication, success -> {
            if (success) {
                final TextView tvUpdate = findViewById(R.id.tvUpdate);
                tvUpdate.setText(getString(R.string.last_update_at) + SimpleDateFormat.getDateTimeInstance().format(baseApplication.getLastUpdate()));
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
                for (final String country : baseApplication.getCountryCodes()) {
                    rsapi.getStatistic(country).enqueue(new Callback<Statistic>() {
                        @Override
                        public void onResponse(final Call<Statistic> call, final Response<Statistic> response) {
                            if (response.isSuccessful()) {
                                checkForUpdates(response.body(), country);
                            }
                        }

                        @Override
                        public void onFailure(final Call<Statistic> call, final Throwable t) {
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

    private void checkForUpdates(final Statistic apiStat, final String country) {
        if (apiStat == null) {
            return;
        }

        final Statistic dbStat = dbAdapter.getStatistic(country);
        Log.d(TAG, "DbStat: " + dbStat);
        if (apiStat.getTotal() != dbStat.getTotal() || apiStat.getWithPhoto() != dbStat.getWithPhoto() || apiStat.getWithoutPhoto() != dbStat.getWithoutPhoto()) {
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
        final Intent intent = new Intent(this, NearbyNotificationService.class);
        intent.setAction(NearbyNotificationService.STATUS_INTERFACE);
        if (!this.getApplicationContext().bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                Log.d(TAG, "Bound to status service of NearbyNotificationService");
                statusBinder = (NearbyNotificationService.StatusBinder) service;
                invalidateOptionsMenu();
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
                Log.d(TAG, "Unbound from status service of NearbyNotificationService");
                statusBinder = null;
                invalidateOptionsMenu();
            }
        }, 0))

        Log.e(TAG, "Bind request to statistics interface failed");
    }

    public void toggleNotification() {
        final Intent intent = new Intent(MainActivity.this, NearbyNotificationService.class);
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
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
                return;
            }

            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            // getting GPS status
            final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

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
                final boolean isNetworkEnabled = locationManager
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
        } catch (final Exception e) {
            Log.e(TAG, "Error registering LocationManager", e);
            final Bundle b = new Bundle();
            b.putString("error", "Error registering LocationManager: " + e.toString());
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // if we don't have location permission we cannot remove updates (should not happen, but the API requires this check
                // so we just set it to null
                locationManager = null;
            } else {
                locationManager.removeUpdates(this);
            }
            locationManager = null;
        }
        Log.i(TAG, "LocationManager unregistered");
    }

    @Override
    public void onLocationChanged(@NonNull final Location location) {
        myPos = location;
        updateStationList();
    }

    @Override
    public void stationFilterChanged(final StationFilter stationFilter) {
        baseApplication.setStationFilter(stationFilter);
        updateStationList();
    }

    @Override
    public void sortOrderChanged(final boolean sortByDistance) {
        if (sortByDistance) {
            registerLocationManager();
        }
        updateStationList();
    }

}
