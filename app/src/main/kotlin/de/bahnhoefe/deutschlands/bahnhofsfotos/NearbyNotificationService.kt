package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManager
import de.bahnhoefe.deutschlands.bahnhofsfotos.notification.NearbyBahnhofNotificationManagerFactory
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

@AndroidEntryPoint
class NearbyNotificationService : Service(), LocationListener {

    private var nearStations = listOf<Station>()
    private var myPos: Location? = Location(null as String?)
    private var locationManager: LocationManager? = null
    private var notifiedStationManager: NearbyBahnhofNotificationManager? = null

    @Inject
    lateinit var preferencesService: PreferencesService

    @Inject
    lateinit var dbAdapter: DbAdapter

    override fun onCreate() {
        super.onCreate()
        myPos!!.latitude = 50.0
        myPos!!.longitude = 8.0
        notifiedStationManager = null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        cancelNotification()
        Log.i(TAG, "Received start command")
        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NearbyBahnhofNotificationManager.createChannel(this)

        // show a permanent notification to indicate that position detection is running
        val ongoingNotification: Notification =
            NotificationCompat.Builder(this, NearbyBahnhofNotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.nearby_notification_active))
                .setOngoing(true)
                .setLocalOnly(true)
                .setContentIntent(resultPendingIntent)
                .build()
        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return START_STICKY
        }
        notificationManager.notify(ONGOING_NOTIFICATION_ID, ongoingNotification)
        registerLocationManager()
        return START_STICKY
    }

    override fun onLowMemory() {
        // stop tracking
        super.onLowMemory()
        stopSelf()
    }

    override fun onDestroy() {
        Log.i(TAG, "Service gets destroyed")
        try {
            cancelNotification()
            unregisterLocationManager()
        } catch (t: Throwable) {
            Log.wtf(TAG, "Unknown problem when trying to de-register from GPS updates", t)
        }

        // Cancel the ongoing notification
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun cancelNotification() {
        if (notifiedStationManager != null) {
            notifiedStationManager!!.destroy()
            notifiedStationManager = null
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.i(TAG, "Received new location: $location")
        try {
            myPos = location

            // check if currently advertised station is still in range
            if (notifiedStationManager != null) {
                if (calcDistance(notifiedStationManager!!.station) > MIN_NOTIFICATION_DISTANCE) {
                    cancelNotification()
                }
            }
            Log.d(TAG, "Reading matching stations from local database")
            readStations()

            // check if a user notification is appropriate
            checkNearestStation()
        } catch (t: Throwable) {
            Log.e(TAG, "Unknown Problem arised during location change handling", t)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    private fun checkNearestStation() {
        var minDist = 3e3
        var nearest: Station? = null
        for (station in nearStations) {
            val dist = calcDistance(station)
            if (dist < minDist) {
                nearest = station
                minDist = dist
            }
        }
        if (nearest != null && minDist < MIN_NOTIFICATION_DISTANCE) {
            notifyNearest(nearest, minDist)
            Log.i(TAG, "Issued notification to user")
        } else {
            Log.d(TAG, "No notification - nearest station was $minDist km away: $nearest")
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
    private fun notifyNearest(nearest: Station, distance: Double) {
        if (notifiedStationManager != null) {
            notifiedStationManager = if (nearest == notifiedStationManager!!.station) {
                return  // Notification für diesen Bahnhof schon angezeigt
            } else {
                notifiedStationManager!!.destroy()
                null
            }
        }
        notifiedStationManager = NearbyBahnhofNotificationManagerFactory.create(
            this,
            nearest,
            distance,
            dbAdapter.fetchCountriesWithProviderApps(preferencesService.countryCodes)
        )
        notifiedStationManager!!.notifyUser()
    }

    /**
     * Calculate the distance between the given station and our current position (myLatitude, myLongitude)
     *
     * @param station the station to calculate the distance to
     * @return the distance
     */
    private fun calcDistance(station: Station): Double {
        // Wir nähern für glatte Oberflächen, denn wir sind an Abständen kleiner 1km interessiert
        val lateralDiff = myPos!!.latitude - station.lat
        val longDiff =
            if (abs(myPos!!.latitude) < 89.99) (myPos!!.longitude - station.lon) * cos(
                myPos!!.latitude / 180 * Math.PI
            ) else 0.0 // at the poles, longitude doesn't matter
        // simple Pythagoras now.
        return sqrt(
            lateralDiff.pow(2.0) + longDiff.pow(2.0)
        ) * EARTH_CIRCUMFERENCE / 360.0
    }

    private fun registerLocationManager() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "No Location Permission")
                return
            }
            locationManager =
                applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

            // getting GPS status
            val isGPSEnabled = locationManager!!
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                )
                Log.d(TAG, "GPS Enabled")
                if (locationManager != null) {
                    myPos = locationManager!!
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
            } else {
                // getting network status
                val isNetworkEnabled = locationManager!!
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                    )
                    Log.d(TAG, "Network Location enabled")
                    if (locationManager != null) {
                        myPos = locationManager!!
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering LocationManager", e)
            val b = Bundle()
            b.putString("error", "Error registering LocationManager: $e")
            locationManager = null
            myPos = null
            return
        }
        Log.i(TAG, "LocationManager registered")
    }

    private fun unregisterLocationManager() {
        if (locationManager != null) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager!!.removeUpdates(this)
            }
            locationManager = null
        }
        Log.i(TAG, "LocationManager unregistered")
    }

    /**
     * Class returned when an activity binds to this service.
     * Currently, can only be used to query the service state, i.e. if the location tracking
     * is switched off or on with photo or on without photo.
     */
    class StatusBinder : Binder()

    /**
     * Bind to interfaces provided by this service. Currently implemented:
     *
     *  * STATUS_INTERFACE: Returns a StatusBinder that can be used to query the tracking status
     *
     *
     * @param intent an Intent giving the intended action
     * @return a Binder instance suitable for the intent supplied, or null if none matches.
     */
    override fun onBind(intent: Intent): IBinder? {
        return if (STATUS_INTERFACE == intent.action) {
            StatusBinder()
        } else {
            null
        }
    }

    private fun readStations() {
        try {
            Log.i(TAG, "Lade nahegelegene Bahnhoefe")
            nearStations = dbAdapter.getStationByLatLngRectangle(
                myPos!!.latitude,
                myPos!!.longitude,
                preferencesService.stationFilter
            )
        } catch (e: Exception) {
            Log.e(TAG, "Datenbank konnte nicht geöffnet werden", e)
        }
    }

    companion object {
        private val TAG = NearbyNotificationService::class.java.simpleName

        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 1000 // 1km

        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES: Long = 10000 // 10 seconds
        private const val MIN_NOTIFICATION_DISTANCE = 1.0 // km
        private const val EARTH_CIRCUMFERENCE = 40075.017 // km at equator
        private const val ONGOING_NOTIFICATION_ID = -0x21524111

        /**
         * The intent action to use to bind to this service's status interface.
         */
        val STATUS_INTERFACE =
            (NearbyNotificationService::class.java.getPackage()?.name ?: "") + ".Status"
    }
}