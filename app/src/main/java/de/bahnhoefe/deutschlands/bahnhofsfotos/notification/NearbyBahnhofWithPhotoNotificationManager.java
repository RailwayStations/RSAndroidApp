package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;

public class NearbyBahnhofWithPhotoNotificationManager extends NearbyBahnhofNotificationManager implements BitmapAvailableHandler {

    private static final long[] VIBRATION_PATTERN = new long[]{300};
    private static final int LED_COLOR = 0x00ff0000;

    public NearbyBahnhofWithPhotoNotificationManager(final Context context, final Station station, final double distance, final Set<Country> countries) {
        super(context, station, distance, countries);
        Log.d(TAG, "Creating " + getClass().getSimpleName());
    }

    /**
     * Build a notification for a station with Photo
     */
    @Override
    public void notifyUser() {
        if (ConnectionUtil.checkInternetConnection(context)) {
            BitmapCache.getInstance().getFoto(this, notificationStation.getPhotoUrl());
        }
    }


    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     */
    @Override
    public void onBitmapAvailable(@Nullable Bitmap bitmap) {
        if (context == null) {
            return; // we're already destroyed
        }

        if (bitmap == null) {
            bitmap = getBitmapFromResource(R.drawable.ic_stations_with_photo);
        }

        final NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        if (bitmap != null) {
            bigPictureStyle.bigPicture(bitmap).setBigContentTitle(null).setSummaryText(notificationStation.getLicense());
        }

        final Notification fullImagePage = new NotificationCompat.Builder(context)
                .setStyle(bigPictureStyle)
                .extend(new NotificationCompat.WearableExtender()
                        .setHintShowBackgroundOnly(true)
                        .setHintScreenTimeout(NotificationCompat.WearableExtender.SCREEN_TIMEOUT_LONG))
                .build();

        final NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true)
                        .setBackground(bitmap)
                        .addPage(fullImagePage);

        final NotificationCompat.Builder notificationBuilder = getBasicNotificationBuilder();

        // apply our specifics
        notificationBuilder
                .extend(wearableExtender)
                .setVibrate(VIBRATION_PATTERN)
                .setColor(LED_COLOR);

        // ...and we're done!
        onNotificationReady(notificationBuilder.build());
    }

    /**
     * Construct a Bitmap object from a given drawable resource ID.
     *
     * @param id the resource ID denoting a drawable resource
     * @return the Bitmap. May be null.
     */
    private Bitmap getBitmapFromResource(final int id) {
        final Drawable vectorDrawable = context.getDrawable(id);
        final int h = 400;
        final int w = 400;
        vectorDrawable.setBounds(0, 0, w, h);
        final Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        canvas.drawColor(Color.WHITE);
        vectorDrawable.draw(canvas);
        return bm;
    }


}