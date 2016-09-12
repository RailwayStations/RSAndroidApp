package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.content.Context;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

/**
 * Created by pelzi on 11.09.16.
 */
public class NearbyBahnhofNotificationManagerFactory {
    /**
     * Construct the appropriate subclass of NearbyBahnhofNotificationManager for the given parameters.
     * @param context the Android Context to construct for
     * @param bahnhof the Bahnhof that is going to be shown to the user
     * @param distance the distance of the station from current position of the user
     * @return an instance of NearbyBahnhofNotificationManager
     */
    static public NearbyBahnhofNotificationManager create (Context context, Bahnhof bahnhof, double distance) {
        if (bahnhof.getPhotoflag() == null)
            return new NearbyBahnhofWithoutPhotoNotificationManager(context, bahnhof, distance);
        else
            return new NearbyBahnhofWithPhotoNotificationManager(context, bahnhof, distance);
    }
}
