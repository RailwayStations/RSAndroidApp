package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;

public class NearbyBahnhofWithoutPhotoNotificationManager extends NearbyBahnhofNotificationManager {

    private static final long[] VIBRATION_PATTERN = new long[]{300, 100, 300, 100, 300};
    public static final int LED_COLOR = 0x0000ffff;
    private static final int REQUEST_FOTO = 0x100;

    public NearbyBahnhofWithoutPhotoNotificationManager(Context context, Bahnhof bahnhof, double distance, Country country) {
        super(context, bahnhof, distance, country);
        Log.d(TAG, "Creating " + getClass().getSimpleName());
    }

    // helpers that create notification elements that are common to "with foto" and "without foto"
    private PendingIntent getFotoPendingIntent() {
        // Build an intent for an action to take a picture
        // actually this launches DetailsActivity with a specific Extra that causes it to launch
        // Photo immediately.
        Intent detailFotoIntent = getDetailIntent();
        detailFotoIntent.putExtra(DetailsActivity.EXTRA_TAKE_FOTO, true);
        return pendifyMe(detailFotoIntent, REQUEST_FOTO);
    }


    /**
     * Build a notification for a station without photo. Call onNotificationReady if done.
     */
    @Override
    public void notifyUser() {
        NotificationCompat.Builder notificationBuilder = getBasicNotificationBuilder();

        PendingIntent fotoPendingIntent = getFotoPendingIntent();

        notificationBuilder
                .addAction(R.drawable.ic_photo_camera_white_48px, "Foto", fotoPendingIntent)
                .setVibrate(VIBRATION_PATTERN)
                .setColor(LED_COLOR)
        ;

        onNotificationReady(notificationBuilder.build());
    }

}