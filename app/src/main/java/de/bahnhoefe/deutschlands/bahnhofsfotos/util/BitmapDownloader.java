package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class to download image in background
 * @Author pelzvieh
 */
public class BitmapDownloader extends AsyncTask<Void, Void, Bitmap> {
    private final String TAG = BitmapDownloader.class.getSimpleName();
    private final BitmapAvailableHandler bitmapAvailableHandler;

    private URL url;
    private BitmapFactory.Options options;

    /**
     * Construct a bitmap Downloader for the given URL
     * @param handler the BitmapAvailableHandler instance that is called on completion
     * @param url the URL to fetch the Bitmap from
     */
    public BitmapDownloader(BitmapAvailableHandler handler, URL url) {
        this.bitmapAvailableHandler = handler;
        this.url = url;
        this.options = options;
    }

    /**
     * Asynchronous fetching from a URL and construction of an image from the resource.
     * @return the constructed Bitmap or null if downloading or construction failed
     */
    @Override
    protected Bitmap doInBackground(Void... voids) {
        // Fetch the station photo
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            Log.i(TAG, "Feting Bitmap from URL: " + url);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            is = httpConnection.getInputStream();
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String contentType = httpConnection.getContentType();
                if (contentType != null && !contentType.startsWith("image"))
                    Log.w(TAG, "Supplied URL does not appear to be an image resource (type=" + contentType+ ")");
                bitmap = BitmapFactory.decodeStream(is, null, options);
            } else {
                Log.e(TAG, "Error downloading photo: "+ httpConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not download photo");
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close channel to photo download");
                }
            }
        }
        return bitmap;
    }

    /**
     * Run the callback function when Bitmap creation is finished
     * @param bitmap the downloaded Bitmap, may be null.
     */
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        bitmapAvailableHandler.onBitmapAvailable(bitmap);
    }
}
