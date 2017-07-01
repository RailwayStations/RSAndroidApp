package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

/**
 * Created by pelzi on 11.09.16.
 */
public interface BitmapAvailableHandler {
    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     */
    void onBitmapAvailable(@Nullable Bitmap bitmap);
}
