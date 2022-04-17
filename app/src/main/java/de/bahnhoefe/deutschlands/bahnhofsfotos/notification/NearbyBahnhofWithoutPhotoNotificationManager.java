package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;

public class NearbyBahnhofWithoutPhotoNotificationManager extends NearbyBahnhofNotificationManager {

    private static final long[] VIBRATION_PATTERN = new long[]{300, 100, 300, 100, 300};
    public static final int LED_COLOR = 0x0000ffff;
    private static final int REQUEST_FOTO = 0x100;

    public NearbyBahnhofWithoutPhotoNotificationManager(final Context context, final Station station, final double distance, final Set<Country> countries) {
        super(context, station, distance, countries);
        Log.d(TAG, "Creating " + getClass().getSimpleName());
    }

    // helpers that create notification elements that are common to "with foto" and "without foto"
    private PendingIntent getFotoPendingIntent() {
        // Build an intent for an action to take a picture
        // actually this launches DetailsActivity with a specific Extra that causes it to launch
        // Photo immediately.
        final var detailFotoIntent = getDetailIntent();
        detailFotoIntent.putExtra(DetailsActivity.EXTRA_TAKE_FOTO, true);
        return pendifyMe(detailFotoIntent, REQUEST_FOTO);
    }


    /**
     * Build a notification for a station without photo. Call onNotificationReady if done.
     */
    @Override
    public void notifyUser() {
        final var notificationBuilder = getBasicNotificationBuilder();

        final var fotoPendingIntent = getFotoPendingIntent();

        notificationBuilder
                .addAction(R.drawable.ic_photo_camera_white_48px, context.getString(R.string.photo), fotoPendingIntent)
                .setVibrate(VIBRATION_PATTERN)
                .setColor(LED_COLOR);

        onNotificationReady(notificationBuilder.build());
    }

}