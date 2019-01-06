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
import android.database.Cursor;
import android.net.Uri;
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

import java.io.File;
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
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private String searchString;

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

        handleGalleryNavItem();

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

        cursor = dbAdapter.getStationsList(baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
        customAdapter = new CustomAdapter(this, cursor, 0);
        ListView listView = findViewById(R.id.lstStations);
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

        Intent searchIntent = getIntent();
        if (Intent.ACTION_SEARCH.equals(searchIntent.getAction())) {
            String query = searchIntent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(MainActivity.this, query, Toast.LENGTH_SHORT).show();
        }

        bindToStatus();
    }


    private void handleGalleryNavItem() {
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + Constants.PHOTO_DIRECTORY);
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

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit: " + s);
                searchKeyword(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "onQueryTextChange: " + s);
                searchKeyword(s);
                return false;
            }

        });

        photoFilterMenuItem = menu.findItem(R.id.menu_toggle_photo);
        photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());

        initNotificationMenuItem(menu.findItem(R.id.notify), false);

        return true;
    }

    private void searchKeyword(String keyword) {
        searchString = keyword;
        try {
            cursor = dbAdapter.getBahnhofsListByKeyword(keyword, baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
            if (cursor != null) {
                customAdapter.swapCursor(cursor);
            }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.countrySelection) {
            final Intent intent = new Intent(MainActivity.this, CountryActivity.class);
            startActivity(intent);
            item.setIcon(R.drawable.ic_language_white_24px);
        } else if (id == R.id.menu_toggle_photo) {
            PhotoFilter photoFilter = baseApplication.getPhotoFilter().getNextFilter();
            item.setIcon(photoFilter.getIcon());
            baseApplication.setPhotoFilter(photoFilter);
            cursor = dbAdapter.getStationsList(photoFilter, baseApplication.getNicknameFilter());
            customAdapter.swapCursor(cursor);
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
                        cursor = dbAdapter.getStationsList(photoFilter, baseApplication.getNicknameFilter());
                        customAdapter.swapCursor(cursor);
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
        final ProgressDialog progress = new ProgressDialog(MainActivity.this);
        progress.setMessage(getResources().getString(R.string.loading_stations));
        progress.setTitle(getResources().getString(R.string.app_name));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();

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

        rsapi.getStations(baseApplication.getCountryCode().toLowerCase()).enqueue(new Callback<List<Bahnhof>>() {
            @Override
            public void onResponse(Call<List<Bahnhof>> call, Response<List<Bahnhof>> response) {
                if (response.isSuccessful()) {
                    dbAdapter.insertBahnhoefe(response.body());

                    baseApplication.setLastUpdate(System.currentTimeMillis());
                    TextView tvUpdate = findViewById(R.id.tvUpdate);
                    tvUpdate.setText(getString(R.string.last_update_at) + SimpleDateFormat.getDateTimeInstance().format(baseApplication.getLastUpdate()));
                    customAdapter.swapCursor(dbAdapter.getStationsList(baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter()));
                }
                progress.dismiss();
            }

            @Override
            public void onFailure(Call<List<Bahnhof>> call, Throwable t) {
                Log.e(TAG, "Error refreshing stations", t);
                progress.dismiss();
                Toast.makeText(getBaseContext(), getString(R.string.station_update_failed) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

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
                rsapi.getStatistic(baseApplication.getCountryCode().toLowerCase()).enqueue(new Callback<Statistic>() {
                    @Override
                    public void onResponse(Call<Statistic> call, Response<Statistic> response) {
                        if (response.isSuccessful()) {
                            checkForUpdates(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<Statistic> call, Throwable t) {
                        Log.e(TAG, "Error loading country statistic", t);
                    }
                });
            }
        }

        if (photoFilterMenuItem != null) {
            photoFilterMenuItem.setIcon(baseApplication.getPhotoFilter().getIcon());
        }
        if (searchString != null && searchString.length() > 0) {
            searchKeyword(searchString);
        } else if (customAdapter != null) {
            cursor = dbAdapter.getStationsList(baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
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
