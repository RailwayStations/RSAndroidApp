package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

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
    protected double notificationDistance;

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
    public NearbyBahnhofNotificationManager(@NonNull final Context context, @NonNull final Station station, final double distance, final Set<Country> countries) {
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
     *
     * @param notification
     */
    protected void onNotificationReady(final Notification notification) {
        if (context == null) {
            return; // we're already destroyed
        }

        // Get an instance of the NotificationManager service
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Helper method that configures a NotificationBuilder wtih the elements common to both
     * notification types.
     *
     * @return
     */
    protected NotificationCompat.Builder getBasicNotificationBuilder() {
        // Build an intent for an action to see station details
        final PendingIntent detailPendingIntent = getDetailPendingIntent();
        // Build an intent to see the station on a map
        final PendingIntent mapPendingIntent = getMapPendingIntent();
        // Build an intent to view the station's timetable
        final PendingIntent timetablePendingIntent = getTimetablePendingIntent(Country.getCountryByCode(countries, notificationStation.getCountry()), notificationStation);
        // Build an intent to launch the DB Bahnhöfe Live app
        final PendingIntent stationPendingIntent = getStationPendingIntent();

        createChannel(context);

        // Texts and bigStyle
        final TextCreator textCreator = new TextCreator().invoke();
        final String shortText = textCreator.getShortText();
        final NotificationCompat.BigTextStyle bigStyle = textCreator.getBigStyle();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(R.drawable.ic_directions_white_24dp,
                        context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_map), mapPendingIntent)
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        if (timetablePendingIntent != null) {
            builder.addAction(R.drawable.ic_timetable,
                    context.getString(de.bahnhoefe.deutschlands.bahnhofsfotos.R.string.label_timetable),
                    timetablePendingIntent);
        }
        // @todo aktivieren, sobald ein wirklich funktionierende Intent gefunden ist
            /*builder.addAction(R.drawable.ic_timetable,
                    "Bahnhofsinfos",
                    stationPendingIntent);*/

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
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        return builder;
    }

    public static void createChannel(final Context context) {
        final CharSequence name = context.getString(R.string.channel_name);// The user-visible name of the channel.
        final NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    protected PendingIntent pendifyMe(final Intent intent, final int requestCode) {
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        try {
            stackBuilder.addNextIntentWithParentStack(intent); // syntesize a back stack from the parent information in manifest
        } catch (final IllegalArgumentException iae) {
            // unfortunately, this occurs if the supplied intent is not handled by our app
            // in this case, just add the the intent...
            stackBuilder.addNextIntent(intent);
        }
        return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @NonNull
    protected Intent getDetailIntent() {
        // Build an intent for an action to see station details
        final Intent detailIntent = new Intent(context, DetailsActivity.class);
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
        //
        final Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        final Uri geoUri = Uri.parse("geo:" + notificationStation.getLat() + "," + notificationStation.getLon());
        mapIntent.setData(geoUri);
        return pendifyMe(mapIntent, REQUEST_MAP);
    }

    /**
     * Build an intent for an action to view a timetable for the station.
     *
     * @return the PendingIntent built.
     */
    protected
    @Nullable
    PendingIntent getTimetablePendingIntent(final Country country, final Station station) {
        final Intent timetableIntent = new Timetable().createTimetableIntent(country, station);
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
        final Intent stationIntent = new Intent().setClassName(DB_BAHNHOF_LIVE_PKG, DB_BAHNHOF_LIVE_CLASS);
        stationIntent.putExtra(DetailsActivity.EXTRA_STATION, notificationStation);
        return pendifyMe(stationIntent, REQUEST_STATION);
    }


    public void destroy() {
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
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
            final String shortTextTemplate = context.getString(R.string.template_short_text);
            final String longTextTemplate = context.getString(R.string.template_long_text);

            shortText = String.format(shortTextTemplate, notificationStation.getTitle(), notificationDistance);
            final String longText = String.format(longTextTemplate,
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
