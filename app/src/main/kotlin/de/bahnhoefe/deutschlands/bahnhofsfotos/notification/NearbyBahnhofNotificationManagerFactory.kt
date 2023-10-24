package de.bahnhoefe.deutschlands.bahnhofsfotos.notification

import android.content.Context
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station

object NearbyBahnhofNotificationManagerFactory {
    /**
     * Construct the appropriate subclass of NearbyBahnhofNotificationManager for the given parameters.
     *
     * @param context  the Android Context to construct for
     * @param station  the Bahnhof that is going to be shown to the user
     * @param distance the distance of the station from current position of the user
     * @return an instance of NearbyBahnhofNotificationManager
     */
    fun create(
        context: Context,
        station: Station,
        distance: Double,
        countries: Set<Country>
    ): NearbyBahnhofNotificationManager {
        return if (station.hasPhoto()) {
            NearbyBahnhofWithPhotoNotificationManager(context, station, distance, countries)
        } else {
            NearbyBahnhofWithoutPhotoNotificationManager(context, station, distance, countries)
        }
    }
}