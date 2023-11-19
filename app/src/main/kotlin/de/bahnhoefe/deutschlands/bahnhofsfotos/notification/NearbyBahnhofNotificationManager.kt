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
    val context: Context,
    val station: Station,
    val distance: Double,
    val countries: Set<Country>
) {
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

    /**
     * Helper method that configures a NotificationBuilder with the elements common to both
     * notification types.
     */
    protected fun createBasicNotificationBuilder(): NotificationCompat.Builder {
        val detailPendingIntent = createDetailPendingIntent()
        val mapPendingIntent = createMapPendingIntent()
        val timetablePendingIntent =
            getCountryByCode(countries, station.country)?.let { country: Country ->
                createTimetablePendingIntent(
                    country,
                    station
                )
            }
        createChannel(context)

        val textCreator = TextCreator().invoke()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logotrain_found)
            .setContentTitle(context.getString(R.string.station_is_near))
            .setContentText(textCreator.shortText)
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
            .setStyle(textCreator.bigStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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

    /**
     * Build an intent for an action to see station details
     */
    protected fun createUploadActivity(): Intent {
        val detailIntent = Intent(context, UploadActivity::class.java)
        detailIntent.putExtra(UploadActivity.EXTRA_STATION, station)
        return detailIntent
    }

    /**
     * Build an intent for an action to see station details
     */
    private fun createDetailPendingIntent(): PendingIntent {
        return pendifyMe(createUploadActivity(), REQUEST_DETAIL)
    }

    /**
     * Build an intent for an action to view a map.
     */
    private fun createMapPendingIntent(): PendingIntent {
        val mapIntent = Intent(Intent.ACTION_VIEW)
        mapIntent.data = Uri.parse("geo:" + station.lat + "," + station.lon)
        return pendifyMe(mapIntent, REQUEST_MAP)
    }

    /**
     * Build an intent for an action to view a timetable for the station.
     */
    private fun createTimetablePendingIntent(country: Country, station: Station?): PendingIntent? {
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
                distance
            )
            val longText = context.getString(
                R.string.template_long_text,
                station.title,
                distance,
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