package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManagerFactory;

import static android.location.LocationManager.GPS_PROVIDER;

public class NearbyNotificationService extends Service implements LocationListener {

    // some useful constants
    private static final double MIN_NOTIFICATION_DISTANCE = 1.0d; // km
    private static final double EARTH_CIRCUMFERENCE = 40075.017d; // km at equator

    private final String TAG = NearbyNotificationService.class.getSimpleName();
    private List<Bahnhof> nearStations;
    private LatLng myPos = new LatLng(50d, 8d);


    // Parameters for requests to the Location Api.
    private long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 60000; // ms

    private float SHORTEST_UPDATE_DISTANCE = 500; // m

    private boolean started;// we have only one notification

    public NearbyNotificationService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "About to create");
        super.onCreate();
        nearStations = new ArrayList<Bahnhof>(0); // no markers until we know where we are
        started = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            Log.i(TAG, "Received start command");
            try {
                startLocationUpdates();
            } catch (Throwable t) {
                Log.wtf(TAG, "Unknown problem when trying to register for GPS updates", t);
            }
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
        started = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // todo should we use a bound interface for stopping the action or a broadcast?
        // todo MapsActivity could perhaps bind to this service and we can remove the tracking aspect from there
        return null;
    }

    private void readStations() {
        BahnhofsDbAdapter bahnhofsDbAdapter = new BahnhofsDbAdapter(this);

        try {
            bahnhofsDbAdapter.open();
            nearStations = bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos.latitude, myPos.longitude, false);
            nearStations.addAll(bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos.latitude, myPos.longitude, true));
        } catch (Exception e) {
            Log.e (TAG,"Datenbank konnte nicht geöffnet werden", e);
        } finally {
            bahnhofsDbAdapter.close();;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service gets destroyed");
        try {
            stopLocationUpdates();
        } catch (Throwable t) {
            Log.wtf(TAG, "Unknown problem when trying to de-register from GPS updates", t);
        }
        started = false;
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Received new location: " + location);
        try {
            myPos = new LatLng(location.getLatitude(), location.getLongitude());

            Log.d(TAG, "Reading matching stations from local database");
            readStations();

            // check if a user notification is appropriate
            checkNearestStation();
        } catch (Throwable t) {
            Log.e (TAG, "Unknown Problem arised during location change handling", t);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        if (LocationManager.GPS_PROVIDER.equals(s)) {
            Log.d(TAG, "GPS status change:" + i);
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        // not interesting, it will just start to work
        Log.i(TAG, "Location provider enabled: " + s);
    }

    @Override
    public void onProviderDisabled(String s) {
        if (LocationManager.GPS_PROVIDER.equals(s)) {
            Log.w(TAG, "GPS provider is disabled, no notifications will happen");
        }
    }

    private void checkNearestStation() {
        double minDist = 3e3;
        Bahnhof nearest = null;
        for (Bahnhof bahnhof: nearStations) {
            double dist = calcDistance (bahnhof);
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
        NearbyBahnhofNotificationManager manager =
                NearbyBahnhofNotificationManagerFactory.create(this, nearest, distance);
        manager.notifyUser();
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
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(GPS_PROVIDER,
                    FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,
                    SHORTEST_UPDATE_DISTANCE,
                    this);
        } catch (SecurityException se) {
            Log.w(TAG, "Cannot register location updates", se);
            Toast.makeText(this, "Bitte einmal \"in der Nähe\" aufrufen", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException se) {
            Log.w(TAG, "Cannot deregister from locationManager", se);
        }
    }
}
