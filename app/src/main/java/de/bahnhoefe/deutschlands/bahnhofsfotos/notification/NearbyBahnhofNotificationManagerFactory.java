package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.content.Context;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

public class NearbyBahnhofNotificationManagerFactory {
    /**
     * Construct the appropriate subclass of NearbyBahnhofNotificationManager for the given parameters.
     *
     * @param context  the Android Context to construct for
     * @param bahnhof  the Bahnhof that is going to be shown to the user
     * @param distance the distance of the station from current position of the user
     * @return an instance of NearbyBahnhofNotificationManager
     */
    static public NearbyBahnhofNotificationManager create(Context context, Bahnhof bahnhof, double distance, BahnhofsDbAdapter dbAdapter) {
        if (bahnhof.getPhotoflag() == null)
            return new NearbyBahnhofWithoutPhotoNotificationManager(context, bahnhof, distance, dbAdapter);
        else
            return new NearbyBahnhofWithPhotoNotificationManager(context, bahnhof, distance, dbAdapter);
    }
}
