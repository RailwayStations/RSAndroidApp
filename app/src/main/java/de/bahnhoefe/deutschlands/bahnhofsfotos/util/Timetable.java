package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import org.apache.commons.lang3.StringUtils;

public class Timetable {

    /**
     * Build an intent for an action to view a timetable for the station.
     *
     * @return the PendingIntent built.
     */
    @Nullable
    public Intent createTimetableIntent(Country country, Bahnhof station) {
        final Intent timetableIntent = new Intent(Intent.ACTION_VIEW);

        String timeTableTemplate = country.getTimetableUrlTemplate();
        if (timeTableTemplate == null || timeTableTemplate.isEmpty()) {
            return null;
        }

        timeTableTemplate = timeTableTemplate.replace("{id}", station.getId());
        timeTableTemplate = timeTableTemplate.replace("{title}", station.getTitle());
        timeTableTemplate = timeTableTemplate.replace("{DS100}", StringUtils.trimToEmpty(station.getDs100()));

        Uri timeTableUri = Uri.parse(
                String.format(timeTableTemplate, Uri.encode(station.getTitle()))
        );
        timetableIntent.setData(timeTableUri);
        return timetableIntent;
    }
}