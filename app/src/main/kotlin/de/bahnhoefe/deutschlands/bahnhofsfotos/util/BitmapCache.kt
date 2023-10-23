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
    private val TAG = BitmapCache::class.java.simpleName
    private val requests: HashMap<URL, MutableCollection<BitmapAvailableHandler>>
    private val cache: HashMap<URL, Bitmap>

    init {
        cache = HashMap(10)
        requests = HashMap(3)
    }

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param callback    the BitmapAvailableHandler to call on completion.
     * @param resourceUrl the URL to fetch
     */
    fun getPhoto(callback: BitmapAvailableHandler, resourceUrl: String) {
        var url: URL? = null
        try {
            url = URL(resourceUrl)
            getPhoto(callback, url)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Couldn't load photo from malformed URL $resourceUrl")
            callback.onBitmapAvailable(null)
        }
    }

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param callback    the BitmapAvailableHandler to call on completion.
     * @param resourceUrl the URL to fetch
     */
    fun getPhoto(callback: BitmapAvailableHandler, resourceUrl: URL) {
        val bitmap = cache[resourceUrl]
        if (bitmap == null) {
            val downloader = BitmapDownloader({ bitmap1: Bitmap? ->
                if (bitmap1 != null) {
                    cache[resourceUrl] = bitmap1
                }

                // inform all requestors about the available image
                synchronized(requests) {
                    val handlers: Collection<BitmapAvailableHandler>? = requests.remove(resourceUrl)
                    handlers?.forEach(Consumer { handler: BitmapAvailableHandler ->
                        handler.onBitmapAvailable(
                            bitmap1
                        )
                    })
                        ?: Log.wtf(
                            TAG,
                            "Request result without a saved requestor. This should never happen."
                        )
                }
            }, resourceUrl)
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