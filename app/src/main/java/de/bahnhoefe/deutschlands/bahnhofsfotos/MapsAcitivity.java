package de.bahnhoefe.deutschlands.bahnhofsfotos;


import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

public class MapsAcitivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener , LocationListener {

    private static final double MIN_NOTIFICATION_DISTANCE = 30.0d; // km
    private static final double EARTH_CIRCUMFERENCE = 40075.017d; // km at equator
    private GoogleMap mMap;
    private Double myLatitude=50d;
    private Double myLongitude=8d;
    private static final String TAG = MapsAcitivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 200;

    private List<Bahnhof> bahnhofMarker;
    private LatLng myPos;
    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = true;
    private long UPDATE_INTERVAL_IN_MILLISECONDS= 300000;
    private long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=300000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_acitivty);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //readBahnhoefe(); <-- gehört m.E. nicht hier her, sondern zum ersten Location-Update
        bahnhofMarker = new ArrayList<Bahnhof>(0); // no markers until we know where we are

        myPos = new LatLng(myLatitude,myLongitude);

        buildGoogleApiClient();
    }

    private void readBahnhoefe() {
        BahnhofsDbAdapter bahnhofsDbAdapter = new BahnhofsDbAdapter(this);

        try{
            bahnhofsDbAdapter.open();
            bahnhofMarker = bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myLatitude, myLongitude, false);

        }catch(Exception e){
            Log.i(TAG,"Datenbank konnte nicht geöffnet werden");
        } finally {
            bahnhofsDbAdapter.close();;
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
        addMarkers(bahnhofMarker,myPos);

    }

    private void addMarkers(List<Bahnhof> bahnhofMarker,LatLng myPos )
    {

        for(int i=0; i< bahnhofMarker.size();i++){
            LatLng bahnhofPos = new LatLng(Double.valueOf(bahnhofMarker.get(i).getLat()),Double.valueOf(bahnhofMarker.get(i).getLon()));
            mMap.addMarker(new MarkerOptions()
                    .title(bahnhofMarker.get(i).getTitle())
                    .position(bahnhofPos)
                    .snippet(String.valueOf(bahnhofMarker.get(i).getId()))
                    .icon(BitmapDescriptorFactory.defaultMarker(343)));
            mMap.setInfoWindowAdapter(this);
            mMap.setOnInfoWindowClickListener(this);

        }

        // Add a marker and moves the camera
        mMap.addMarker(new MarkerOptions().position(myPos).title("Meine aktuelle Position: ").icon(BitmapDescriptorFactory.defaultMarker(55)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 11));
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

        if(marker.getSnippet() != null){
            Class cls = DetailsActivity.class;
            Intent intent = new Intent(MapsAcitivity.this, cls);
            intent.putExtra(DetailsActivity.EXTRA_BAHNHOF_NAME, marker.getTitle());
            intent.putExtra(DetailsActivity.EXTRA_BAHNHOF_NUMBER, Integer.valueOf(marker.getSnippet()));
            intent.putExtra(DetailsActivity.EXTRA_POSITION, marker.getPosition());
            startActivity(intent);
        }else{
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
        myLatitude=location.getLatitude();
        myLongitude=location.getLongitude();
        mMap.clear();
        myPos= new LatLng(myLatitude, myLongitude);

        readBahnhoefe();

        // check if a user notification is appropriate
        checkNearestStation ();

        addMarkers(bahnhofMarker,myPos);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 11));
        //Toast.makeText(this, myPos.latitude+"", Toast.LENGTH_SHORT).show();
    }

    private void checkNearestStation() {
        double minDist = MIN_NOTIFICATION_DISTANCE;
        Bahnhof nearest = null;
        for (Bahnhof bahnhof: bahnhofMarker) {
            double dist = calcDistance (bahnhof);
            if (dist < minDist) {
                nearest = bahnhof;
                minDist = dist;
            }
        }
        if (nearest != null)
            notifyNearest (nearest, minDist);
    }

    private void notifyNearest(Bahnhof nearest, double minDist) {
        int notificationId = 001;
        // Build intent for notification content
        //Intent viewIntent = new Intent(this, ViewEventActivity.class);
        //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
        //PendingIntent viewPendingIntent =
        //        PendingIntent.getActivity(this, 0, viewIntent, 0);

        // Build an intent for an action to view a map
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        Uri geoUri = Uri.parse("geo:" + nearest.getLat() + "," + nearest.getLon());
        mapIntent.setData(geoUri);
        PendingIntent mapPendingIntent =
                PendingIntent.getActivity(this, 0, mapIntent, 0);

        DecimalFormat format = new DecimalFormat ("#0.00");
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_logotrain)
                        .setContentTitle("Station near you")
                        .setContentText("Bahnhof " + nearest.getTitle() + " in " + format.format(minDist) + " km")
                        .setContentIntent(mapPendingIntent);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Calculate the distance between the given station and our current position (myLatitude, myLongitude)
     * @param bahnhof the station to calculate the distance to
     * @return the distance
     */
    private double calcDistance(Bahnhof bahnhof) {
        // Wir nähern für glatte Oberflächen, denn wir sind an Abständen kleiner 1km interessiert
        double lateralDiff = myLatitude - bahnhof.getLat();
        double longDiff = (Math.abs(myLatitude) < 89.99d) ?
                (myLongitude - bahnhof.getLon()) / Math.cos(myLatitude) :
                0.0d; // at the poles, longitude doesn't matter
        // simple Pythagoras now.
        double distance = Math.sqrt(Math.pow(lateralDiff, 2.0d) + Math.pow(longDiff, 2.0d)) * EARTH_CIRCUMFERENCE / 360.0d;
        return distance;
    }

    protected void startLocationUpdates() {
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
                                    ActivityCompat.requestPermissions(MapsAcitivity.this,
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
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsAcitivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}

