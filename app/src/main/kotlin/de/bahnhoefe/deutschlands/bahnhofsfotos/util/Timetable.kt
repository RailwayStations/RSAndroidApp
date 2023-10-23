package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.content.Intent
import android.net.Uri
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import org.apache.commons.lang3.StringUtils

class Timetable {
    /**
     * Build an intent for an action to view a timetable for the station.
     *
     * @return the PendingIntent built.
     */
    fun createTimetableIntent(country: Country, station: Station?): Intent? {
        if (!country.hasTimetableUrlTemplate()) {
            return null
        }
        var timeTableTemplate = country.timetableUrlTemplate
        timeTableTemplate = timeTableTemplate!!.replace("{id}", station!!.id!!)
        timeTableTemplate = timeTableTemplate.replace("{title}", station.title!!)
        timeTableTemplate = timeTableTemplate.replace(
            "{DS100}", StringUtils.trimToEmpty(
                station.ds100
            )
        )
        val timetableIntent = Intent(Intent.ACTION_VIEW)
        timetableIntent.data = Uri.parse(timeTableTemplate)
        return timetableIntent
    }
}