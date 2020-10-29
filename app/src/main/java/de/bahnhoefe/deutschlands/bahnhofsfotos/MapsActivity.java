package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
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

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.MapInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMapsBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.ClusterManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.DbsTileSource;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.GeoItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MapsforgeMapView;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MarkerBitmap;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.TapHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;

import static android.view.Menu.NONE;

public class MapsActivity extends AppCompatActivity implements LocationListener, TapHandler<MapsActivity.BahnhofGeoItem> {

    public static final String EXTRAS_LATITUDE = "Extras_Latitude";
    public static final String EXTRAS_LONGITUDE = "Extras_Longitude";

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 500; // minute

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int REQUEST_FINE_LOCATION = 1;
    private static final int REQUEST_MAP_DIRECTORY = 2;
    private static final int REQUEST_THEME_DIRECTORY = 3;
    private static final String USER_AGENT = "railway-stations.org-android";

    private final Map<String, OnlineTileSource> onlineTileSources = new HashMap<>();

    protected Layer layer;
    protected ClusterManager<BahnhofGeoItem> clusterer = null;
    protected List<TileCache> tileCaches = new ArrayList<>();

    private LatLong myPos = null;

    private CheckBox myLocSwitch = null;

    private DbAdapter dbAdapter;
    private String nickname;
    private BaseApplication baseApplication;
    private LocationManager locationManager;
    private boolean askedForPermission = false;
    private Marker missingMarker;
    private AlertDialog progress;

    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#c71c4d"));

        setSupportActionBar(binding.mapsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        nickname = baseApplication.getNickname();

        final Intent intent = getIntent();
        if (intent != null) {
            final Double latitude = (Double) intent.getSerializableExtra(EXTRAS_LATITUDE);
            final Double longitude = (Double) intent.getSerializableExtra(EXTRAS_LONGITUDE);
            setMyLocSwitch(false);
            if (latitude != null && longitude != null) {
                myPos = new LatLong(latitude, longitude);
            }
        }

        final DbsTileSource dbsBasic = new DbsTileSource(getString(R.string.dbs_osm_basic), "/styles/dbs-osm-basic/");
        onlineTileSources.put(dbsBasic.getName(), dbsBasic);
        final DbsTileSource dbsRailway = new DbsTileSource(getString(R.string.dbs_osm_railway), "/styles/dbs-osm-railway/");
        onlineTileSources.put(dbsRailway.getName(), dbsRailway);

        createMapViews();
        createTileCaches();
        checkPermissionsAndCreateLayersAndControls();
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
     * @return the mapviewposition set
     */
    protected IMapViewPosition initializePosition(final IMapViewPosition mvp) {
        if (myPos != null) {
            mvp.setMapPosition(new MapPosition(myPos, baseApplication.getZoomLevelDefault()));
        } else {
            mvp.setMapPosition(baseApplication.getLastMapPosition());
        }
        mvp.setZoomLevelMax(getZoomLevelMax());
        mvp.setZoomLevelMin(getZoomLevelMin());
        return mvp;
    }

    /**
     * Template method to create the map views.
     */
    protected void createMapViews() {
        binding.map.mapView.setClickable(true);
        binding.map.mapView.setOnMapDragListener(new MapsforgeMapView.MapDragListener() {
            @Override
            public void onDrag() {
                myLocSwitch.setChecked(false);
            }
        });
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
        for (final TileCache tileCache : tileCaches) {
            tileCache.purge();
        }
        tileCaches.clear();
    }

    protected XmlRenderTheme getRenderTheme() {
        final Uri mapTheme = baseApplication.getMapThemeUri();
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            final DocumentFile renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
            return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(renderThemeFile.getUri()));
        } catch (final Exception e) {
            Log.e( TAG,"Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile(final Uri mapUri) {
        if (mapUri == null || !DocumentFile.isDocumentUri(this, mapUri)) {
            return null;
        }
        try {
            final FileInputStream inputStream = (FileInputStream) getContentResolver().openInputStream(mapUri);
            return new MapFile(inputStream, 0, null);
        } catch (final FileNotFoundException e) {
            Log.e(TAG, "Can't open mapFile", e);
        }
        return null;
    }

    protected void createLayers() {
        final String map = baseApplication.getMap();
        final Uri mapUri = baseApplication.toUri(map);
        final MapDataStore mapFile = getMapFile(mapUri);

        if (mapFile != null) {
            final TileRendererLayer rendererLayer = new TileRendererLayer(this.tileCaches.get(0), mapFile,
                    this.binding.map.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE) {
                @Override
                public boolean onLongPress(final LatLong tapLatLong, final Point thisXY,
                                           final Point tapXY) {
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
                public boolean onLongPress(final LatLong tapLatLong, final Point thisXY,
                                           final Point tapXY) {
                    MapsActivity.this.onLongPress(tapLatLong);
                    return true;
                }
            };
            binding.map.mapView.getLayerManager().getLayers().add(this.layer);

            binding.map.mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            binding.map.mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
        }

    }

    private void onLongPress(final LatLong tapLatLong) {
        if (missingMarker == null) {
            // marker to show at the location
            final Drawable drawable = ContextCompat.getDrawable(this, R.drawable.marker_missing);
            final Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
            missingMarker = new Marker(tapLatLong, bitmap, -(bitmap.getWidth()/2), -bitmap.getHeight()) {
                @Override
                public boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
                    new SimpleDialogs().confirm(MapsActivity.this, R.string.add_missing_station, (dialogInterface, i) -> {
                        final Intent intent = new Intent(MapsActivity.this, DetailsActivity.class);
                        intent.putExtra(DetailsActivity.EXTRA_LATITUDE, getLatLong().latitude);
                        intent.putExtra(DetailsActivity.EXTRA_LONGITUDE, getLatLong().longitude);
                        startActivityForResult(intent, 0); // workaround to handle backstack correctly in DetailsActivity
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
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

        final String map = baseApplication.getMap();

        final MenuItem osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(map == null);
        osmMapnick.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, null));

        final SubMenu mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();
        for (final OnlineTileSource tileSource : onlineTileSources.values()) {
            final MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, tileSource.getName());
            mapItem.setChecked(tileSource.getName().equals(map));
            mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, tileSource.getName()));
        }

        final Uri mapDirectory = baseApplication.getMapDirectoryUri();
        if (mapDirectory != null) {
            final DocumentFile documentsTree = getDocumentFileFromTreeUri(mapDirectory);
            if (documentsTree != null) {
                for (final DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".map")) {
                        final MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, file.getName());
                        mapItem.setChecked(file.getUri().equals(baseApplication.toUri(map)));
                        mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, file.getUri().toString()));
                    }
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true);

        final MenuItem mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(item1 -> {
            openMapDirectoryChooser();
            return false;
        });

        final Uri mapTheme = baseApplication.getMapThemeUri();
        final Uri mapThemeDirectory = baseApplication.getMapThemeDirectoryUri();

        final MenuItem defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, null));
        final SubMenu themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null) {
            final DocumentFile documentsTree = getDocumentFileFromTreeUri(mapThemeDirectory);
            if (documentsTree != null) {
                for (final DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        final String themeName = file.getName();
                        final MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                        themeItem.setChecked(file.getUri().equals(mapTheme));
                        themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, file.getUri()));
                    } else if (file.isDirectory()) {
                        final DocumentFile childFile = file.findFile(file.getName() + ".xml");
                        if (childFile != null) {
                            final String themeName = file.getName();
                            final MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                            themeItem.setChecked(childFile.getUri().equals(mapTheme));
                            themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, childFile.getUri()));
                        }
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        final MenuItem themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(item12 -> {
            openThemeDirectoryChooser();
            return false;
        });

        return true;
    }

    private DocumentFile getDocumentFileFromTreeUri(final Uri uri) {
        try {
            return DocumentFile.fromTreeUri(getApplication(), uri);
        } catch (final Exception e) {
            Log.w(TAG, "Error getting DocumentFile from Uri: " + uri);
        }
        return null;
    }

    public void openDirectory(final int requestCode) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }


    private void openMapDirectoryChooser() {
        openDirectory(REQUEST_MAP_DIRECTORY);
    }

    private void openThemeDirectoryChooser() {
        openDirectory(REQUEST_THEME_DIRECTORY);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            final Uri uri = resultData.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                        resultData.getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                if (requestCode == REQUEST_MAP_DIRECTORY) {
                    baseApplication.setMapDirectoryUri(resultData.getData());
                    recreate();
                } else if (requestCode == REQUEST_THEME_DIRECTORY) {
                    baseApplication.setMapThemeDirectoryUri(resultData.getData());
                    recreate();
                }
            }
        }
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.map_info) {
            final MapInfoFragment mapInfoFragment = new MapInfoFragment();
            mapInfoFragment.show(getSupportFragmentManager(), "Map Info Dialog");
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void reloadMap() {
        progress = ProgressDialog.show(this, "", getResources().getString(R.string.loading_stations));
        destroyClusterManager();

        new LoadMapMarkerTask(this).start();
    }

    private void onStationsLoaded(final List<Station> stationList, final List<Upload> uploadList) {
        try {
            createClusterManager();
            addMarkers(stationList, uploadList);
            progress.dismiss();
            Toast.makeText(this, getResources().getQuantityString(R.plurals.stations_loaded, stationList.size(), stationList.size()), Toast.LENGTH_LONG).show();
        } catch (final Exception e) {
            Log.e(TAG, "Error loading markers", e);
        }
    }

    @Override
    public void onTap(final BahnhofGeoItem marker) {
        final Intent intent = new Intent(MapsActivity.this, DetailsActivity.class);
        final String id = marker.getStation().getId();
        final String country = marker.getStation().getCountry();
        try {
            final Station station = dbAdapter.getStationByKey(country, id);
            intent.putExtra(DetailsActivity.EXTRA_STATION, station);
            startActivityForResult(intent, 0); // workaround to handle backstack correctly in DetailsActivity
        } catch (final RuntimeException e) {
            Log.wtf(TAG, String.format("Could not fetch station id %s that we put onto the map", id), e);
        }
    }

    public void setMyLocSwitch(final boolean checked) {
        if (myLocSwitch != null) {
            myLocSwitch.setChecked(checked);
        }
        baseApplication.setLocationUpdates(checked);
    }

    private static class LoadMapMarkerTask extends Thread {
        private final WeakReference<MapsActivity> activityRef;

        public LoadMapMarkerTask(final MapsActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            final List<Station> stationList = activityRef.get().readStations();
            final List<Upload> uploadList = activityRef.get().readPendingUploads();
            final MapsActivity mapsActivity = activityRef.get();
            if (mapsActivity != null) {
                mapsActivity.runOnUiThread(()-> {
                    mapsActivity.onStationsLoaded(stationList, uploadList);
                });
            }
        }

    }

    private List<Station> readStations() {
        try {
            return dbAdapter.getAllStations(baseApplication.getStationFilter(), baseApplication.getCountryCodes());
        } catch (final Exception e) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden");
        }
        return null;
    }

    private List<Upload> readPendingUploads() {
        try {
            return dbAdapter.getPendingUploads(false);
        } catch (final Exception e) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden");
        }
        return null;
    }

    private List<MarkerBitmap> getMarkerBitmap() {
        final List<MarkerBitmap> markerBitmaps = new ArrayList<>();
        // prepare for marker icons.

        // small icon for maximum single item
        final Bitmap bitmapWithPhoto = loadBitmap(R.drawable.marker_green);
        final Bitmap markerWithoutPhoto = loadBitmap(R.drawable.marker_red);
        final Bitmap markerOwnPhoto = loadBitmap(R.drawable.marker_violet);
        final Bitmap markerPendingUpload = loadBitmap(R.drawable.marker_yellow);

        final Bitmap markerWithPhotoInactive = loadBitmap(R.drawable.marker_green_inactive);
        final Bitmap markerWithoutPhotoInactive = loadBitmap(R.drawable.marker_red_inactive);
        final Bitmap markerOwnPhotoInactive = loadBitmap(R.drawable.marker_violet_inactive);

        final Paint paint1 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint1.setStyle(Style.FILL);
        paint1.setTextAlign(Align.CENTER);
        FontFamily fontFamily = FontFamily.DEFAULT;
        FontStyle fontStyle = FontStyle.BOLD;
        paint1.setTypeface(fontFamily, fontStyle);
        paint1.setColor(Color.RED);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), markerWithoutPhoto, bitmapWithPhoto, markerOwnPhoto,
                markerWithoutPhotoInactive, markerWithPhotoInactive, markerOwnPhotoInactive, markerPendingUpload,
                new Point(0, -(markerWithoutPhoto.getHeight()/2)), 10f, 1, paint1));

        // small cluster icon. for 10 or less items.
        final Bitmap bitmapBalloonSN = loadBitmap(R.drawable.balloon_s_n);
        final Paint paint2 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint2.setStyle(Style.FILL);
        paint2.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint2.setTypeface(fontFamily, fontStyle);
        paint2.setColor(Color.BLACK);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), bitmapBalloonSN,
                new Point(0, 0), 9f, 10, paint2));

        // large cluster icon. 100 will be ignored.
        final Bitmap bitmapBalloonMN = loadBitmap(R.drawable.balloon_m_n);
        final Paint paint3 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint3.setStyle(Style.FILL);
        paint3.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint3.setTypeface(fontFamily, fontStyle);
        paint3.setColor(Color.BLACK);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), bitmapBalloonMN,
                new Point(0, 0), 11f, 100, paint3));
        return markerBitmaps;
    }

    private Bitmap loadBitmap(final int resourceId){
        final Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(ResourcesCompat.getDrawable(getResources(), resourceId, null));
        bitmap.incrementRefCount();
        return bitmap;
    }

    private void addMarkers(final List<Station> stationMarker, final List<Upload> uploadList) {
        double minLat = 0;
        double maxLat = 0;
        double minLon = 0;
        double maxLon = 0;
        for (final Station station : stationMarker) {
            final boolean isPendingUpload = isPendingUpload(station, uploadList);
            final BahnhofGeoItem geoItem = new BahnhofGeoItem(station, isPendingUpload);
            final LatLong bahnhofPos = geoItem.getLatLong();
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
        setProgressBarIndeterminateVisibility(false);

        if (myPos == null || (myPos.latitude == 0.0 && myPos.longitude == 0.0)) {
            myPos = new LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2);
        }
        updatePosition();
    }

    private boolean isPendingUpload(final Station station, final List<Upload> uploadList) {
        for (final Upload upload : uploadList) {
            if (upload.isPendingPhotoUpload() && station.getId().equals(upload.getStationId()) && station.getCountry().equals(upload.getCountry())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.layer instanceof TileDownloadLayer) {
            ((TileDownloadLayer)this.layer).onResume();
        }
        reloadMap();
        if (baseApplication.isLocationUpdates()) {
            registerLocationManager();
        }
    }

    private void createClusterManager() {
        // create clusterer instance
        clusterer = new ClusterManager<>(
                binding.map.mapView,
                getMarkerBitmap(),
                getZoomLevelMax(),
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
        final MapPosition mapPosition = binding.map.mapView.getModel().mapViewPosition.getMapPosition();
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
    public void onLocationChanged(final Location location) {
        myPos = new LatLong(location.getLatitude(), location.getLongitude());
        updatePosition();
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {

    }

    @Override
    public void onProviderEnabled(final String provider) {

    }

    @Override
    public void onProviderDisabled(final String provider) {

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
            final boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d(TAG, "GPS Enabled");
                if (locationManager != null) {
                    final Location loc = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    myPos = new LatLong(loc.getLatitude(), loc.getLongitude());
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
                        final Location loc = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        myPos = new LatLong(loc.getLatitude(), loc.getLongitude());
                    }
                }
            }
            setMyLocSwitch(true);
        } catch (final Exception e) {
            Log.e(TAG, "Error registering LocationManager", e);
            final Bundle b = new Bundle();
            b.putString("error", "Error registering LocationManager: " + e.toString());
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

    private void updatePosition() {
        if (myLocSwitch != null && myLocSwitch.isChecked()) {
            binding.map.mapView.setCenter(myPos);
            binding.map.mapView.repaint();
        }
    }

    protected class BahnhofGeoItem implements GeoItem {
        public Station station;
        public LatLong latLong;
        public boolean pendingUpload;

        public BahnhofGeoItem(final Station station, final boolean pendingUpload) {
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

        private MapMenuListener(final MapsActivity mapsActivity, final BaseApplication baseApplication, final String map) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.map = map;
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMap(null);
            } else {
                baseApplication.setMap(map);
            }

            final MapsActivity mapsActivity = mapsActivityRef.get();
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

        private MapThemeMenuListener(final MapsActivity mapsActivity, final BaseApplication baseApplication, final Uri mapThemeUri) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.mapThemeUri = mapThemeUri;
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) { // default theme
                baseApplication.setMapThemeUri(null);
            } else {
                baseApplication.setMapThemeUri(mapThemeUri);
            }

            final MapsActivity mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

}

