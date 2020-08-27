package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
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

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.AppInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMainBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.StationListAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Url;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DIALOG_TAG = "App Info Dialog";
    private static final long CHECK_UPDATE_INTERVAL = 10 * 60 * 1000; // 10 minutes
    private static final String TAG = MainActivity.class.getSimpleName();

    private BaseApplication baseApplication;
    private DbAdapter dbAdapter;
    private MenuItem photoFilterMenuItem;

    private ActivityMainBinding binding;

    private StationListAdapter stationListAdapter;
    private String searchString;

    private NearbyNotificationService.StatusBinder statusBinder;
    private RSAPI rsapi;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        rsapi = baseApplication.getRSAPI();

        binding.appBarMain.fab.setOnClickListener(view -> {
            final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.fab_email)));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
        });

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

        updateStationList();
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

        photoFilterMenuItem = menu.findItem(R.id.menu_toggle_photo);
        photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());

        initNotificationMenuItem(menu.findItem(R.id.notify), false);

        final UpdatePolicy updatePolicy = baseApplication.getUpdatePolicy();
        menu.findItem(updatePolicy.getId()).setChecked(true);

        return true;
    }

    private void updateStationList() {
        try {
            final int stationCount = dbAdapter.countBahnhoefe(baseApplication.getCountryCodes());
            Cursor cursor = dbAdapter.getStationsListByKeyword(searchString, baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter(), baseApplication.getCountryCodes());
            if (stationListAdapter != null) {
                stationListAdapter.swapCursor(cursor);
            } else {
                cursor = dbAdapter.getStationsList(baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter(), baseApplication.getCountryCodes());
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
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final MenuItem item = menu.findItem(R.id.notify);
        initNotificationMenuItem(item, statusBinder != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

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
            final PhotoFilter photoFilter = baseApplication.getPhotoFilter().getNextFilter();
            item.setIcon(photoFilter.getIcon());
            baseApplication.setPhotoFilter(photoFilter);
            updateStationList();
        } else if (id == R.id.nicknameFilter) {
            int selectedNickname = -1;
            final String[] nicknames = dbAdapter.getPhotographerNicknames();
            final String selectedNick = baseApplication.getNicknameFilter();
            if (nicknames.length == 0) {
                Toast.makeText(getBaseContext(), getString(R.string.no_nicknames_found), Toast.LENGTH_LONG).show();
                return true;
            }
            for (int i = 0; i < nicknames.length; i++) {
                if (nicknames[i].equals(selectedNick)) {
                    selectedNickname = i;
                }
            }
            new SimpleDialogs().select(this, getString(R.string.select_nickname), nicknames, selectedNickname, (dialog, whichButton) -> {
                dialog.dismiss();
                final int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                if (selectedPosition >= 0 && nicknames.length > selectedPosition) {
                    baseApplication.setNicknameFilter(nicknames[selectedPosition]);
                    final PhotoFilter photoFilter = PhotoFilter.NICKNAME;
                    baseApplication.setPhotoFilter(photoFilter);
                    item.setIcon(photoFilter.getIcon());
                    updateStationList();
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
        } else if (id == R.id.apiUrl) {
            new SimpleDialogs().prompt(this, R.string.apiUrl, EditorInfo.TYPE_TEXT_VARIATION_URI, R.string.api_url_hint, baseApplication.getApiUrl(), v -> {
                try {
                    if (!Uri.parse(v).getScheme().matches("https\\?")) {
                        throw new IllegalArgumentException("Only http(s) URIs are allowed");
                    }
                } catch (final Exception e) {
                    Toast.makeText(getBaseContext(), getString(R.string.invalid_api_url), Toast.LENGTH_LONG).show();
                }
                baseApplication.setApiUrl(v);
                baseApplication.setLastUpdate(0);
                recreate();
            });
        }

        return super.onOptionsItemSelected(item);
    }

    private void initNotificationMenuItem(final MenuItem item, final boolean active) {
        item.setChecked(active);
        item.setIcon(active ? R.drawable.ic_notifications_active_white_24px : R.drawable.ic_notifications_off_white_24px);
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
        } else if (id == R.id.nav_app_info) {
            final AppInfoFragment appInfoFragment = new AppInfoFragment();
            appInfoFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void runUpdateCountriesAndStations() {
        binding.appBarMain.main.progressBar.setVisibility(View.VISIBLE);

        rsapi.getCountries().enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(final Call<List<Country>> call, final Response<List<Country>> response) {
                if (response.isSuccessful()) {
                    dbAdapter.insertCountries(response.body());
                }
            }

            @Override
            public void onFailure(final Call<List<Country>> call, final Throwable t) {
                Log.e(TAG, "Error refreshing countries", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_updating_countries) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        rsapi.getStations(baseApplication.getCountryCodes().toArray(new String[0])).enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(final Call<List<Station>> call, final Response<List<Station>> response) {
                if (response.isSuccessful()) {
                    dbAdapter.insertBahnhoefe(response.body(), baseApplication.getCountryCodes());

                    baseApplication.setLastUpdate(System.currentTimeMillis());
                    final TextView tvUpdate = findViewById(R.id.tvUpdate);
                    tvUpdate.setText(getString(R.string.last_update_at) + SimpleDateFormat.getDateTimeInstance().format(baseApplication.getLastUpdate()));
                    updateStationList();
                }
                binding.appBarMain.main.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(final Call<List<Station>> call, final Throwable t) {
                Log.e(TAG, "Error refreshing stations", t);
                binding.appBarMain.main.progressBar.setVisibility(View.GONE);
                Toast.makeText(getBaseContext(), getString(R.string.station_update_failed) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

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

        if (photoFilterMenuItem != null) {
            photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());
        }
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

}
