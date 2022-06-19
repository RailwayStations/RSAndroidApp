package de.bahnhoefe.deutschlands.bahnhofsfotos;

import static android.view.Menu.NONE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.MapZoomControls;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMapsBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.MapInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.StationFilterBar;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.ClusterManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.DbsTileSource;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.GeoItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MarkerBitmap;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.TapHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;

public class MapsActivity extends AppCompatActivity implements LocationListener, TapHandler<MapsActivity.BahnhofGeoItem>, StationFilterBar.OnChangeListener {

    public static final String EXTRAS_LATITUDE = "Extras_Latitude";
    public static final String EXTRAS_LONGITUDE = "Extras_Longitude";
    public static final String EXTRAS_MARKER = "Extras_Marker";

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 500; // minute

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int REQUEST_FINE_LOCATION = 1;
    private static final String USER_AGENT = "railway-stations.org-android";

    private final Map<String, OnlineTileSource> onlineTileSources = new HashMap<>();
    protected Layer layer;
    protected ClusterManager<BahnhofGeoItem> clusterer = null;
    protected final List<TileCache> tileCaches = new ArrayList<>();
    private LatLong myPos = null;
    private CheckBox myLocSwitch = null;
    private DbAdapter dbAdapter;
    private String nickname;
    private BaseApplication baseApplication;
    private LocationManager locationManager;
    private boolean askedForPermission = false;
    private Marker missingMarker;

    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        var window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#c71c4d"));

        setSupportActionBar(binding.mapsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        nickname = baseApplication.getNickname();

        var intent = getIntent();
        Marker extraMarker = null;
        if (intent != null) {
            var latitude = (Double) intent.getSerializableExtra(EXTRAS_LATITUDE);
            var longitude = (Double) intent.getSerializableExtra(EXTRAS_LONGITUDE);
            setMyLocSwitch(false);
            if (latitude != null && longitude != null) {
                myPos = new LatLong(latitude, longitude);
            }

            var markerRes = (Integer) intent.getSerializableExtra(EXTRAS_MARKER);
            if (markerRes != null) {
                extraMarker = createBitmapMarker(myPos, markerRes);
            }
        }

        addDBSTileSource(R.string.dbs_osm_basic, "/styles/dbs-osm-basic/");
        addDBSTileSource(R.string.dbs_osm_railway, "/styles/dbs-osm-railway/");

        createMapViews();
        createTileCaches();
        checkPermissionsAndCreateLayersAndControls();

        if (extraMarker != null) {
            binding.map.mapView.getLayerManager().getLayers().add(extraMarker);
        }
    }

    private void addDBSTileSource(int nameResId, String baseUrl) {
        var dbsBasic = new DbsTileSource(getString(nameResId), baseUrl);
        onlineTileSources.put(dbsBasic.getName(), dbsBasic);
    }

    protected void createTileCaches() {
        this.tileCaches.add(AndroidUtil.createTileCache(this, getPersistableId(),
                this.binding.map.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                this.binding.map.mapView.getModel().frameBufferModel.getOverdrawFactor(), true));
    }

    /**
     * The persistable ID is used to store settings information, like the center of the last view
     * and the zoomlevel. By default the simple name of the class is used. The value is not user
     * visibile.
     *
     * @return the id that is used to save this mapview.
     */
    protected String getPersistableId() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the relative size of a map view in relation to the screen size of the device. This
     * is used for cache size calculations.
     * By default this returns 1.0, for a full size map view.
     *
     * @return the screen ratio of the mapview
     */
    protected float getScreenRatio() {
        return 1.0f;
    }

    /**
     * Hook to check for Android Runtime Permissions.
     */
    protected void checkPermissionsAndCreateLayersAndControls() {
        createLayers();
        createControls();
    }

    /**
     * Hook to create controls, such as scale bars.
     * You can add more controls.
     */
    protected void createControls() {
        initializePosition(binding.map.mapView.getModel().mapViewPosition);
    }

    /**
     * initializes the map view position.
     *
     * @param mvp the map view position to be set
     */
    protected void initializePosition(IMapViewPosition mvp) {
        if (myPos != null) {
            mvp.setMapPosition(new MapPosition(myPos, baseApplication.getZoomLevelDefault()));
        } else {
            mvp.setMapPosition(baseApplication.getLastMapPosition());
        }
        mvp.setZoomLevelMax(getZoomLevelMax());
        mvp.setZoomLevelMin(getZoomLevelMin());
    }

    /**
     * Template method to create the map views.
     */
    protected void createMapViews() {
        binding.map.mapView.setClickable(true);
        binding.map.mapView.setOnMapDragListener(() -> myLocSwitch.setChecked(false));
        binding.map.mapView.getMapScaleBar().setVisible(true);
        binding.map.mapView.setBuiltInZoomControls(true);
        binding.map.mapView.getMapZoomControls().setAutoHide(true);
        binding.map.mapView.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
        binding.map.mapView.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());

        binding.map.mapView.getMapZoomControls().setZoomControlsOrientation(MapZoomControls.Orientation.VERTICAL_IN_OUT);
        binding.map.mapView.getMapZoomControls().setZoomInResource(R.drawable.zoom_control_in);
        binding.map.mapView.getMapZoomControls().setZoomOutResource(R.drawable.zoom_control_out);
        binding.map.mapView.getMapZoomControls().setMarginHorizontal(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
        binding.map.mapView.getMapZoomControls().setMarginVertical(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
    }

    protected byte getZoomLevelMax() {
        return binding.map.mapView.getModel().mapViewPosition.getZoomLevelMax();
    }

    protected byte getZoomLevelMin() {
        return binding.map.mapView.getModel().mapViewPosition.getZoomLevelMin();
    }

    /**
     * Hook to purge tile caches.
     * By default we purge every tile cache that has been added to the tileCaches list.
     */
    protected void purgeTileCaches() {
        for (TileCache tileCache : tileCaches) {
            tileCache.purge();
        }
        tileCaches.clear();
    }

    protected XmlRenderTheme getRenderTheme() {
        var mapTheme = baseApplication.getMapThemeUri();
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            var renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
            return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(renderThemeFile.getUri()));
        } catch (Exception e) {
            Log.e( TAG,"Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile(Uri mapUri) {
        if (mapUri == null || !DocumentFile.isDocumentUri(this, mapUri)) {
            return null;
        }
        try {
            var inputStream = (FileInputStream) getContentResolver().openInputStream(mapUri);
            return new MapFile(inputStream, 0, null);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Can't open mapFile", e);
        }
        return null;
    }

    protected void createLayers() {
        var map = baseApplication.getMap();
        var mapUri = baseApplication.toUri(map);
        var mapFile = getMapFile(mapUri);

        if (mapFile != null) {
            var rendererLayer = new TileRendererLayer(this.tileCaches.get(0), mapFile,
                    this.binding.map.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE) {
                @Override
                public boolean onLongPress(LatLong tapLatLong, Point thisXY,
                                           Point tapXY) {
                    MapsActivity.this.onLongPress(tapLatLong);
                    return true;
                }
            };
            rendererLayer.setXmlRenderTheme(getRenderTheme());
            this.layer = rendererLayer;
            binding.map.mapView.getLayerManager().getLayers().add(this.layer);
        } else {
            AbstractTileSource tileSource = onlineTileSources.get(map);

            if (tileSource == null) {
                tileSource = OpenStreetMapMapnik.INSTANCE;
            }

            tileSource.setUserAgent(USER_AGENT);
            this.layer = new TileDownloadLayer(this.tileCaches.get(0),
                    this.binding.map.mapView.getModel().mapViewPosition, tileSource,
                    AndroidGraphicFactory.INSTANCE) {
                @Override
                public boolean onLongPress(LatLong tapLatLong, Point thisXY,
                                           Point tapXY) {
                    MapsActivity.this.onLongPress(tapLatLong);
                    return true;
                }
            };
            binding.map.mapView.getLayerManager().getLayers().add(this.layer);

            binding.map.mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            binding.map.mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
        }

    }

    private Marker createBitmapMarker(LatLong latLong, int markerRes) {
        var drawable = ContextCompat.getDrawable(this, markerRes);
        assert drawable != null;
        var bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        return new Marker(latLong, bitmap, -(bitmap.getWidth() / 2), -bitmap.getHeight());
    }

    private void onLongPress(LatLong tapLatLong) {
        if (missingMarker == null) {
            // marker to show at the location
            var drawable = ContextCompat.getDrawable(this, R.drawable.marker_missing);
            assert drawable != null;
            var bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
            missingMarker = new Marker(tapLatLong, bitmap, -(bitmap.getWidth()/2), -bitmap.getHeight()) {
                @Override
                public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                    SimpleDialogs.confirm(MapsActivity.this, R.string.add_missing_station, (dialogInterface, i) -> {
                        var intent = new Intent(MapsActivity.this, DetailsActivity.class);
                        intent.putExtra(DetailsActivity.EXTRA_LATITUDE, getLatLong().latitude);
                        intent.putExtra(DetailsActivity.EXTRA_LONGITUDE, getLatLong().longitude);
                        startActivity(intent);
                    });
                    return false;
                }
            };
            binding.map.mapView.getLayerManager().getLayers().add(missingMarker);
        } else {
            missingMarker.setLatLong(tapLatLong);
            missingMarker.requestRedraw();
        }

        // feedback for long click
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        var item = menu.findItem(R.id.menu_toggle_mypos);
        myLocSwitch = new CheckBox(this);
        myLocSwitch.setButtonDrawable(R.drawable.ic_gps_fix_selector);
        myLocSwitch.setChecked(baseApplication.isLocationUpdates());
        item.setActionView(myLocSwitch);
        myLocSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            baseApplication.setLocationUpdates(isChecked);
            if (isChecked) {
                askedForPermission = false;
                registerLocationManager();
            } else {
                unregisterLocationManager();
            }
        }
        );

        var map = baseApplication.getMap();

        var osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(map == null);
        osmMapnick.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, null));

        var mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();
        for (var tileSource : onlineTileSources.values()) {
            var mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, tileSource.getName());
            mapItem.setChecked(tileSource.getName().equals(map));
            mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, tileSource.getName()));
        }

        var mapDirectory = baseApplication.getMapDirectoryUri();
        if (mapDirectory != null) {
            var documentsTree = getDocumentFileFromTreeUri(mapDirectory);
            if (documentsTree != null) {
                for (var file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".map")) {
                        var mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, file.getName());
                        mapItem.setChecked(file.getUri().equals(baseApplication.toUri(map)));
                        mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, file.getUri().toString()));
                    }
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true);

        var mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(item1 -> {
            openMapDirectoryChooser();
            return false;
        });

        var mapTheme = baseApplication.getMapThemeUri();
        var mapThemeDirectory = baseApplication.getMapThemeDirectoryUri();

        var defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, null));
        var themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null) {
            var documentsTree = getDocumentFileFromTreeUri(mapThemeDirectory);
            if (documentsTree != null) {
                for (var file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        var themeName = file.getName();
                        var themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                        themeItem.setChecked(file.getUri().equals(mapTheme));
                        themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, file.getUri()));
                    } else if (file.isDirectory()) {
                        var childFile = file.findFile(file.getName() + ".xml");
                        if (childFile != null) {
                            var themeName = file.getName();
                            var themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                            themeItem.setChecked(childFile.getUri().equals(mapTheme));
                            themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, childFile.getUri()));
                        }
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        var themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(item12 -> {
            openThemeDirectoryChooser();
            return false;
        });

        return true;
    }

    private DocumentFile getDocumentFileFromTreeUri(Uri uri) {
        try {
            return DocumentFile.fromTreeUri(getApplication(), uri);
        } catch (Exception e) {
            Log.w(TAG, "Error getting DocumentFile from Uri: " + uri);
        }
        return null;
    }

    protected ActivityResultLauncher<Intent> themeDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    var uri = result.getData().getData();
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        baseApplication.setMapThemeDirectoryUri(uri);
                        recreate();
                    }
                }
            });

    protected ActivityResultLauncher<Intent> mapDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    var uri = result.getData().getData();
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        baseApplication.setMapDirectoryUri(uri);
                        recreate();
                    }
                }
            });

    public void openDirectory(ActivityResultLauncher<Intent> launcher) {
        var intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        launcher.launch(intent);
    }


    private void openMapDirectoryChooser() {
        openDirectory(mapDirectoryLauncher);
    }

    private void openThemeDirectoryChooser() {
        openDirectory(themeDirectoryLauncher);
    }

    /**
     * Android Activity life cycle method.
     */
    @Override
    protected void onDestroy() {
        binding.map.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        purgeTileCaches();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map_info) {
            new MapInfoFragment().show(getSupportFragmentManager(), "Map Info Dialog");
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void reloadMap() {
        destroyClusterManager();
        new LoadMapMarkerTask(this).start();
    }

    private void runUpdateCountriesAndStations() {
        binding.map.progressBar.setVisibility(View.VISIBLE);
        baseApplication.getRsapiClient().runUpdateCountriesAndStations(this, baseApplication, success -> reloadMap());
    }


    private void onStationsLoaded(List<Station> stationList, List<Upload> uploadList) {
        try {
            createClusterManager();
            addMarkers(stationList, uploadList);
            binding.map.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getResources().getQuantityString(R.plurals.stations_loaded, stationList.size(), stationList.size()), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error loading markers", e);
        }
    }

    @Override
    public void onTap(BahnhofGeoItem marker) {
        var intent = new Intent(MapsActivity.this, DetailsActivity.class);
        var id = marker.getStation().getId();
        var country = marker.getStation().getCountry();
        try {
            var station = dbAdapter.getStationByKey(country, id);
            intent.putExtra(DetailsActivity.EXTRA_STATION, station);
            startActivity(intent);
        } catch (RuntimeException e) {
            Log.wtf(TAG, String.format("Could not fetch station id %s that we put onto the map", id), e);
        }
    }

    public void setMyLocSwitch(boolean checked) {
        if (myLocSwitch != null) {
            myLocSwitch.setChecked(checked);
        }
        baseApplication.setLocationUpdates(checked);
    }

    @Override
    public void stationFilterChanged(StationFilter stationFilter) {
        reloadMap();
    }

    @Override
    public void sortOrderChanged(boolean sortByDistance) {
        // unused
    }

    private static class LoadMapMarkerTask extends Thread {
        private WeakReference<MapsActivity> activityRef;

        public LoadMapMarkerTask(MapsActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            var stationList = activityRef.get().readStations();
            var uploadList = activityRef.get().readPendingUploads();
            var mapsActivity = activityRef.get();
            if (mapsActivity != null) {
                mapsActivity.runOnUiThread(()-> mapsActivity.onStationsLoaded(stationList, uploadList));
            }
        }

    }

    private List<Station> readStations() {
        try {
            return dbAdapter.getAllStations(baseApplication.getStationFilter(), baseApplication.getCountryCodes());
        } catch (Exception e) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden");
        }
        return null;
    }

    private List<Upload> readPendingUploads() {
        try {
            return dbAdapter.getPendingUploads(false);
        } catch (Exception e) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden");
        }
        return null;
    }

    private List<MarkerBitmap> createMarkerBitmaps() {
        var markerBitmaps = new ArrayList<MarkerBitmap>();
        markerBitmaps.add(createSmallSingleIconMarker());
        markerBitmaps.add(createSmallClusterIconMarker());
        markerBitmaps.add(createLargeClusterIconMarker());
        return markerBitmaps;
    }

    /**
     * large cluster icon. 100 will be ignored.
     */
    private MarkerBitmap createLargeClusterIconMarker() {
        var bitmapBalloonMN = loadBitmap(R.drawable.balloon_m_n);
        var paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStyle(Style.FILL);
        paint.setTextAlign(Align.CENTER);
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        paint.setColor(Color.BLACK);
        return new MarkerBitmap(this.getApplicationContext(), bitmapBalloonMN,
                new Point(0, 0), 11f, 100, paint);
    }

    /**
     * small cluster icon. for 10 or less items.
     */
    private MarkerBitmap createSmallClusterIconMarker() {
        var bitmapBalloonSN = loadBitmap(R.drawable.balloon_s_n);
        var paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStyle(Style.FILL);
        paint.setTextAlign(Align.CENTER);
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        paint.setColor(Color.BLACK);
        return new MarkerBitmap(this.getApplicationContext(), bitmapBalloonSN,
                new Point(0, 0), 9f, 10, paint);
    }

    private MarkerBitmap createSmallSingleIconMarker() {
        var bitmapWithPhoto = loadBitmap(R.drawable.marker_green);
        var markerWithoutPhoto = loadBitmap(R.drawable.marker_red);
        var markerOwnPhoto = loadBitmap(R.drawable.marker_violet);
        var markerPendingUpload = loadBitmap(R.drawable.marker_yellow);

        var markerWithPhotoInactive = loadBitmap(R.drawable.marker_green_inactive);
        var markerWithoutPhotoInactive = loadBitmap(R.drawable.marker_red_inactive);
        var markerOwnPhotoInactive = loadBitmap(R.drawable.marker_violet_inactive);

        var paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStyle(Style.FILL);
        paint.setTextAlign(Align.CENTER);
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        paint.setColor(Color.RED);

        return new MarkerBitmap(this.getApplicationContext(), markerWithoutPhoto, bitmapWithPhoto, markerOwnPhoto,
                markerWithoutPhotoInactive, markerWithPhotoInactive, markerOwnPhotoInactive, markerPendingUpload,
                new Point(0, -(markerWithoutPhoto.getHeight()/2.0)), 10f, 1, paint);
    }

    private Bitmap loadBitmap(int resourceId){
        var bitmap = AndroidGraphicFactory.convertToBitmap(ResourcesCompat.getDrawable(getResources(), resourceId, null));
        bitmap.incrementRefCount();
        return bitmap;
    }

    private void addMarkers(List<Station> stationMarker, List<Upload> uploadList) {
        double minLat = 0;
        double maxLat = 0;
        double minLon = 0;
        double maxLon = 0;
        for (var station : stationMarker) {
            var isPendingUpload = isPendingUpload(station, uploadList);
            var geoItem = new BahnhofGeoItem(station, isPendingUpload);
            var bahnhofPos = geoItem.getLatLong();
            if (minLat == 0.0) {
                minLat = bahnhofPos.latitude;
                maxLat = bahnhofPos.latitude;
                minLon = bahnhofPos.longitude;
                maxLon = bahnhofPos.longitude;
            } else {
                minLat = Math.min(minLat, bahnhofPos.latitude);
                maxLat = Math.max(maxLat, bahnhofPos.latitude);
                minLon = Math.min(minLon, bahnhofPos.longitude);
                maxLon = Math.max(maxLon, bahnhofPos.longitude);
            }
            clusterer.addItem(geoItem);
        }

        clusterer.redraw();

        if (myPos == null || (myPos.latitude == 0.0 && myPos.longitude == 0.0)) {
            myPos = new LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2);
        }
        updatePosition();
    }

    private boolean isPendingUpload(Station station, List<Upload> uploadList) {
        for (var upload : uploadList) {
            if (upload.isPendingPhotoUpload() && station.getId().equals(upload.getStationId()) && station.getCountry().equals(upload.getCountry())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.layer instanceof TileDownloadLayer) {
            ((TileDownloadLayer)this.layer).onResume();
        }
        if (baseApplication.getLastUpdate() == 0) {
            runUpdateCountriesAndStations();
        } else {
            reloadMap();
        }
        if (baseApplication.isLocationUpdates()) {
            registerLocationManager();
        }
        binding.map.stationFilterBar.setBaseApplication(baseApplication);
        binding.map.stationFilterBar.setOnChangeListener(this);
        binding.map.stationFilterBar.setSortOrderEnabled(false);
    }

    private void createClusterManager() {
        // create clusterer instance
        clusterer = new ClusterManager<>(
                binding.map.mapView,
                createMarkerBitmaps(),
                (byte)9,
                this);
        // this uses the framebuffer position, the mapview position can be out of sync with
        // what the user sees on the screen if an animation is in progress
        this.binding.map.mapView.getModel().frameBufferModel.addObserver(clusterer);
    }

    @Override
    protected void onPause() {
        if (this.layer instanceof TileDownloadLayer) {
            ((TileDownloadLayer)this.layer).onPause();
        }
        unregisterLocationManager();
        var mapPosition = binding.map.mapView.getModel().mapViewPosition.getMapPosition();
        baseApplication.setLastMapPosition(mapPosition);
        destroyClusterManager();
        super.onPause();
    }

    private void destroyClusterManager() {
        if (clusterer != null) {
            clusterer.destroyGeoClusterer();
            this.binding.map.mapView.getModel().frameBufferModel.removeObserver(clusterer);
            clusterer = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        myPos = new LatLong(location.getLatitude(), location.getLongitude());
        updatePosition();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

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
                Toast.makeText(MapsActivity.this, R.string.grant_location_permission, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void registerLocationManager() {

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!askedForPermission) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
                    askedForPermission = true;
                }
                setMyLocSwitch(false);
                return;
            }

            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            // getting GPS status
            var isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d(TAG, "GPS Enabled");
                if (locationManager != null) {
                    var loc = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    myPos = new LatLong(loc.getLatitude(), loc.getLongitude());
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
                        var loc = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        myPos = new LatLong(loc.getLatitude(), loc.getLongitude());
                    }
                }
            }
            setMyLocSwitch(true);
        } catch (Exception e) {
            Log.e(TAG, "Error registering LocationManager", e);
            var b = new Bundle();
            b.putString("error", "Error registering LocationManager: " + e);
            locationManager = null;
            myPos = null;
            setMyLocSwitch(false);
            return;
        }
        Log.i(TAG, "LocationManager registered");
        updatePosition();
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

    private void updatePosition() {
        if (myLocSwitch != null && myLocSwitch.isChecked()) {
            binding.map.mapView.setCenter(myPos);
            binding.map.mapView.repaint();
        }
    }

    protected class BahnhofGeoItem implements GeoItem {
        public Station station;
        public final LatLong latLong;
        public final boolean pendingUpload;

        public BahnhofGeoItem(Station station, boolean pendingUpload) {
            this.station = station;
            this.pendingUpload = pendingUpload;
            this.latLong = new LatLong(station.getLat(), station.getLon());
        }

        public LatLong getLatLong() {
            return latLong;
        }

        @Override
        public String getTitle() {
            return station.getTitle();
        }

        @Override
        public boolean hasPhoto() {
            return station.hasPhoto();
        }

        @Override
        public boolean ownPhoto() {
            return hasPhoto() && station.getPhotographer().equals(nickname);
        }

        @Override
        public boolean stationActive() {
            return station.isActive();
        }

        @Override
        public boolean isPendingUpload() {
            return pendingUpload;
        }

        public Station getStation() {
            return station;
        }

    }

    private static class MapMenuListener implements MenuItem.OnMenuItemClickListener {

        private final WeakReference<MapsActivity> mapsActivityRef;

        private final BaseApplication baseApplication;

        private final String map;

        private MapMenuListener(MapsActivity mapsActivity, BaseApplication baseApplication, String map) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.map = map;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMap(null);
            } else {
                baseApplication.setMap(map);
            }

            var mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

    private static class MapThemeMenuListener implements MenuItem.OnMenuItemClickListener {

        private final WeakReference<MapsActivity> mapsActivityRef;

        private final BaseApplication baseApplication;

        private final Uri mapThemeUri;

        private MapThemeMenuListener(MapsActivity mapsActivity, BaseApplication baseApplication, Uri mapThemeUri) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.mapThemeUri = mapThemeUri;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) { // default theme
                baseApplication.setMapThemeUri(null);
            } else {
                baseApplication.setMapThemeUri(mapThemeUri);
            }

            var mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

}

