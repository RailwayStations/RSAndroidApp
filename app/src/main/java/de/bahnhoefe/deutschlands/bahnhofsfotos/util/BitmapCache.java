package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

/**
 * Created by pelzi on 10.09.16.
 */

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * A cache for station fotos.
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
            if (instance == null)
                instance = new BitmapCache(10);
            return instance;
        }
    }

    private BitmapCache(int capacity) {
        cache = new HashMap<>(capacity);
        requests = new HashMap<>(3);
    }

    private final HashMap<URL, Collection<BitmapAvailableHandler>> requests;

    private final HashMap<URL, Bitmap> cache;

    private class Request implements BitmapAvailableHandler {
        private final URL url;

        public Request(URL url) {
            this.url = url;
        }

        /**
         * This gets called if the requested bitmap is available. Write Bitmap to cache, find the requestor and pass on the result.
         *
         * @param bitmap the fetched Bitmap for the notification. May be null
         */
        @Override
        public void onBitmapAvailable(@Nullable Bitmap bitmap) {
            // save the image in the cache if it was constructed
            if (bitmap != null)
                cache.put(url, bitmap);

            // inform all requestors about the available image
            synchronized (requests) {
                Collection<BitmapAvailableHandler> handlers = requests.remove(url);
                if (handlers == null) {
                    Log.wtf(TAG, "Request result without a saved requestor. This should never happen.");
                } else {
                    for (BitmapAvailableHandler handler : handlers) {
                        handler.onBitmapAvailable(bitmap);
                    }
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
    public void getFoto(BitmapAvailableHandler callback, @NonNull String resourceUrl) {
        try {
            getFoto(callback, new URL(resourceUrl));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Couldn't load photo from malformed URL " + resourceUrl);
            callback.onBitmapAvailable(null);
        }
    }

    /**
     * Get a picture for the given URL, either from cache or by downloading.
     * The fetching happens asynchronously. When finished, the provided callback interface is called.
     *
     * @param callback    the BitmapAvailableHandler to call on completion.
     * @param resourceUrl the URL to fetch
     */
    public void getFoto(BitmapAvailableHandler callback, @NonNull URL resourceUrl) {
        Bitmap bitmap = cache.get(resourceUrl);
        if (bitmap == null) {
            BitmapDownloader downloader = new BitmapDownloader(
                    new Request(resourceUrl),
                    resourceUrl);
            synchronized (requests) {
                Collection<BitmapAvailableHandler> handlers = requests.get(resourceUrl);
                if (handlers == null) {
                    handlers = new ArrayList<>();
                    handlers.add(callback);
                    requests.put(resourceUrl, handlers);
                } else {
                    handlers.add(callback);
                }
            }
            downloader.execute();
        } else {
            callback.onBitmapAvailable(bitmap);
        }

    }

}
