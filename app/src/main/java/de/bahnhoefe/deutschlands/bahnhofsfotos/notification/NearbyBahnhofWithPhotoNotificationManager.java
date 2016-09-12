package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;

public class NearbyBahnhofWithPhotoNotificationManager extends NearbyBahnhofNotificationManager implements BitmapAvailableHandler {

    private static final long[] VIBRATION_PATTERN = new long[]{300};
    private static final int LED_COLOR = 0x00ff0000;

    public NearbyBahnhofWithPhotoNotificationManager(Context context, Bahnhof bahnhof, double distance) {
        super(context, bahnhof, distance);
        Log.d(TAG, "Creating " + getClass().getSimpleName());
    }

    /**
     * Build a notification for a station with Photo
     */
    @Override
    public void notifyUser() {
        String template = "http://www.deutschlands-bahnhoefe.de/images/%s.jpg";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 640;
        try {
            URL url = new URL(String.format(template, notificationStation.getId()));
            // fetch bitmap asynchronously, call onBitmapAvailable if ready
            BitmapCache.getInstance().getFoto(this, url, options);
        } catch (MalformedURLException e) {
            Log.wtf(TAG, "URL not well formed", e);
        }
    }


    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     */
    @Override
    public void onBitmapAvailable(@Nullable Bitmap bitmap) {
        if (bitmap == null)
            bitmap = getBitmapFromResource(R.drawable.ic_broken_image);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true)
                        .setBackground(bitmap);

        NotificationCompat.Builder notificationBuilder = getBasicNotificationBuilder();

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
    @Nullable
    private Bitmap getBitmapFromResource(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = context.getDrawable(id);
            int h = 640;
            int w = 400;
            vectorDrawable.setBounds(0, 0, w, h);
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            vectorDrawable.draw(canvas);
            return bm;
        } else
            return null;
    }



}