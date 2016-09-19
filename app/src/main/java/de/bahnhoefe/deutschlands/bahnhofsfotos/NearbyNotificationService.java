package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManagerFactory;

public class NearbyNotificationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // some useful constants
    private static final double MIN_NOTIFICATION_DISTANCE = 1.0d; // km
    private static final double EARTH_CIRCUMFERENCE = 40075.017d; // km at equator
    private static final int ONGOING_NOTIFICATION_ID = 0xdeadbeef;

    private final String TAG = NearbyNotificationService.class.getSimpleName();
    private List<Bahnhof> nearStations;
    private LatLng myPos = new LatLng(50d, 8d);


    // Parameters for requests to the Location Api.
    private long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 60000; // ms

    private float SHORTEST_UPDATE_DISTANCE = 500; // m

    private boolean started;// we have only one notification
    private NearbyBahnhofNotificationManager notifiedStationManager;
    private GoogleApiClient googleApiClient = null;

    public NearbyNotificationService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "About to create");
        super.onCreate();
        nearStations = new ArrayList<Bahnhof>(0); // no markers until we know where we are
        notifiedStationManager = null;
        started = false;

        // Create an instance of GoogleAPIClient.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            Log.i(TAG, "Received start command");
            // connect google services
            googleApiClient.connect();
            // show a permanent notification to indicate that position detection is running
            Notification ongoingNotification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_logotrain)
                    .setContentTitle(getString(R.string.nearby_notification_active))
                    .setOngoing(true)
                    .setLocalOnly(true)
                    .build();
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(ONGOING_NOTIFICATION_ID, ongoingNotification);

            // set internal flag to avoid multi-starting
            started = true;
            return START_STICKY;
        } else
            return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory() {
        // stop tracking
        super.onLowMemory();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void readStations() {
        BahnhofsDbAdapter bahnhofsDbAdapter = new BahnhofsDbAdapter(this);

        try {
            bahnhofsDbAdapter.open();
            nearStations = bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos, false);
            nearStations.addAll(bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos, true));
        } catch (Exception e) {
            Log.e(TAG, "Datenbank konnte nicht geöffnet werden", e);
        } finally {
            bahnhofsDbAdapter.close();
            ;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service gets destroyed");
        try {
            cancelNotification();
            stopLocationUpdates();
        } catch (Throwable t) {
            Log.wtf(TAG, "Unknown problem when trying to de-register from GPS updates", t);
        }

        googleApiClient.disconnect();

        // Cancel the ongoing notification
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);


        started = false;
        super.onDestroy();
    }

    private void cancelNotification() {
        if (notifiedStationManager != null) {
            notifiedStationManager.destroy();
            notifiedStationManager = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Received new location: " + location);
        try {
            myPos = new LatLng(location.getLatitude(), location.getLongitude());

            // check if currently advertised station is still in range
            if (notifiedStationManager != null) {
                if (calcDistance(notifiedStationManager.getStation()) > MIN_NOTIFICATION_DISTANCE) {
                    cancelNotification();
                }
            }
            Log.d(TAG, "Reading matching stations from local database");
            readStations();

            // check if a user notification is appropriate
            checkNearestStation();
        } catch (Throwable t) {
            Log.e(TAG, "Unknown Problem arised during location change handling", t);
        }
    }

    private void checkNearestStation() {
        double minDist = 3e3;
        Bahnhof nearest = null;
        for (Bahnhof bahnhof : nearStations) {
            double dist = calcDistance(bahnhof);
            if (dist < minDist) {
                nearest = bahnhof;
                minDist = dist;
            }
        }
        if (nearest != null && minDist < MIN_NOTIFICATION_DISTANCE) {
            notifyNearest(nearest, minDist);
            Log.i(TAG, "Issued notification to user");
        } else {
            Log.d(TAG, "No notification - nearest station was " + minDist + " km away: " + nearest);
        }
    }


    /**
     * Start notification build process. This might run asynchronous in case that required
     * photos need to be fetched fist. If the notification is built up, #onNotificationReady(Notification)
     * will be called.
     *
     * @param nearest the station nearest to the current position
     * @param distance the distance of the station to the current position
     */
    private void notifyNearest(Bahnhof nearest, double distance) {
        if (notifiedStationManager != null) {
            if (notifiedStationManager.getStation().equals(nearest)) {
                return; // Notification für diesen Bahnhof schon angezeigt
            } else {
                notifiedStationManager.destroy();
                notifiedStationManager = null;
            }
        }
        notifiedStationManager = NearbyBahnhofNotificationManagerFactory.create(this, nearest, distance);
        notifiedStationManager.notifyUser();
    }


    /**
     * Calculate the distance between the given station and our current position (myLatitude, myLongitude)
     * @param bahnhof the station to calculate the distance to
     * @return the distance
     */
    private double calcDistance(Bahnhof bahnhof) {
        // Wir nähern für glatte Oberflächen, denn wir sind an Abständen kleiner 1km interessiert
        double lateralDiff = myPos.latitude - bahnhof.getLat();
        double longDiff = (Math.abs(myPos.latitude) < 89.99d) ?
                (myPos.longitude - bahnhof.getLon()) * Math.cos(myPos.latitude / 180 * Math.PI) :
                0.0d; // at the poles, longitude doesn't matter
        // simple Pythagoras now.
        double distance = Math.sqrt(Math.pow(lateralDiff, 2.0d) + Math.pow(longDiff, 2.0d)) * EARTH_CIRCUMFERENCE / 360.0d;
        return distance;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest()
                .setInterval (180000)
                .setFastestInterval (FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        new AsyncTask<PendingResult<LocationSettingsResult>, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(PendingResult<LocationSettingsResult>... pendingResults) {
                com.google.android.gms.common.api.Status status = pendingResults[0].await().getStatus();
                if (status.getStatusCode() != LocationSettingsStatusCodes.SUCCESS) {
                    Log.e(TAG, "Device settings unsuitable for location");
                    Toast.makeText(NearbyNotificationService.this, "Einstellungen erlauben keine Ortung", Toast.LENGTH_LONG).show();
                    stopSelf();
                }
                return status.getStatusCode() != LocationSettingsStatusCodes.RESOLUTION_REQUIRED;
            }
        }.execute(result);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
        } catch (SecurityException se) {
            Log.e(TAG, "Still no permission for location services");
            Toast.makeText(this, "Bitte einmal \"in der Nähe\" aufrufen", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    /**
     * Called by Google Play when the client has connected.
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            startLocationUpdates();
        } catch (Throwable t) {
            Log.wtf(TAG, "Unknown problem when trying to register for GPS updates", t);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permission for location service");
            Toast.makeText(this, "Bitte einmal \"in der Nähe\" aufrufen", Toast.LENGTH_LONG).show();
            return;
        }
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (lastLocation != null) {
            onLocationChanged(lastLocation);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Google Play Service connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Google Play Service connection failed: " + connectionResult.getErrorMessage());
        stopSelf();
    }
}
