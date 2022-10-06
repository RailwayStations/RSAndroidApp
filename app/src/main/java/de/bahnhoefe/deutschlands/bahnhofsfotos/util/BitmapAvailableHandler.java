package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.net.URL;

/**
 * Created by pelzi on 11.09.16.
 */
public interface BitmapAvailableHandler {
    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     * @param url    the url of the fetched Bitmap
     */
    void onBitmapAvailable(@Nullable Bitmap bitmap, URL url);
}
