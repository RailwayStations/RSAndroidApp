package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

/**
 * Created by pelzi on 11.09.16.
 */
public abstract class NearbyBahnhofNotificationManager {
    private static final int NOTIFICATION_ID = 1;
    protected final String TAG = NearbyBahnhofNotificationManager.class.getSimpleName();

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
        // mapIntent is common to both variants
        // Build an intent for an action to see station details
        Intent detailIntent = getDetailIntent();
        PendingIntent detailPendingIntent =
                PendingIntent.getActivity(context, 0, detailIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent mapPendingIntent = getMapPendingIntent();

        // Texts and bigStyle
        TextCreator textCreator = new TextCreator().invoke();
        String shortText = textCreator.getShortText();
        NotificationCompat.BigTextStyle bigStyle = textCreator.getBigStyle();

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_logotrain_found)
                .setContentTitle(context.getString(R.string.station_is_near))
                .setContentText(shortText)
                .setContentIntent(detailPendingIntent)
                .addAction(R.drawable.ic_directions_white_24dp,
                        "Karte", mapPendingIntent)
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
    }


    @NonNull
    protected Intent getDetailIntent() {
        // Build an intent for an action to see station details
        Intent detailIntent = new Intent(context, DetailsActivity.class);
        detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF_NAME, notificationStation.getTitle());
        detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF_NUMBER, notificationStation.getId());
        detailIntent.putExtra(DetailsActivity.EXTRA_POSITION, new LatLng(notificationStation.getLat(), notificationStation.getLon()));
        return detailIntent;
    }

    protected PendingIntent getMapPendingIntent() {
        // Build an intent for an action to view a map
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        Uri geoUri = Uri.parse("geo:" + notificationStation.getLat() + "," + notificationStation.getLon());
        mapIntent.setData(geoUri);
        return PendingIntent.getActivity(context, 0, mapIntent, 0);
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
                    notificationStation.getLat(),
                    notificationStation.getLon(),
                    new Date(notificationStation.getDatum()),
                    (notificationStation.getPhotoflag() != null ?
                            context.getString(R.string.photo_exists) :
                            ""));

            bigStyle = new NotificationCompat.BigTextStyle();
            bigStyle.bigText(longText);
            return this;
        }
    }
}
