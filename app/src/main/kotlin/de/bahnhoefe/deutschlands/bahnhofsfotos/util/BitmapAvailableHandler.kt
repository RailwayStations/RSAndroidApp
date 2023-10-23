package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.graphics.Bitmap

/**
 * Callback for BitmapDownloader
 */
interface BitmapAvailableHandler {
    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     */
    fun onBitmapAvailable(bitmap: Bitmap?)
}