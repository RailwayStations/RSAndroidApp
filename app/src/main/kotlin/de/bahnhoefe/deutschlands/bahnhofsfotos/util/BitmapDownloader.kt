package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class to download image in background
 */
class BitmapDownloader
/**
 * Construct a bitmap Downloader for the given URL
 *
 * @param handler the BitmapAvailableHandler instance that is called on completion
 * @param url     the URL to fetch the Bitmap from
 */(private val bitmapAvailableHandler: BitmapAvailableHandler, private val url: URL) : Thread() {
    private val TAG = BitmapDownloader::class.java.simpleName
    override fun run() {
        var bitmap: Bitmap? = null
        try {
            Log.i(TAG, "Fetching Bitmap from URL: $url")
            val httpConnection = url.openConnection() as HttpURLConnection
            httpConnection.inputStream.use { `is` ->
                if (httpConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    val contentType = httpConnection.contentType
                    if (contentType != null && !contentType.startsWith("image")) {
                        Log.w(
                            TAG,
                            "Supplied URL does not appear to be an image resource (type=$contentType)"
                        )
                    }
                    bitmap = BitmapFactory.decodeStream(`is`)
                } else {
                    Log.e(TAG, "Error downloading photo: " + httpConnection.responseCode)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Could not download photo")
            bitmap = null
        }
        bitmapAvailableHandler.onBitmapAvailable(bitmap)
    }
}