package de.bahnhoefe.deutschlands.bahnhofsfotos.notification

import android.content.Context
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station

object NearbyBahnhofNotificationManagerFactory {

    /**
     * Construct the appropriate subclass of NearbyBahnhofNotificationManager for the given parameters.
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