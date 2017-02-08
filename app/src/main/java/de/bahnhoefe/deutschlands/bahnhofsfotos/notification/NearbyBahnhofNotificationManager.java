package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.Map;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

/**
 * Created by pelzi on 11.09.16.
 */
public abstract class NearbyBahnhofNotificationManager {
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_MAP = 0x10;
    private static final int REQUEST_DETAIL = 0x20;
    private static final int REQUEST_TIMETABLE = 0x30;
    private static final int REQUEST_STATION = 0x40;
    protected final String TAG = NearbyBahnhofNotificationManager.class.getSimpleName();
    // @todo Diese Map aus den zentralen Stammdaten unter https://railway-stations.org/laenderdaten.json laden, sobald die URL-Templates dort drin sind
    private static Map<String, String> TIMETABLE_LINK_TEMPLATES;
    {
        TIMETABLE_LINK_TEMPLATES = new HashMap<String, String>(2);
        TIMETABLE_LINK_TEMPLATES.put("DE", "https://mobile.bahn.de/bin/mobil/bhftafel.exe/dox?bt=dep&max=10&rt=1&use_realtime_filter=1&start=yes&input=%s");
        TIMETABLE_LINK_TEMPLATES.put("CH", "http://fahrplan.sbb.ch/bin/stboard.exe/dn?input=%s&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes");
    }

    private final String countryShortCode;
    private final String DB_BAHNHOF_LIVE_PKG = "de.deutschebahn.bahnhoflive";
    private final String DB_BAHNHOF_LIVE_CLASS = "de.deutschebahn.bahnhoflive.MeinBahnhofActivity";

    /**
     * The Bahnhof about which a notification is being built.
     */
    protected Bahnhof notificationStation;

    /**
     * The distance of the Bahnhof about which a notification is being built.
     */
    protected double notificationDistance;

    /**
     * The Android Context for which the notification is generated.
     */
    protected Context context;

    /**
     * Constructor. After construction, you need to call notifyUser for action to happen.
     * @param context the Android Context from which this object ist created.
     * @param bahnhof the station to issue a notification for.
     * @param distance a double giving the distance from current location to bahnhof (in km)
     */
    public NearbyBahnhofNotificationManager(@NonNull Context context, @NonNull Bahnhof bahnhof, double distance) {
        this.context = context;
        notificationDistance = distance;
        this.notificationStation = bahnhof;
        // Read the configured country code
        // @todo remove once an international solution is found for timetabling
        SharedPreferences sharedPreferences = context.getSharedPreferences("APP_PREF_FILE",Context.MODE_PRIVATE);
        countryShortCode = sharedPreferences.getString("APP_PREF_COUNTRY","DE");

    }

    /**
     * Build a notification for a station with Photo. The start command.
     */
    public abstract void notifyUser();

    /**
     * Called back once the notification was built up ready.
     *
     * @param notification
     */
    protected void onNotificationReady(Notification notification) {
        if (context == null)
            return; // we're already destroyed

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Helper method that configures a NotificationBuilder wtih the elements common to both
     * notification types.
     * @return
     */
    protected NotificationCompat.Builder getBasicNotificationBuilder() {
        // Build an intent for an action to see station details
        PendingIntent detailPendingIntent = getDetailPendingIntent();
        // Build an intent to see the station on a map
        PendingIntent mapPendingIntent = getMapPendingIntent();
        // Build an intent to view the station's timetable
        PendingIntent timetablePendingIntent = getTimetablePendingIntent();
        // Build an intent to launch the DB Bahnhöfe Live app
        PendingIntent stationPendingIntent = getStationPendingIntent();

        // Texts and bigStyle
        TextCreator textCreator = new TextCreator().invoke();
        String shortText = textCreator.getShortText();
        NotificationCompat.BigTextStyle bigStyle = textCreator.getBigStyle();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(R.drawable.ic_directions_white_24dp,
                        context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_map), mapPendingIntent)
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (timetablePendingIntent != null) {
            builder.addAction(R.drawable.ic_timetable,
                    context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_timetable),
                    timetablePendingIntent);
        }
        // @todo aktivieren, sobald ein wirklich funktionierende Intent gefunden ist
            /*builder.addAction(R.drawable.ic_timetable,
                    "Bahnhofsinfos",
                    stationPendingIntent);*/

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(R.drawable.ic_directions_white_24dp,
                        context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_map), mapPendingIntent)
                .addAction(R.drawable.ic_timetable, context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_timetable), timetablePendingIntent)
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
    }

    protected PendingIntent pendifyMe(Intent intent, int requestCode) {
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @NonNull
    protected Intent getDetailIntent() {
        // Build an intent for an action to see station details
        Intent detailIntent = new Intent(context, DetailsActivity.class);
        detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF, notificationStation);
        return detailIntent;
    }

    @NonNull
    protected PendingIntent getDetailPendingIntent() {
        return pendifyMe(getDetailIntent(), REQUEST_DETAIL);
    }

    /**
     * Build an intent for an action to view a map.
     * @return the PendingIntent built.
     */
    protected PendingIntent getMapPendingIntent() {
        //
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        Uri geoUri = Uri.parse("geo:" + notificationStation.getLat() + "," + notificationStation.getLon());
        mapIntent.setData(geoUri);
        return pendifyMe(mapIntent, REQUEST_MAP);
    }

    /**
     * Build an intent for an action to view a timetable for the station.
     * @return the PendingIntent built.
     */
    protected @Nullable PendingIntent getTimetablePendingIntent() {
        //
        final Intent timetableIntent = new Intent(Intent.ACTION_VIEW);
        final String timeTableTemplate = TIMETABLE_LINK_TEMPLATES.get(countryShortCode);
        if (timeTableTemplate == null)
            return null;
        Uri timeTableUri = Uri.parse(
                String.format(timeTableTemplate, Uri.encode(notificationStation.getTitle()))
        );
        timetableIntent.setData(timeTableUri);
        return pendifyMe(timetableIntent, REQUEST_TIMETABLE);
    }

    /**
     * Build an intent for an action to view a map.
     * @return the PendingIntent built.
     */

    @NonNull
    protected PendingIntent getStationPendingIntent() {
        // Build an intent for an action to see station details
        Intent stationIntent = new Intent().setClassName(DB_BAHNHOF_LIVE_PKG, DB_BAHNHOF_LIVE_CLASS);
        stationIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF, notificationStation);
        return pendifyMe(stationIntent, REQUEST_STATION);
    }



    public void destroy() {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
        notificationStation = null;
        context = null;
    }

    public Bahnhof getStation() {
        return notificationStation;
    }

    protected class TextCreator {
        private String shortText;
        private NotificationCompat.BigTextStyle bigStyle;
        private String longText;

        public String getShortText() {
            return shortText;
        }

        public NotificationCompat.BigTextStyle getBigStyle() {
            return bigStyle;
        }

        public TextCreator invoke() {
            String shortTextTemplate = context.getString(R.string.template_short_text);
            String longTextTemplate = context.getString(R.string.template_long_text);

            shortText = String.format(shortTextTemplate, notificationStation.getTitle(), notificationDistance);
            longText = String.format(longTextTemplate,
                    notificationStation.getTitle(),
                    notificationDistance,
                    (notificationStation.getPhotoflag() != null ?
                            context.getString(R.string.photo_exists) :
                            ""));

            bigStyle = new NotificationCompat.BigTextStyle();
            bigStyle.bigText(longText);
            return this;
        }
    }
}
