package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManager;
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManagerFactory;

public class NearbyNotificationService extends Service implements LocationListener {

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1000; // 1km

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 10000; // 10 seconds

    private static final double MIN_NOTIFICATION_DISTANCE = 1.0d; // km
    private static final double EARTH_CIRCUMFERENCE = 40075.017d; // km at equator
    private static final int ONGOING_NOTIFICATION_ID = 0xdeadbeef;

    private final String TAG = NearbyNotificationService.class.getSimpleName();
    private List<Bahnhof> nearStations;
    private Location myPos = new Location((String)null);

    private LocationManager locationManager;

    private NearbyBahnhofNotificationManager notifiedStationManager;
    private BaseApplication baseApplication = null;
    private BahnhofsDbAdapter bahnhofsDbAdapter = null;

    /**
     * The intent action to use to bind to this service's status interface.
     */
    public static final String STATUS_INTERFACE = NearbyNotificationService.class.getPackage().getName() + ".Status";

    public NearbyNotificationService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "About to create");
        super.onCreate();
        myPos.setLatitude(50d);
        myPos.setLongitude(8d);
        nearStations = new ArrayList<>(0); // no markers until we know where we are
        notifiedStationManager = null;
        baseApplication = (BaseApplication)getApplication();
        bahnhofsDbAdapter = baseApplication.getDbAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        cancelNotification();

        Log.i(TAG, "Received start command");

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NearbyBahnhofNotificationManager.createChannel(this);

        // show a permanent notification to indicate that position detection is running
        final Notification ongoingNotification = new NotificationCompat.Builder(this, NearbyBahnhofNotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.nearby_notification_active))
                .setOngoing(true)
                .setLocalOnly(true)
                .setContentIntent(resultPendingIntent)
                .build();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(ONGOING_NOTIFICATION_ID, ongoingNotification);

        registerLocationManager();

        return START_STICKY;
    }

    @Override
    public void onLowMemory() {
        // stop tracking
        super.onLowMemory();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service gets destroyed");
        try {
            cancelNotification();
            unregisterLocationManager();
        } catch (Throwable t) {
            Log.wtf(TAG, "Unknown problem when trying to de-register from GPS updates", t);
        }

        // Cancel the ongoing notification
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);

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
            myPos = location;

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

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

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
     * @param nearest  the station nearest to the current position
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
        notifiedStationManager = NearbyBahnhofNotificationManagerFactory.create(this, nearest, distance, bahnhofsDbAdapter.fetchCountriesByCountryCodes(baseApplication.getCountryCodes()));
        notifiedStationManager.notifyUser();
    }

    /**
     * Calculate the distance between the given station and our current position (myLatitude, myLongitude)
     *
     * @param bahnhof the station to calculate the distance to
     * @return the distance
     */
    private double calcDistance(Bahnhof bahnhof) {
        // Wir nähern für glatte Oberflächen, denn wir sind an Abständen kleiner 1km interessiert
        double lateralDiff = myPos.getLatitude() - bahnhof.getLat();
        double longDiff = (Math.abs(myPos.getLatitude()) < 89.99d) ?
                (myPos.getLongitude() - bahnhof.getLon()) * Math.cos(myPos.getLatitude() / 180 * Math.PI) :
                0.0d; // at the poles, longitude doesn't matter
        // simple Pythagoras now.
        return Math.sqrt(Math.pow(lateralDiff, 2.0d) + Math.pow(longDiff, 2.0d)) * EARTH_CIRCUMFERENCE / 360.0d;
    }

    public void registerLocationManager() {

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No Location Permission");
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
                    myPos = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
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
                        myPos = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering LocationManager", e);
            Bundle b = new Bundle();
            b.putString("error", "Error registering LocationManager: " + e.toString());
            locationManager = null;
            myPos = null;
            return;
        }
        Log.i(TAG, "LocationManager registered");
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

    /**
     * Class returned when an activity binds to this service.
     * Currently, can only be used to query the service state, i.e. if the location tracking
     * is switched off or on with photo or on without photo.
     */
    public class StatusBinder extends Binder {
    }

    /**
     * Bind to interfaces provided by this service. Currently implemented:
     * <ul>
     * <li>STATUS_INTERFACE: Returns a StatusBinder that can be used to query the tracking status</li>
     * </ul>
     *
     * @param intent an Intent giving the intended action
     * @return a Binder instance suitable for the intent supplied, or null if none matches.
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (STATUS_INTERFACE.equals(intent.getAction())) {
            return new StatusBinder();
        } else {
            return null;
        }
    }

    private void readStations() {
        try {
            Log.i(TAG, "Lade nahegelegene Bahnhoefe");
            nearStations = bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos.getLatitude(), myPos.getLongitude(), baseApplication.getPhotoFilter(), baseApplication.getNicknameFilter());
        } catch (Exception e) {
            Log.e(TAG, "Datenbank konnte nicht geöffnet werden", e);
        }
    }

}
