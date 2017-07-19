package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

public class BahnhofsFotoFetchTask extends AsyncTask<Bahnhof, Void, URL> {
    private final static String TAG = BahnhofsFotoFetchTask.class.getSimpleName();
    private final BitmapAvailableHandler handler;

    public BahnhofsFotoFetchTask(BitmapAvailableHandler handler, Context context) {
        this.handler = handler;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        handler.onBitmapAvailable(null);
    }

    @Override
    protected void onPostExecute(URL url) {
        super.onPostExecute(url);
        if (url != null) {
            Log.i(TAG, "Fetching photo from " + url);
            // fetch bitmap asynchronously, call onBitmapAvailable if ready
            BitmapCache.getInstance().getFoto(handler, url);
        } else {
            handler.onBitmapAvailable(null);
        }
    }

    @Override
    protected URL doInBackground(Bahnhof... bahnhoefe) {
        Bahnhof bahnhof = bahnhoefe[0];
        Log.i(TAG, "Fetching Photo descriptor for station nr.: " + bahnhof.getId());
        try {
            return new URL(bahnhof.getPhotoUrl());
        } catch (IOException e) {
            Log.e(TAG, "Could not download descriptor", e);
        }
        return null;
    }

}
