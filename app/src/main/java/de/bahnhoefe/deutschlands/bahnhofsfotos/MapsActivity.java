package de.bahnhoefe.deutschlands.bahnhofsfotos;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import org.mapsforge.map.android.util.ExternalRenderThemeUsingJarResources;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.MapInfoFragment;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.ClusterManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.GeoItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MapsforgeMapView;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MarkerBitmap;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.TapHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;

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

    protected MapsforgeMapView mapView;
    protected Layer layer;
    protected ClusterManager clusterer = null;
    protected List<TileCache> tileCaches = new ArrayList<TileCache>();

    private List<Bahnhof> bahnhofList;
    private LatLong myPos = null;

    private CheckBox myLocSwitch = null;

    private BahnhofsDbAdapter dbAdapter;
    private String nickname;
    private BaseApplication baseApplication;
    private LocationManager locationManager;
    private boolean askedForPermission = false;
    private Marker missingMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());

        setContentView(R.layout.activity_maps_activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#c71c4d"));
        }

        Toolbar myToolbar = findViewById(R.id.maps_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        nickname = baseApplication.getNickname();

        bahnhofList = new ArrayList<>(0); // no markers until we know where we are

        Intent intent = getIntent();
        if (intent != null) {
            Double latitude = (Double) intent.getSerializableExtra(EXTRAS_LATITUDE);
            Double longitude = (Double) intent.getSerializableExtra(EXTRAS_LONGITUDE);
            setMyLocSwitch(false);
            if (latitude != null && longitude != null) {
                myPos = new LatLong(latitude, longitude);
            }
        }

        createMapViews();
        createTileCaches();
        checkPermissionsAndCreateLayersAndControls();
    }

    protected void createTileCaches() {
        this.tileCaches.add(AndroidUtil.createTileCache(this, getPersistableId(),
                this.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                this.mapView.getModel().frameBufferModel.getOverdrawFactor(), true));
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
        initializePosition(mapView.getModel().mapViewPosition);
    }

    /**
     * initializes the map view position.
     *
     * @param mvp the map view position to be set
     * @return the mapviewposition set
     */
    protected IMapViewPosition initializePosition(IMapViewPosition mvp) {
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
        mapView = findViewById(getMapViewId());
        mapView.setClickable(true);
        mapView.setOnMapDragListener(new MapsforgeMapView.MapDragListener() {
            @Override
            public void onDrag() {
                myLocSwitch.setChecked(false);
            }
        });
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getMapZoomControls().setAutoHide(true);
        mapView.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
        mapView.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());

        //this.mapView.getModel().displayModel.setFixedTileSize(256);
        mapView.getMapZoomControls().setZoomControlsOrientation(MapZoomControls.Orientation.VERTICAL_IN_OUT);
        mapView.getMapZoomControls().setZoomInResource(R.drawable.zoom_control_in);
        mapView.getMapZoomControls().setZoomOutResource(R.drawable.zoom_control_out);
        mapView.getMapZoomControls().setMarginHorizontal(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
        mapView.getMapZoomControls().setMarginVertical(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
    }

    protected int getMapViewId() {
        return R.id.mapView;
    }

    protected byte getZoomLevelMax() {
        return mapView.getModel().mapViewPosition.getZoomLevelMax();
    }

    protected byte getZoomLevelMin() {
        return mapView.getModel().mapViewPosition.getZoomLevelMin();
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
        Uri mapTheme = baseApplication.getMapTheme();
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            DocumentFile renderThemeFile = DocumentFile.fromTreeUri(getApplication(), mapTheme);
            if (renderThemeFile.isDirectory()) {
                for (DocumentFile file : renderThemeFile.listFiles()) {
                    if (file.getName().equals(renderThemeFile.getName() + ".xml")) {
                        renderThemeFile = file;
                    }
                }
            }
            return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(renderThemeFile.getUri()));
        } catch (Exception e) {
            Log.e( TAG,"Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile() {
        if (baseApplication.getMap() == null) {
            return null;
        }
        final Uri mapFile = baseApplication.getMap();
        if (mapFile == null || !DocumentFile.isDocumentUri(this, mapFile)) {
            return null;
        }
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(mapFile, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            return new MapFile(new FileInputStream(fileDescriptor), 0, null);
        } catch (FileNotFoundException ignored) {
            Log.e(TAG, "Can't open mapFile", ignored);
        }
        return null;
    }

    protected void createLayers() {
        final MapDataStore mapFile = getMapFile();

        if (mapFile != null) {
            TileRendererLayer tileRendererLayer1 = new TileRendererLayer(this.tileCaches.get(0), mapFile,
                    this.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE) {
                @Override
                public boolean onLongPress(LatLong tapLatLong, Point thisXY,
                                           Point tapXY) {
                    MapsActivity.this.onLongPress(tapLatLong);
                    return true;
                }
            };
            tileRendererLayer1.setXmlRenderTheme(getRenderTheme());
            this.layer = tileRendererLayer1;
            mapView.getLayerManager().getLayers().add(this.layer);
        } else {
            OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
            tileSource.setUserAgent("railway-stations.org-android");
            this.layer = new TileDownloadLayer(this.tileCaches.get(0),
                    this.mapView.getModel().mapViewPosition, tileSource,
                    AndroidGraphicFactory.INSTANCE) {
                @Override
                public boolean onLongPress(LatLong tapLatLong, Point thisXY,
                                           Point tapXY) {
                    MapsActivity.this.onLongPress(tapLatLong);
                    return true;
                }
            };
            mapView.getLayerManager().getLayers().add(this.layer);

            mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
        }

    }

    private void onLongPress(LatLong tapLatLong) {
        if (missingMarker == null) {
            // marker to show at the location
            Drawable drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? getDrawable(R.drawable.marker_missing) : getResources().getDrawable(R.drawable.marker_missing);
            Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
            missingMarker = new Marker(tapLatLong, bitmap, -(bitmap.getWidth()/2), -bitmap.getHeight()) {
                @Override
                public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                    new SimpleDialogs().confirm(MapsActivity.this, R.string.add_missing_station, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(MapsActivity.this, DetailsActivity.class);
                            intent.putExtra(DetailsActivity.EXTRA_LATITUDE, getLatLong().latitude);
                            intent.putExtra(DetailsActivity.EXTRA_LONGITUDE, getLatLong().longitude);
                            startActivityForResult(intent, 0); // workaround to handle backstack correctly in DetailsActivity
                        }
                    });
                    return false;
                }
            };
            mapView.getLayerManager().getLayers().add(missingMarker);
        } else {
            missingMarker.setLatLong(tapLatLong);
            missingMarker.requestRedraw();
        }

        // feedback for long click
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
        myLocSwitch = new CheckBox(this);
        myLocSwitch.setButtonDrawable(R.drawable.ic_gps_fix_selector);
        myLocSwitch.setChecked(baseApplication.isLocationUpdates());
        item.setActionView(myLocSwitch);
        myLocSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
               @Override
               public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                   baseApplication.setLocationUpdates(isChecked);
                   if (isChecked) {
                       askedForPermission = false;
                       registerLocationManager();
                   } else {
                       unregisterLocationManager();
                   }
               }
           }
        );

        menu.findItem(R.id.menu_toggle_photo).setIcon(baseApplication.getPhotoFilter().getIcon());

        final Uri mapUri = baseApplication.getMap();

        MenuItem osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(mapUri == null);
        osmMapnick.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, null));

        SubMenu mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();

        final Uri mapDirectory = baseApplication.getMapDirectory();
        if (mapDirectory != null) {
            DocumentFile documentsTree = DocumentFile.fromTreeUri(getApplication(), mapDirectory);
            if (documentsTree != null) {
                for (DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".map")) {
                        MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, file.getName());
                        mapItem.setChecked(file.getUri().equals(mapUri));
                        mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, file.getUri()));
                    }
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true);

        MenuItem mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                openMapDirectoryChooser();
                return false;
            }
        });

        final Uri mapTheme = baseApplication.getMapTheme();
        final Uri mapThemeDirectory = baseApplication.getMapThemeDirectory();

        MenuItem defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, null));
        SubMenu themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null && DocumentFile.isDocumentUri(this, mapThemeDirectory)) {
            DocumentFile documentsTree = DocumentFile.fromTreeUri(getApplication(), mapThemeDirectory);
            if (documentsTree != null) {
                for (DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        String themeName = file.getName();
                        MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                        themeItem.setChecked(file.getUri().equals(mapTheme));
                        themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, file.getUri()));
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        MenuItem themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                openThemeDirectoryChooser();
                return false;
            }
        });

        return true;
    }

    public void openDirectory(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
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
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                        resultData.getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                if (requestCode == REQUEST_MAP_DIRECTORY) {
                    baseApplication.setMapDirectory(resultData.getData());
                    recreate();
                } else if (requestCode == REQUEST_THEME_DIRECTORY) {
                    baseApplication.setMapThemeDirectory(resultData.getData());
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
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        purgeTileCaches();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_photo:
                PhotoFilter photoFilter = baseApplication.getPhotoFilter().getNextFilter();
                item.setIcon(photoFilter.getIcon());
                baseApplication.setPhotoFilter(photoFilter);
                reloadMap();
                break;
            case R.id.map_info:
                MapInfoFragment mapInfoFragment = new MapInfoFragment();
                mapInfoFragment.show(getSupportFragmentManager(), "Map Info Dialog");

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void reloadMap() {
        new LoadMapMarkerTask().execute((Void)null);
    }

    @Override
    public void onTap(BahnhofGeoItem marker) {
        Intent intent = new Intent(MapsActivity.this, DetailsActivity.class);
        String id = marker.getBahnhof().getId();
        String country = marker.getBahnhof().getCountry();
        try {
            Bahnhof bahnhof = dbAdapter.fetchBahnhof(country, id);
            intent.putExtra(DetailsActivity.EXTRA_BAHNHOF, bahnhof);
            startActivityForResult(intent, 0); // workaround to handle backstack correctly in DetailsActivity
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

    private class LoadMapMarkerTask extends AsyncTask<Void, Void, Integer> {

        final android.app.AlertDialog progress = ProgressDialog.show(MapsActivity.this, "", getResources().getString(R.string.loading_stations));

        @Override
        protected void onPreExecute() {
            progress.show();
            destroyClusterManager();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            readBahnhoefe();
            return bahnhofList.size();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            createClusterManager();
            addMarkers(bahnhofList);
            progress.dismiss();
            Toast.makeText(MapsActivity.this, getResources().getQuantityString(R.plurals.stations_loaded, bahnhofList.size(), bahnhofList.size()), Toast.LENGTH_LONG).show();
        }
    }

    private void readBahnhoefe() {
        try {
            bahnhofList = dbAdapter.getAllBahnhoefe(baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
        } catch (Exception e) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden");
        }
    }

    private List<MarkerBitmap> getMarkerBitmap() {
        List<MarkerBitmap> markerBitmaps = new ArrayList<>();
        // prepare for marker icons.

        // small icon for maximum single item
        Bitmap bitmapWithPhoto = loadBitmap(R.drawable.marker_green);
        Bitmap markerWithoutPhoto = loadBitmap(R.drawable.marker_red);
        Bitmap markerOwnPhoto = loadBitmap(R.drawable.marker_violet);

        Bitmap markerWithPhotoInactive = loadBitmap(R.drawable.marker_green_inactive);
        Bitmap markerWithoutPhotoInactive = loadBitmap(R.drawable.marker_red_inactive);
        Bitmap markerOwnPhotoInactive = loadBitmap(R.drawable.marker_violet_inactive);

        Paint paint1 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint1.setStyle(Style.FILL);
        paint1.setTextAlign(Align.CENTER);
        FontFamily fontFamily = FontFamily.DEFAULT;
        FontStyle fontStyle = FontStyle.BOLD;
        paint1.setTypeface(fontFamily, fontStyle);
        paint1.setColor(Color.RED);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), markerWithoutPhoto, bitmapWithPhoto, markerOwnPhoto,
                markerWithoutPhotoInactive, markerWithPhotoInactive, markerOwnPhotoInactive,
                new Point(0, -(markerWithoutPhoto.getHeight()/2)), 10f, 1, paint1));

        // small cluster icon. for 10 or less items.
        Bitmap bitmapBalloonSN = loadBitmap(R.drawable.balloon_s_n);
        Paint paint2 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint2.setStyle(Style.FILL);
        paint2.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint2.setTypeface(fontFamily, fontStyle);
        paint2.setColor(Color.BLACK);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), bitmapBalloonSN,
                new Point(0, 0), 9f, 10, paint2));

        // large cluster icon. 100 will be ignored.
        Bitmap bitmapBalloonMN = loadBitmap(R.drawable.balloon_m_n);
        Paint paint3 = AndroidGraphicFactory.INSTANCE.createPaint();
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

    private Bitmap loadBitmap(int resourceId){
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(resourceId));
        bitmap.incrementRefCount();
        return bitmap;
    }

    private void addMarkers(List<Bahnhof> bahnhofMarker) {
        double minLat = 0;
        double maxLat = 0;
        double minLon = 0;
        double maxLon = 0;
        for (Bahnhof bahnhof : bahnhofMarker) {
            BahnhofGeoItem geoItem = new BahnhofGeoItem(bahnhof);
            LatLong bahnhofPos = geoItem.getLatLong();
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
        clusterer = new ClusterManager(
                mapView,
                getMarkerBitmap(),
                getZoomLevelMax(),
                this);
        // this uses the framebuffer position, the mapview position can be out of sync with
        // what the user sees on the screen if an animation is in progress
        this.mapView.getModel().frameBufferModel.addObserver(clusterer);
    }

    @Override
    protected void onPause() {
        if (this.layer instanceof TileDownloadLayer) {
            ((TileDownloadLayer)this.layer).onPause();
        }
        unregisterLocationManager();
        final MapPosition mapPosition = mapView.getModel().mapViewPosition.getMapPosition();
        baseApplication.setLastMapPosition(mapPosition);
        destroyClusterManager();
        super.onPause();
    }

    private void destroyClusterManager() {
        if (clusterer != null) {
            clusterer.destroyGeoClusterer();
            this.mapView.getModel().frameBufferModel.removeObserver(clusterer);
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
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
        } else if (requestCode == REQUEST_MAP_DIRECTORY) {
            Log.i(TAG, "Received response for external file permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted
                openMapDirectoryChooser();
            } else {
                //Permission not granted
                Toast.makeText(MapsActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_THEME_DIRECTORY) {
            Log.i(TAG, "Received response for external file permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted
                openThemeDirectoryChooser();
            } else {
                //Permission not granted
                Toast.makeText(MapsActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
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
            boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d(TAG, "GPS Enabled");
                if (locationManager != null) {
                    Location loc = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    myPos = new LatLong(loc.getLatitude(), loc.getLongitude());
                }
            } else {
                // getting network status
                boolean isNetworkEnabled = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d(TAG, "Network Location enabled");
                    if (locationManager != null) {
                        Location loc = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        myPos = new LatLong(loc.getLatitude(), loc.getLongitude());
                    }
                }
            }
            setMyLocSwitch(true);
        } catch (Exception e) {
            Log.e(TAG, "Error registering LocationManager", e);
            Bundle b = new Bundle();
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
            mapView.setCenter(myPos);
            mapView.repaint();
        }
    }

    protected class BahnhofGeoItem implements GeoItem {
        public Bahnhof bahnhof;
        public LatLong latLong;

        public BahnhofGeoItem(Bahnhof bahnhof) {
            this.bahnhof = bahnhof;
            this.latLong = new LatLong(bahnhof.getLat(), bahnhof.getLon());
        }

        public LatLong getLatLong() {
            return latLong;
        }

        @Override
        public String getTitle() {
            return bahnhof.getTitle();
        }

        @Override
        public boolean hasPhoto() {
            return bahnhof.hasPhoto();
        }

        @Override
        public boolean ownPhoto() {
            return hasPhoto() && bahnhof.getPhotographer().equals(nickname);
        }

        @Override
        public boolean stationActive() {
            return bahnhof.isActive();
        }

        public Bahnhof getBahnhof() {
            return bahnhof;
        }

    }

    private static class MapMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<MapsActivity> mapsActivityRef;

        private BaseApplication baseApplication;

        private Uri mapUri;

        private MapMenuListener(final MapsActivity mapsActivity, final BaseApplication baseApplication, final Uri mapUri) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.mapUri = mapUri;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMap(null);
            } else {
                baseApplication.setMap(mapUri);
            }

            MapsActivity mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

    private static class MapThemeMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<MapsActivity> mapsActivityRef;

        private BaseApplication baseApplication;

        private Uri mapThemeUri;

        private MapThemeMenuListener(final MapsActivity mapsActivity, final BaseApplication baseApplication, final Uri mapThemeUri) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.mapThemeUri = mapThemeUri;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) { // default theme
                baseApplication.setMapTheme(null);
            } else {
                baseApplication.setMapTheme(mapThemeUri);
            }

            MapsActivity mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

}

