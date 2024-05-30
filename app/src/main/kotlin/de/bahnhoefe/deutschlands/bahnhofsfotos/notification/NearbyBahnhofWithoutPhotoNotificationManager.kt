package de.bahnhoefe.deutschlands.bahnhofsfotos.notification

import android.app.PendingIntent
import android.content.Context
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station

private val VIBRATION_PATTERN = longArrayOf(300, 100, 300, 100, 300)
private const val LED_COLOR = 0x0000ffff
private const val REQUEST_FOTO = 0x100

class NearbyBahnhofWithoutPhotoNotificationManager(
    context: Context,
    station: Station,
    distance: Double,
    countries: Set<Country>
) : NearbyBahnhofNotificationManager(context, station, distance, countries) {

    /**
     * Build an intent for an action to take a picture
     * actually this launches UploadActivity with a specific Extra that causes it to launch
     * Photo immediately.
     */
    private fun createFotoPendingIntent(): PendingIntent {
        val intent = createUploadActivity()
        return pendifyMe(intent, REQUEST_FOTO)
    }

    /**
     * Build a notification for a station without photo. Call onNotificationReady if done.
     */
    override fun notifyUser() {
        val notificationBuilder = createBasicNotificationBuilder()
        val fotoPendingIntent = createFotoPendingIntent()
        notificationBuilder
            .addAction(
                R.drawable.ic_photo_camera_white_48px,
                context.getString(R.string.photo),
                fotoPendingIntent
            )
            .setVibrate(VIBRATION_PATTERN).color = LED_COLOR
        onNotificationReady(notificationBuilder.build())
    }

}