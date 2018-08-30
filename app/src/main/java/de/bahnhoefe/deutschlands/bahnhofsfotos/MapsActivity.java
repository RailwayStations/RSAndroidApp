package de.bahnhoefe.deutschlands.bahnhofsfotos;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.ClusterManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.GeoItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MapsforgeMapView;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MarkerBitmap;
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.TapHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
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
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.model.MapViewPosition;

public class MapsActivity extends AppCompatActivity implements LocationListener, TapHandler<MapsActivity.BahnhofGeoItem> {

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 500; // minute

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int REQUEST_FINE_LOCATION = 1;

    protected MapsforgeMapView mapView;
    protected TileDownloadLayer downloadLayer;
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
    protected MapViewPosition initializePosition(MapViewPosition mvp) {
        if (myPos != null) {
            mvp.setMapPosition(new MapPosition(myPos, getZoomLevelDefault()));
        } else {
            mvp.setMapPosition(new MapPosition(new LatLong(0, 0), getZoomLevelDefault()));
        }
        mvp.setZoomLevelMax(getZoomLevelMax());
        mvp.setZoomLevelMin(getZoomLevelMin());
        return mvp;
    }

    /**
     * @return the default starting zoom level if nothing is encoded in the map file.
     */
    protected byte getZoomLevelDefault() {
        return (byte) 12;
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

    protected void createLayers() {
        this.downloadLayer = new TileDownloadLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition, OpenStreetMapMapnik.INSTANCE,
                AndroidGraphicFactory.INSTANCE);
        mapView.getLayerManager().getLayers().add(this.downloadLayer);

        mapView.setZoomLevelMin(OpenStreetMapMapnik.INSTANCE.getZoomLevelMin());
        mapView.setZoomLevelMax(OpenStreetMapMapnik.INSTANCE.getZoomLevelMax());
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

        return true;
    }

    /**
     * Android Activity life cycle method.
     */
    @Override
    protected void onDestroy() {
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        tileCaches.clear();
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
        try {
            Bahnhof bahnhof = dbAdapter.fetchBahnhofByBahnhofId(id);
            intent.putExtra(DetailsActivity.EXTRA_BAHNHOF, bahnhof);
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
        List<MarkerBitmap> markerBitmaps = new ArrayList<MarkerBitmap>();
        // prepare for marker icons.
        Drawable balloon;
        // small icon for maximum single item
        balloon = getResources().getDrawable(R.drawable.marker_green);
        Bitmap bitmapWithPhoto = AndroidGraphicFactory.convertToBitmap(balloon);
        bitmapWithPhoto.incrementRefCount();
        balloon = getResources().getDrawable(R.drawable.marker_red);
        Bitmap markerWithoutPhoto = AndroidGraphicFactory.convertToBitmap(balloon);
        markerWithoutPhoto.incrementRefCount();
        balloon = getResources().getDrawable(R.drawable.marker_violet);
        Bitmap markerOwnPhoto = AndroidGraphicFactory.convertToBitmap(balloon);
        markerWithoutPhoto.incrementRefCount();
        Paint paint1;
        paint1 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint1.setStyle(Style.FILL);
        paint1.setTextAlign(Align.CENTER);
        FontFamily fontFamily = FontFamily.DEFAULT;
        FontStyle fontStyle = FontStyle.BOLD;
        paint1.setTypeface(fontFamily, fontStyle);
        paint1.setColor(Color.RED);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), markerWithoutPhoto, bitmapWithPhoto, markerOwnPhoto,
                new Point(0, -(markerWithoutPhoto.getHeight()/2)), 10f, 1, paint1));

        // small cluster icon. for 10 or less items.
        balloon = getResources().getDrawable(R.drawable.balloon_s_n);
        Bitmap bitmapBalloonSN = AndroidGraphicFactory
                .convertToBitmap(balloon);
        bitmapBalloonSN.incrementRefCount();
        Paint paint2;
        paint2 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint2.setStyle(Style.FILL);
        paint2.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint2.setTypeface(fontFamily, fontStyle);
        paint2.setColor(Color.BLACK);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), bitmapBalloonSN,
                bitmapBalloonSN, bitmapBalloonSN, new Point(0, 0), 9f, 10, paint2));

        // large cluster icon. 100 will be ignored.
        balloon = getResources().getDrawable(R.drawable.balloon_m_n);
        Bitmap bitmapBalloonMN = AndroidGraphicFactory.convertToBitmap(balloon);
        bitmapBalloonMN.incrementRefCount();
        Paint paint3;
        paint3 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint3.setStyle(Style.FILL);
        paint3.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint3.setTypeface(fontFamily, fontStyle);
        paint3.setColor(Color.BLACK);
        markerBitmaps.add(new MarkerBitmap(this.getApplicationContext(), bitmapBalloonMN,
                bitmapBalloonMN, bitmapBalloonMN, new Point(0, 0), 11f, 100, paint3));
        return markerBitmaps;
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
        this.downloadLayer.onResume();
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
        this.downloadLayer.onPause();
        unregisterLocationManager();
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

        public Bahnhof getBahnhof() {
            return bahnhof;
        }

    }

}

