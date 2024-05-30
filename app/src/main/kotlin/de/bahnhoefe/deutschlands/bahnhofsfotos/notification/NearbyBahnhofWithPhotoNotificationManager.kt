package de.bahnhoefe.deutschlands.bahnhofsfotos.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil

private val VIBRATION_PATTERN = longArrayOf(300)
private const val LED_COLOR = 0x00ff0000
private const val BITMAP_HEIGHT = 400
private const val BITMAP_WIDTH = 400

class NearbyBahnhofWithPhotoNotificationManager(
    context: Context,
    station: Station,
    distance: Double,
    countries: Set<Country>
) : NearbyBahnhofNotificationManager(context, station, distance, countries),
    BitmapAvailableHandler {

    /**
     * Build a notification for a station with Photo
     */
    override fun notifyUser() {
        if (ConnectionUtil.checkInternetConnection(context)) {
            station.photoUrl?.let { BitmapCache.getPhoto(it, this) }
        }
    }

    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param fetchedBitmap the fetched Bitmap for the notification. May be null
     */
    override fun onBitmapAvailable(fetchedBitmap: Bitmap?) {
        var bitmap = fetchedBitmap
        if (bitmap == null) {
            bitmap = getStationWithPhotoBitmapFromResource()
        }
        val bigPictureStyle = NotificationCompat.BigPictureStyle()
        bigPictureStyle.bigPicture(bitmap).setBigContentTitle(null)
            .setSummaryText(station.license)
        val notificationBuilder = createBasicNotificationBuilder()
            .setStyle(bigPictureStyle)
            .extend(NotificationCompat.WearableExtender())
            .setVibrate(VIBRATION_PATTERN)
            .setColor(LED_COLOR)
        onNotificationReady(notificationBuilder.build())
    }

    private fun getStationWithPhotoBitmapFromResource(): Bitmap {
        val vectorDrawable =
            AppCompatResources.getDrawable(context, R.drawable.ic_stations_with_photo)!!
        vectorDrawable.setBounds(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT)
        val bm = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        canvas.drawColor(Color.WHITE)
        vectorDrawable.draw(canvas)
        return bm
    }

}