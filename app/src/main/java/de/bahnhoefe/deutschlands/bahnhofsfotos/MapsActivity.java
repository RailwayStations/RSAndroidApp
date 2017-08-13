package de.bahnhoefe.deutschlands.bahnhofsfotos;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final int LOCATION_REQUEST_INTERVAL_MILLIS = 500;
    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 200;

    private List<Bahnhof> bahnhofMarker;
    private LatLng myPos;

    private CheckBox myLocSwitch = null;

    /**
     * Provides the entry point to Google Play services.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;
    private boolean nextCameraChangeIsManual = true;
    private BahnhofsDbAdapter dbAdapter;
    private String nickname;
    private Marker myPositionMarker;
    private BaseApplication baseApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#c71c4d"));
        }

        Toolbar myToolbar = (Toolbar) findViewById(R.id.maps_toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();
        nickname = baseApplication.getNickname();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bahnhofMarker = new ArrayList<>(0); // no markers until we know where we are

        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
        myLocSwitch = new CheckBox(this);
        myLocSwitch.setButtonDrawable(R.drawable.ic_gps_fix_selector);
        myLocSwitch.setChecked(true);
        item.setActionView(myLocSwitch);

        menu.findItem(R.id.menu_toggle_photo).setIcon(baseApplication.getPhotoFilter().getIcon());

        return true;
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

    private class LoadMapMarkerTask extends AsyncTask<Void, Void, Integer> {

        final android.app.AlertDialog progress = ProgressDialog.show(MapsActivity.this, "", getResources().getString(R.string.loading_stations));

        @Override
        protected void onPreExecute() {
            progress.show();
            mMap.clear();
            myPositionMarker = null;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            readBahnhoefe();
            return bahnhofMarker.size();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            addMarkers(bahnhofMarker);
            progress.dismiss();
            Toast.makeText(MapsActivity.this, bahnhofMarker.size() + " Bahnhöfe geladen", Toast.LENGTH_LONG).show();
        }
    }

    private void readBahnhoefe() {
        try {
            bahnhofMarker = dbAdapter.getAllBahnhoefe(baseApplication.getPhotoFilter());
        } catch (Exception e) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden");
        }
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
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        reloadMap();
        if (myPos != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 11));
        } else {
            mMap.moveCamera(CameraUpdateFactory.zoomBy(11));
        }
        nextCameraChangeIsManual = false;
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if (nextCameraChangeIsManual) {
                    myLocSwitch.setChecked(false);
                }
            }
        });
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                nextCameraChangeIsManual = true;
            }
        });
    }

    private void addMarkers(List<Bahnhof> bahnhofMarker) {
        double minLat = 0;
        double maxLat = 0;
        double minLon = 0;
        double maxLon = 0;
        for (Bahnhof bahnhof : bahnhofMarker) {
            LatLng bahnhofPos = bahnhof.getPosition();
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
            mMap.addMarker(new MarkerOptions()
                    .title(bahnhof.getTitle())
                    .position(bahnhofPos)
                    .snippet(String.valueOf(bahnhof.getId()))
                    .icon(getMarkerIcon(bahnhof, nickname)));
        }

        mMap.setInfoWindowAdapter(this);
        mMap.setOnInfoWindowClickListener(this);
        if (myPos == null || (myPos.latitude == 0.0 && myPos.longitude == 0.0)) {
            myPos = new LatLng((minLat + maxLat) / 2, (minLon + maxLon) / 2);
            updatePosition();
        }
    }

    private BitmapDescriptor getMarkerIcon(Bahnhof bahnhof, String nickname) {
        if (!bahnhof.hasPhoto()) {
            return BitmapDescriptorFactory.defaultMarker(343);
        } else if (bahnhof.getPhotographer().equals(nickname)) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
        }
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }


    @Override
    public View getInfoContents(Marker marker) {
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.info_window, null, false);
        ((TextView) view.findViewById(R.id.tvbahnhofname)).setText(marker.getTitle());
        if (marker.getSnippet() != null) {
            ((TextView) view.findViewById(R.id.tvbahnhofnr)).setText("BahnhofNr: " + marker.getSnippet());
        } else {
            ((TextView) view.findViewById(R.id.tvbahnhofnr)).setText(" ");
        }

        ((TextView) view.findViewById(R.id.tvlatlon)).setText(marker.getPosition().toString());
        return view;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.getSnippet() != null) {
            Intent intent = new Intent(MapsActivity.this, DetailsActivity.class);
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
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
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
        myPos = new LatLng(location.getLatitude(), location.getLongitude());
        updatePosition();
    }

    private void updatePosition() {
        if (myPositionMarker == null) {
            myPositionMarker = mMap.addMarker(new MarkerOptions().position(myPos).title("Meine aktuelle Position: ").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        } else {
            myPositionMarker.setPosition(myPos);
        }

        nextCameraChangeIsManual = false;
        if (myLocSwitch.isChecked()) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, mMap.getCameraPosition().zoom));
        }
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
                                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
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
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

