package de.bahnhoefe.deutschlands.bahnhofsfotos.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil

class NearbyBahnhofWithPhotoNotificationManager(
    context: Context,
    station: Station,
    distance: Double,
    countries: Set<Country?>?
) : NearbyBahnhofNotificationManager(context, station, distance, countries),
    BitmapAvailableHandler {
    init {
        Log.d(TAG, "Creating " + javaClass.simpleName)
    }

    /**
     * Build a notification for a station with Photo
     */
    override fun notifyUser() {
        if (ConnectionUtil.checkInternetConnection(context!!)) {
            BitmapCache.Companion.getInstance().getPhoto(this, notificationStation.photoUrl)
        }
    }

    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     */
    override fun onBitmapAvailable(bitmap: Bitmap?) {
        var bitmap = bitmap
        if (context == null) {
            return  // we're already destroyed
        }
        if (bitmap == null) {
            bitmap = getBitmapFromResource(R.drawable.ic_stations_with_photo)
        }
        val bigPictureStyle = NotificationCompat.BigPictureStyle()
        if (bitmap != null) {
            bigPictureStyle.bigPicture(bitmap).setBigContentTitle(null)
                .setSummaryText(notificationStation.license)
        }
        val notificationBuilder = basicNotificationBuilder
            .setStyle(bigPictureStyle)
            .extend(NotificationCompat.WearableExtender())
            .setVibrate(VIBRATION_PATTERN)
            .setColor(LED_COLOR)
        onNotificationReady(notificationBuilder.build())
    }

    /**
     * Construct a Bitmap object from a given drawable resource ID.
     *
     * @param id the resource ID denoting a drawable resource
     * @return the Bitmap. May be null.
     */
    private fun getBitmapFromResource(id: Int): Bitmap {
        val vectorDrawable = AppCompatResources.getDrawable(context!!, id)!!
        vectorDrawable.setBounds(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT)
        val bm = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        canvas.drawColor(Color.WHITE)
        vectorDrawable.draw(canvas)
        return bm
    }

    companion object {
        private val VIBRATION_PATTERN = longArrayOf(300)
        private const val LED_COLOR = 0x00ff0000
        const val BITMAP_HEIGHT = 400
        const val BITMAP_WIDTH = 400
    }
}