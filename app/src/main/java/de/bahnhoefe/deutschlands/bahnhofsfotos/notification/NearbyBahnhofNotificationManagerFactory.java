package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.content.Context;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;

public class NearbyBahnhofNotificationManagerFactory {

    /**
     * Construct the appropriate subclass of NearbyBahnhofNotificationManager for the given parameters.
     *
     * @param context  the Android Context to construct for
     * @param station  the Bahnhof that is going to be shown to the user
     * @param distance the distance of the station from current position of the user
     * @return an instance of NearbyBahnhofNotificationManager
     */
    static public NearbyBahnhofNotificationManager create(Context context, Station station, double distance, Set<Country> countries) {
        if (station.hasPhoto()) {
            return new NearbyBahnhofWithPhotoNotificationManager(context, station, distance, countries);
        } else {
            return new NearbyBahnhofWithoutPhotoNotificationManager(context, station, distance, countries);
        }
    }
}
