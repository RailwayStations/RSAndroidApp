package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * A cache for station photos.
 */
public class BitmapCache {
    private final String TAG = BitmapCache.class.getSimpleName();

    /**
     * The singleton.
     */
    private static BitmapCache instance = null;

    /**
     * Get the BitmapCache instance.
     *
     * @return the single instance of BitmapCache.
     */
    public static BitmapCache getInstance() {
        synchronized (BitmapCache.class) {
            if (instance == null) {
                instance = new BitmapCache();
            }
            return instance;
        }
    }

    private BitmapCache() {
        cache = new HashMap<>(10);
        requests = new HashMap<>(3);
    }

    private final HashMap<URL, Collection<BitmapAvailableHandler>> requests;

    private final HashMap<URL, Bitmap> cache;

    private class Request implements BitmapAvailableHandler {
        /**
         * This gets called if the requested bitmap is available. Write Bitmap to cache, find the requestor and pass on the result.
         *
         * @param bitmap the fetched Bitmap for the notification. May be null
         */
        @Override
        public void onBitmapAvailable(@Nullable Bitmap bitmap, URL url) {
            // save the image in the cache if it was constructed
            if (bitmap != null) {
                cache.put(url, bitmap);
            }

            // inform all requestors about the available image
            synchronized (requests) {
                var handlers = requests.remove(url);
                if (handlers == null) {
                    Log.wtf(TAG, "Request result without a saved requestor. This should never happen.");
                } else {
                    handlers.forEach(handler -> handler.onBitmapAvailable(bitmap, url));
                }
            }
        }
    }

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param callback    the BitmapAvailableHandler to call on completion.
     * @param resourceUrl the URL to fetch
     */
    public void getPhoto(BitmapAvailableHandler callback, @NonNull String resourceUrl) {
        URL url = null;
        try {
            url = new URL(resourceUrl);
            getPhoto(callback, url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Couldn't load photo from malformed URL " + resourceUrl);
            callback.onBitmapAvailable(null, url);
        }
    }

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param callback    the BitmapAvailableHandler to call on completion.
     * @param resourceUrl the URL to fetch
     */
    public void getPhoto(BitmapAvailableHandler callback, @NonNull URL resourceUrl) {
        var bitmap = cache.get(resourceUrl);
        if (bitmap == null) {
            var downloader = new BitmapDownloader(new Request(), resourceUrl);
            synchronized (requests) {
                var handlers = requests.get(resourceUrl);
                if (handlers == null) {
                    handlers = new ArrayList<>();
                    handlers.add(callback);
                    requests.put(resourceUrl, handlers);
                } else {
                    handlers.add(callback);
                }
            }
            downloader.start();
        } else {
            callback.onBitmapAvailable(bitmap, resourceUrl);
        }

    }

}
