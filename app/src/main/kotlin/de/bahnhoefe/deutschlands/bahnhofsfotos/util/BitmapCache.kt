package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.graphics.Bitmap
import android.util.Log
import java.net.MalformedURLException
import java.net.URL
import java.util.function.Consumer

/**
 * A cache for station photos.
 */
class BitmapCache private constructor() {

    private val requests: MutableMap<URL, MutableCollection<BitmapAvailableHandler>> =
        mutableMapOf()
    private val cache: MutableMap<URL, Bitmap> = mutableMapOf()

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param resourceUrl the URL to fetch
     * @param callback    the BitmapAvailableHandler to call on completion.
     */
    fun getPhoto(resourceUrl: String, callback: BitmapAvailableHandler) {
        try {
            getPhoto(URL(resourceUrl), callback)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Couldn't load photo from malformed URL $resourceUrl")
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
    private fun getPhoto(resourceUrl: URL, callback: BitmapAvailableHandler) {
        val bitmap = cache[resourceUrl]
        if (bitmap == null) {
            val downloader = BitmapDownloader(resourceUrl) { fetchedBitmap: Bitmap? ->
                if (fetchedBitmap != null) {
                    cache[resourceUrl] = fetchedBitmap
                }

                // inform all requesters about the available image
                synchronized(requests) {
                    val handlers: Collection<BitmapAvailableHandler>? = requests.remove(resourceUrl)
                    handlers?.forEach(Consumer { handler: BitmapAvailableHandler ->
                        handler.onBitmapAvailable(
                            fetchedBitmap
                        )
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

    companion object {

        private val TAG = BitmapCache::class.java.simpleName

        /**
         * The singleton.
         */
        var instance: BitmapCache? = null
            /**
             * Get the BitmapCache instance.
             *
             * @return the single instance of BitmapCache.
             */
            get() {
                synchronized(BitmapCache::class.java) {
                    if (field == null) {
                        field = BitmapCache()
                    }
                    return field
                }
            }
            private set
    }
}