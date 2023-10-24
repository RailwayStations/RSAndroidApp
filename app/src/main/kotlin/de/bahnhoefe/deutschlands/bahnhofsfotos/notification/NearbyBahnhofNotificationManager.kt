package de.bahnhoefe.deutschlands.bahnhofsfotos.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.UploadActivity
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country.Companion.getCountryByCode
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable

abstract class NearbyBahnhofNotificationManager(
    context: Context,
    station: Station,
    distance: Double,
    countries: Set<Country>
) {
    private val countries: Set<Country>

    /**
     * The Bahnhof about which a notification is being built.
     */
    var station: Station
        protected set

    /**
     * The distance of the Bahnhof about which a notification is being built.
     */
    protected val notificationDistance: Double

    /**
     * The Android Context for which the notification is generated.
     */
    protected var context: Context

    /**
     * Constructor. After construction, you need to call notifyUser for action to happen.
     */
    init {
        this.context = context
        notificationDistance = distance
        this.station = station
        this.countries = countries
    }

    /**
     * Build a notification for a station with Photo. The start command.
     */
    abstract fun notifyUser()

    /**
     * Called back once the notification was built up ready.
     */
    protected fun onNotificationReady(notification: Notification?) {
        // Get an instance of the NotificationManager service
        val notificationManager = NotificationManagerCompat.from(
            context
        )

        // Build the notification and issues it with notification manager.
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(NOTIFICATION_ID, notification!!)
    }

    protected val basicNotificationBuilder: NotificationCompat.Builder
        /**
         * Helper method that configures a NotificationBuilder wtih the elements common to both
         * notification types.
         */
        get() {
            // Build an intent for an action to see station details
            val detailPendingIntent = detailPendingIntent
            // Build an intent to see the station on a map
            val mapPendingIntent = mapPendingIntent
            // Build an intent to view the station's timetable
            val countryByCode = getCountryByCode(countries, station.country)
            val timetablePendingIntent = countryByCode?.let { country: Country ->
                getTimetablePendingIntent(
                    country,
                    station
                )
            }
            createChannel(context)

            // Texts and bigStyle
            val textCreator = TextCreator().invoke()
            val shortText = textCreator.shortText
            val bigStyle = textCreator.bigStyle
            var builder = NotificationCompat.Builder(
                context, CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(
                    R.drawable.ic_directions_white_24dp,
                    context.getString(R.string.label_map), mapPendingIntent
                )
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            if (timetablePendingIntent != null) {
                builder.addAction(
                    R.drawable.ic_timetable,
                    context.getString(R.string.label_timetable),
                    timetablePendingIntent
                )
            }
            builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(
                    R.drawable.ic_directions_white_24dp,
                    context.getString(R.string.label_map), mapPendingIntent
                )
                .addAction(
                    R.drawable.ic_timetable,
                    context.getString(R.string.label_timetable),
                    timetablePendingIntent
                )
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            return builder
        }

    protected fun pendifyMe(intent: Intent?, requestCode: Int): PendingIntent {
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addNextIntent(intent)
        try {
            stackBuilder.addNextIntentWithParentStack(intent) // syntesize a back stack from the parent information in manifest
        } catch (iae: IllegalArgumentException) {
            // unfortunately, this occurs if the supplied intent is not handled by our app
            // in this case, just add the the intent...
            stackBuilder.addNextIntent(intent)
        }
        return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    protected val uploadActivity: Intent
        get() {
            // Build an intent for an action to see station details
            val detailIntent = Intent(context, UploadActivity::class.java)
            detailIntent.putExtra(UploadActivity.EXTRA_STATION, station)
            return detailIntent
        }
    private val detailPendingIntent: PendingIntent
        get() = pendifyMe(uploadActivity, REQUEST_DETAIL)
    private val mapPendingIntent: PendingIntent
        /**
         * Build an intent for an action to view a map.
         *
         * @return the PendingIntent built.
         */
        get() {
            val mapIntent = Intent(Intent.ACTION_VIEW)
            mapIntent.data = Uri.parse("geo:" + station.lat + "," + station.lon)
            return pendifyMe(mapIntent, REQUEST_MAP)
        }

    /**
     * Build an intent for an action to view a timetable for the station.
     *
     * @return the PendingIntent built.
     */
    private fun getTimetablePendingIntent(country: Country, station: Station?): PendingIntent? {
        val timetableIntent = Timetable().createTimetableIntent(country, station)
        return if (timetableIntent != null) {
            pendifyMe(timetableIntent, REQUEST_TIMETABLE)
        } else null
    }

    fun destroy() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    protected inner class TextCreator {
        var shortText: String? = null
            private set
        var bigStyle: NotificationCompat.BigTextStyle? = null
            private set

        operator fun invoke(): TextCreator {
            shortText = context.getString(
                R.string.template_short_text,
                station.title,
                notificationDistance
            )
            val longText = context.getString(
                R.string.template_long_text,
                station.title,
                notificationDistance,
                if (station.hasPhoto()) context.getString(R.string.photo_exists) else ""
            )
            bigStyle = NotificationCompat.BigTextStyle()
            bigStyle!!.bigText(longText)
            return this
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_MAP = 0x10
        private const val REQUEST_DETAIL = 0x20
        private const val REQUEST_TIMETABLE = 0x30
        const val CHANNEL_ID = "bahnhoefe_channel_01" // The id of the channel.
        fun createChannel(context: Context?) {
            val notificationManager =
                context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, context.getString(
                        R.string.channel_name
                    ), NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }
}