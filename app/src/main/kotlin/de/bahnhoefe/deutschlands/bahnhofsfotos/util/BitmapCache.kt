package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

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
    public void getPhoto(BitmapAvailableHandler callback, @NonNull URL resourceUrl) {
        var bitmap = cache.get(resourceUrl);
        if (bitmap == null) {
            var downloader = new BitmapDownloader((bitmap1) -> {
                if (bitmap1 != null) {
                    cache.put(resourceUrl, bitmap1);
                }

                // inform all requestors about the available image
                synchronized (requests) {
                    var handlers = requests.remove(resourceUrl);
                    if (handlers == null) {
                        Log.wtf(TAG, "Request result without a saved requestor. This should never happen.");
                    } else {
                        handlers.forEach(handler -> handler.onBitmapAvailable(bitmap1));
                    }
                }
            }, resourceUrl);
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
            callback.onBitmapAvailable(bitmap);
        }

    }

}
