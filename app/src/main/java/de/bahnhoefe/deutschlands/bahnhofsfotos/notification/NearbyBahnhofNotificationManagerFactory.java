package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.content.Context;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;

public class NearbyBahnhofNotificationManagerFactory {

    /**
     * Construct the appropriate subclass of NearbyBahnhofNotificationManager for the given parameters.
     *
     * @param context  the Android Context to construct for
     * @param bahnhof  the Bahnhof that is going to be shown to the user
     * @param distance the distance of the station from current position of the user
     * @return an instance of NearbyBahnhofNotificationManager
     */
    static public NearbyBahnhofNotificationManager create(Context context, Bahnhof bahnhof, double distance, Set<Country> countries) {
        if (bahnhof.hasPhoto()) {
            return new NearbyBahnhofWithPhotoNotificationManager(context, bahnhof, distance, countries);
        } else {
            return new NearbyBahnhofWithoutPhotoNotificationManager(context, bahnhof, distance, countries);
        }
    }
}
