package de.bahnhoefe.deutschlands.bahnhofsfotos;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final int MIN_METER_DISTANCE_BEFORE_RELOAD = 1000;
    public static final int LOCATION_REQUEST_INTERVAL_MILLIS = 500;
    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 200;

    private List<Bahnhof> bahnhofMarker;
    private LatLng myPos;
    private LatLng lastLoadPos;

    // views
    private CheckBox myLocSwitch = null;

    /**
     * Provides the entry point to Google Play services.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = true;
    private BahnhofsDbAdapter dbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bahnhofMarker = new ArrayList<Bahnhof>(0); // no markers until we know where we are

        myPos = new LatLng(50d, 8d);

        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
        myLocSwitch = new CheckBox(this);
        myLocSwitch.setButtonDrawable(R.drawable.ic_gps_fix_selector);
        item.setActionView(myLocSwitch);
        initMyLocationSwitchButton(myLocSwitch);
        return true;
    }

    private void initMyLocationSwitchButton(final CheckBox locSwitch) {
        myLocSwitch = locSwitch;
        myLocSwitch.setOnClickListener(new MyLocationListener(this));
        switchMyLocationButton();
    }

    private void switchMyLocationButton() {
        if (myLocSwitch != null) {
            myLocSwitch.setChecked(mRequestingLocationUpdates);
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        }
    }

    private class MyLocationListener implements View.OnClickListener {

        private final WeakReference<MapsActivity> mapRef;

        MyLocationListener(@NonNull final MapsActivity map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void onClick(final View view) {
            final MapsActivity map = mapRef.get();
            if (map != null) {
                mRequestingLocationUpdates = !mRequestingLocationUpdates;
                map.switchMyLocationButton();
            }
        }
    }

    private void readBahnhoefe() {
        try{
            bahnhofMarker = dbAdapter.getBahnhoefeByLatLngRectangle(myPos, false);
        }catch(Exception e){
            Log.i(TAG,"Datenbank konnte nicht geöffnet werden");
        }

        Toast.makeText(this, bahnhofMarker.size() + " Bahnhöfe geladen", Toast.LENGTH_SHORT).show();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        addMarkers(bahnhofMarker, myPos);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 11));
    }

    private void addMarkers(List<Bahnhof> bahnhofMarker,LatLng myPos) {
        for(Bahnhof bahnhof: bahnhofMarker){
            LatLng bahnhofPos = bahnhof.getPosition();
            mMap.addMarker(new MarkerOptions()
                    .title(bahnhof.getTitle())
                    .position(bahnhofPos)
                    .snippet(String.valueOf(bahnhof.getId()))
                    .icon(BitmapDescriptorFactory.defaultMarker(343)));
        }

        // Add a marker and moves the camera
        mMap.addMarker(new MarkerOptions().position(myPos).title("Meine aktuelle Position: ").icon(BitmapDescriptorFactory.defaultMarker(55)));
        mMap.setInfoWindowAdapter(this);
        mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }


    @Override
    public View getInfoContents(Marker marker) {

        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.info_window,null,false);
        ((TextView)view.findViewById(R.id.tvbahnhofname)).setText(marker.getTitle());
        if(marker.getSnippet() != null){
            ((TextView)view.findViewById(R.id.tvbahnhofnr)).setText("BahnhofNr: " + marker.getSnippet());
        }else{
            ((TextView)view.findViewById(R.id.tvbahnhofnr)).setText(" ");
        }

        ((TextView)view.findViewById(R.id.tvlatlon)).setText(marker.getPosition().toString());
        return view;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        BaseApplication baseApplication = (BaseApplication)getApplication();
        String countryShortCode = baseApplication.getCountryShortCode();

        if(marker.getSnippet() != null){

            Class cls = DetailsActivity.class;
            Intent intent = new Intent(MapsActivity.this, cls);
            long id = Long.valueOf(marker.getSnippet());
            try {
                Bahnhof bahnhof = dbAdapter.fetchBahnhofByBahnhofId(id);
                intent.putExtra(DetailsActivity.EXTRA_BAHNHOF, bahnhof);
                startActivity(intent);
            } catch (RuntimeException e) {
                Log.wtf(TAG, String.format("Could not fetch station id %s that we put onto the map", id), e);
            }
        } else {
            marker.hideInfoWindow();
        }


    }


    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        createLocationRequest();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }


    }
    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (!mRequestingLocationUpdates) {
            return;
        }
        mMap.clear();
        myPos= new LatLng(location.getLatitude(), location.getLongitude());

        if (lastLoadPos == null || distanceInMeter(lastLoadPos, myPos) > MIN_METER_DISTANCE_BEFORE_RELOAD) {
            readBahnhoefe();
            lastLoadPos = myPos;
        }

        addMarkers(bahnhofMarker,myPos);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, mMap.getCameraPosition().zoom));
    }

    private float distanceInMeter(LatLng oldPos, LatLng myPos) {
        float[] result = new float[1];
        Location.distanceBetween(oldPos.latitude, oldPos.longitude, myPos.latitude, myPos.longitude, result);
        return result[0];
    }


    protected void startLocationUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            return;
        }
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Check Permissions Now


                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showMessageOKCancel("You need to allow access to Locations",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MapsActivity.this,
                                            new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},
                                            PERMISSION_REQUEST_CODE);
                                }
                            });
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this);
                } else {
                    ActivityCompat.requestPermissions(
                            this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_CODE);

                }
            } else {
                // permission has been granted, continue as usual
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, mLocationRequest, this);
            }
        }else{
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);

        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL_MILLIS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL_MILLIS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}

