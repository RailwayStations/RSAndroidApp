package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;

public class Timetable {

    /**
     * Build an intent for an action to view a timetable for the station.
     *
     * @return the PendingIntent built.
     */
    @Nullable
    public Intent createTimetableIntent(final Country country, final Station station) {
        final Intent timetableIntent = new Intent(Intent.ACTION_VIEW);

        if (!country.hasTimetableUrlTemplate()) {
            return null;
        }

        String timeTableTemplate = country.getTimetableUrlTemplate();

        timeTableTemplate = timeTableTemplate.replace("{id}", station.getId());
        timeTableTemplate = timeTableTemplate.replace("{title}", station.getTitle());
        timeTableTemplate = timeTableTemplate.replace("{DS100}", StringUtils.trimToEmpty(station.getDs100()));

        timetableIntent.setData(Uri.parse(timeTableTemplate));
        return timetableIntent;
    }
}