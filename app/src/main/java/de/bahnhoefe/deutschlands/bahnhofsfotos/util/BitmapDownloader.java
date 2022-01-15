package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class to download image in background
 */
public class BitmapDownloader extends Thread {

    private final String TAG = BitmapDownloader.class.getSimpleName();
    private final BitmapAvailableHandler bitmapAvailableHandler;
    private final URL url;

    /**
     * Construct a bitmap Downloader for the given URL
     *
     * @param handler the BitmapAvailableHandler instance that is called on completion
     * @param url     the URL to fetch the Bitmap from
     */
    public BitmapDownloader(final BitmapAvailableHandler handler, final URL url) {
        super();
        this.bitmapAvailableHandler = handler;
        this.url = url;
    }

    @Override
    public void run() {
        Bitmap bitmap = null;
        try {
            Log.i(TAG, "Fetching Bitmap from URL: " + url);
            final var httpConnection = (HttpURLConnection) url.openConnection();
            try (final var is = httpConnection.getInputStream()) {
                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    final String contentType = httpConnection.getContentType();
                    if (contentType != null && !contentType.startsWith("image")) {
                        Log.w(TAG, "Supplied URL does not appear to be an image resource (type=" + contentType + ")");
                    }
                    bitmap = BitmapFactory.decodeStream(is);
                } else {
                    Log.e(TAG, "Error downloading photo: " + httpConnection.getResponseCode());
                }
            }
        } catch (final IOException e) {
            Log.e(TAG, "Could not download photo");
            bitmap = null;
        }
        bitmapAvailableHandler.onBitmapAvailable(bitmap);
    }
}
