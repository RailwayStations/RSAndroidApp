package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.graphics.Bitmap
import android.util.Log
import java.net.MalformedURLException
import java.net.URI
import java.util.function.Consumer

private val TAG = BitmapCache::class.java.simpleName

/**
 * A cache for station photos.
 */
object BitmapCache {

    private val requests = mutableMapOf<URI, MutableCollection<BitmapAvailableHandler>>()
    private val cache = mutableMapOf<URI, Bitmap>()

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param resourceUri the URI to fetch
     * @param callback    the BitmapAvailableHandler to call on completion.
     */
    fun getPhoto(resourceUri: String, callback: BitmapAvailableHandler) {
        try {
            getPhoto(URI.create(resourceUri), callback)
        } catch (_: MalformedURLException) {
            Log.e(TAG, "Couldn't load photo from malformed URL $resourceUri")
            callback.onBitmapAvailable(null)
        }
    }

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param resourceUrl the URL to fetch
     * @param callback    the BitmapAvailableHandler to call on completion.
     */
    private fun getPhoto(resourceUrl: URI, callback: BitmapAvailableHandler) {
        val bitmap = cache[resourceUrl]
        if (bitmap == null) {
            val downloader = BitmapDownloader(resourceUrl) { fetchedBitmap: Bitmap? ->
                if (fetchedBitmap != null) {
                    cache[resourceUrl] = fetchedBitmap
                }

                // inform all requesters about the available image
                synchronized(requests) {
                    requests.remove(resourceUrl)
                        ?.forEach(Consumer { handler ->
                            handler.onBitmapAvailable(fetchedBitmap)
                        })
                        ?: Log.e(
                            TAG,
                            "Request result without a saved requester. This should never happen."
                        )
                }
            }
            synchronized(requests) {
                var handlers = requests[resourceUrl]
                if (handlers == null) {
                    handlers = ArrayList()
                    handlers.add(callback)
                    requests.put(resourceUrl, handlers)
                } else {
                    handlers.add(callback)
                }
            }
            downloader.start()
        } else {
            callback.onBitmapAvailable(bitmap)
        }
    }
}
