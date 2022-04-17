package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
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
    public static final int BITMAP_HEIGHT = 400;
    public static final int BITMAP_WIDTH = 400;

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

        final var bigPictureStyle = new NotificationCompat.BigPictureStyle();
        if (bitmap != null) {
            bigPictureStyle.bigPicture(bitmap).setBigContentTitle(null).setSummaryText(notificationStation.getLicense());
        }

        final var notificationBuilder = getBasicNotificationBuilder()
                .setStyle(bigPictureStyle)
                .extend(new NotificationCompat.WearableExtender())
                .setVibrate(VIBRATION_PATTERN)
                .setColor(LED_COLOR);

        onNotificationReady(notificationBuilder.build());
    }

    /**
     * Construct a Bitmap object from a given drawable resource ID.
     *
     * @param id the resource ID denoting a drawable resource
     * @return the Bitmap. May be null.
     */
    private Bitmap getBitmapFromResource(final int id) {
        final var vectorDrawable = AppCompatResources.getDrawable(context, id);
        assert vectorDrawable != null;
        vectorDrawable.setBounds(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT);
        final var bm = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        final var canvas = new Canvas(bm);
        canvas.drawColor(Color.WHITE);
        vectorDrawable.draw(canvas);
        return bm;
    }

}