package de.bahnhoefe.deutschlands.bahnhofsfotos.notification;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BahnhofsFotoFetchTask;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler;

public class NearbyBahnhofWithPhotoNotificationManager extends NearbyBahnhofNotificationManager implements BitmapAvailableHandler {

    private static final long[] VIBRATION_PATTERN = new long[]{300};
    private static final int LED_COLOR = 0x00ff0000;
    private BahnhofsFotoFetchTask fetchTask;

    public NearbyBahnhofWithPhotoNotificationManager(Context context, Bahnhof bahnhof, double distance) {
        super(context, bahnhof, distance);
        Log.d(TAG, "Creating " + getClass().getSimpleName());
    }

    /**
     * Build a notification for a station with Photo
     */
    @Override
    public void notifyUser() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 640;
        fetchTask = new BahnhofsFotoFetchTask(this, options);
        fetchTask.execute(notificationStation.getId());
    }


    /**
     * This gets called if the requested bitmap is available. Finish and issue the notification.
     *
     * @param bitmap the fetched Bitmap for the notification. May be null
     */
    @Override
    public void onBitmapAvailable(@Nullable Bitmap bitmap) {
        if (context == null)
            return; // we're already destroyed
        if (bitmap == null)
            bitmap = getBitmapFromResource(R.drawable.ic_stations_with_photo);

        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        bigPictureStyle.bigPicture(bitmap).setBigContentTitle(null).setSummaryText(fetchTask.getLicense());

        Notification fullImagePage = new NotificationCompat.Builder (context)
                .setStyle(bigPictureStyle)
                .extend(new NotificationCompat.WearableExtender()
                        .setHintShowBackgroundOnly(true)
                        .setHintScreenTimeout(NotificationCompat.WearableExtender.SCREEN_TIMEOUT_LONG))
                .build();

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true)
                        .setBackground(bitmap)
                        .addPage(fullImagePage);

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
            int h = 400;
            int w = 400;
            vectorDrawable.setBounds(0, 0, w, h);
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            canvas.drawColor(Color.WHITE);
            vectorDrawable.draw(canvas);
            return bm;
        } else
            return null;
    }



}