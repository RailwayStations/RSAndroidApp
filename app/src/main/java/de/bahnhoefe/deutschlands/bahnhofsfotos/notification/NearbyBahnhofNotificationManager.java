package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable;

public abstract class NearbyBahnhofNotificationManager {
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_MAP = 0x10;
    private static final int REQUEST_DETAIL = 0x20;
    private static final int REQUEST_TIMETABLE = 0x30;
    private static final int REQUEST_STATION = 0x40;
    protected final String TAG = NearbyBahnhofNotificationManager.class.getSimpleName();

    public static final String CHANNEL_ID = "bahnhoefe_channel_01";// The id of the channel.

    private static final String DB_BAHNHOF_LIVE_PKG = "de.deutschebahn.bahnhoflive";
    private static final String DB_BAHNHOF_LIVE_CLASS = "de.deutschebahn.bahnhoflive.MeinBahnhofActivity";
    private final Set<Country> countries;

    /**
     * The Bahnhof about which a notification is being built.
     */
    protected Station notificationStation;

    /**
     * The distance of the Bahnhof about which a notification is being built.
     */
    protected final double notificationDistance;

    /**
     * The Android Context for which the notification is generated.
     */
    protected Context context;

    /**
     * Constructor. After construction, you need to call notifyUser for action to happen.
     *
     * @param context  the Android Context from which this object ist created.
     * @param station  the station to issue a notification for.
     * @param distance a double giving the distance from current location to bahnhof (in km)
     */
    public NearbyBahnhofNotificationManager(@NonNull Context context, @NonNull Station station, double distance, Set<Country> countries) {
        this.context = context;
        notificationDistance = distance;
        this.notificationStation = station;
        this.countries = countries;
    }

    /**
     * Build a notification for a station with Photo. The start command.
     */
    public abstract void notifyUser();

    /**
     * Called back once the notification was built up ready.
     */
    protected void onNotificationReady(Notification notification) {
        if (context == null) {
            return; // we're already destroyed
        }

        // Get an instance of the NotificationManager service
        var notificationManager = NotificationManagerCompat.from(context);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Helper method that configures a NotificationBuilder wtih the elements common to both
     * notification types.
     */
    protected NotificationCompat.Builder getBasicNotificationBuilder() {
        // Build an intent for an action to see station details
        var detailPendingIntent = getDetailPendingIntent();
        // Build an intent to see the station on a map
        var mapPendingIntent = getMapPendingIntent();
        // Build an intent to view the station's timetable
        var countryByCode = Country.getCountryByCode(countries, notificationStation.getCountry());
        var timetablePendingIntent = countryByCode.map(country -> getTimetablePendingIntent(country, notificationStation)).orElse(null);

        createChannel(context);

        // Texts and bigStyle
        var textCreator = new TextCreator().invoke();
        var shortText = textCreator.getShortText();
        var bigStyle = textCreator.getBigStyle();

        var builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(R.drawable.ic_directions_white_24dp,
                        context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_map), mapPendingIntent)
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (timetablePendingIntent != null) {
            builder.addAction(R.drawable.ic_timetable,
                    context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_timetable),
                    timetablePendingIntent);
        }

        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
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
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder;
    }

    public static void createChannel(Context context) {
        var notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT));
    }

    protected PendingIntent pendifyMe(Intent intent, int requestCode) {
        var stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        try {
            stackBuilder.addNextIntentWithParentStack(intent); // syntesize a back stack from the parent information in manifest
        } catch (IllegalArgumentException iae) {
            // unfortunately, this occurs if the supplied intent is not handled by our app
            // in this case, just add the the intent...
            stackBuilder.addNextIntent(intent);
        }
        return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @NonNull
    protected Intent getDetailIntent() {
        // Build an intent for an action to see station details
        var detailIntent = new Intent(context, DetailsActivity.class);
        detailIntent.putExtra(DetailsActivity.EXTRA_STATION, notificationStation);
        return detailIntent;
    }

    @NonNull
    protected PendingIntent getDetailPendingIntent() {
        return pendifyMe(getDetailIntent(), REQUEST_DETAIL);
    }

    /**
     * Build an intent for an action to view a map.
     *
     * @return the PendingIntent built.
     */
    protected PendingIntent getMapPendingIntent() {
        var mapIntent = new Intent(Intent.ACTION_VIEW);
        mapIntent.setData(Uri.parse("geo:" + notificationStation.getLat() + "," + notificationStation.getLon()));
        return pendifyMe(mapIntent, REQUEST_MAP);
    }

    /**
     * Build an intent for an action to view a timetable for the station.
     *
     * @return the PendingIntent built.
     */
    protected
    @Nullable
    PendingIntent getTimetablePendingIntent(Country country, Station station) {
        var timetableIntent = new Timetable().createTimetableIntent(country, station);
        if (timetableIntent != null) {
            return pendifyMe(timetableIntent, NearbyBahnhofNotificationManager.REQUEST_TIMETABLE);
        }
        return null;
    }

    /**
     * Build an intent for an action to view a map.
     *
     * @return the PendingIntent built.
     */
    @NonNull
    protected PendingIntent getStationPendingIntent() {
        // Build an intent for an action to see station details
        var stationIntent = new Intent().setClassName(DB_BAHNHOF_LIVE_PKG, DB_BAHNHOF_LIVE_CLASS);
        stationIntent.putExtra(DetailsActivity.EXTRA_STATION, notificationStation);
        return pendifyMe(stationIntent, REQUEST_STATION);
    }


    public void destroy() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
        notificationStation = null;
        context = null;
    }

    public Station getStation() {
        return notificationStation;
    }

    protected class TextCreator {
        private String shortText;
        private NotificationCompat.BigTextStyle bigStyle;

        public String getShortText() {
            return shortText;
        }

        public NotificationCompat.BigTextStyle getBigStyle() {
            return bigStyle;
        }

        public TextCreator invoke() {
            shortText = context.getString(R.string.template_short_text, notificationStation.getTitle(), notificationDistance);
            var longText = context.getString(R.string.template_long_text,
                    notificationStation.getTitle(),
                    notificationDistance,
                    (notificationStation.hasPhoto() ?
                            context.getString(R.string.photo_exists) :
                            ""));

            bigStyle = new NotificationCompat.BigTextStyle();
            bigStyle.bigText(longText);
            return this;
        }
    }
}
