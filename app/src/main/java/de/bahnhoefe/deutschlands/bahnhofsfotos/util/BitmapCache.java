package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.Bitmap;

import java.util.HashMap;

/**
 * Created by pelzi on 10.09.16.
 */
public class BitmapCache extends HashMap<Integer, Bitmap> {
    private static BitmapCache instanz = null;
    public static BitmapCache getInstance() {
        synchronized (BitmapCache.class) {
            if (instanz == null)
                instanz = new BitmapCache(10);
            return instanz;
        }
    }

    private BitmapCache(int capacity) {
        super(capacity);
    }
}
