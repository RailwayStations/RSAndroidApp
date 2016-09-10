package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

import static android.location.LocationManager.GPS_PROVIDER;

public class NearbyNotificationService extends Service implements LocationListener {

    // some useful constants
    private static final double MIN_NOTIFICATION_DISTANCE = 1.0d; // km
    private static final double EARTH_CIRCUMFERENCE = 40075.017d; // km at equator
    private final String TAG = NearbyNotificationService.class.getSimpleName();
    private List<Bahnhof> nearStations;
    private LatLng myPos = new LatLng(50d, 8d);


    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=30000;

    private SharedPreferences sharedPreferences;

    private boolean started;

    /**
     * Helper class to download image in background
     */
    private class Connection extends AsyncTask<Void, Void, Notification> {
        private Bahnhof nearest;
        private double distance;
        public Connection(Bahnhof nearest, double distance) {
            this.nearest = nearest;
            this.distance = distance;
        }

        @Override
        protected Notification doInBackground(Void... voids) {
            Notification notification = nearest.getPhotoflag() == null ?
                    buildNotificationWithoutPhoto(nearest, distance) :
                    buildNotificationWithPhoto(nearest, distance);
            return notification;
        }

        @Override
        protected void onPostExecute(Notification notification) {
            int notificationId = 001;

            // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(NearbyNotificationService.this);

            // Build the notification and issues it with notification manager.
            notificationManager.notify(notificationId, notification);
        }

    }

    public NearbyNotificationService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "About to create");
        super.onCreate();
        nearStations = new ArrayList<Bahnhof>(0); // no markers until we know where we are
        started = false;
        sharedPreferences = getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            Log.i(TAG, "Received start command");
            try {
                startLocationUpdates();
            } catch (Throwable t) {
                Log.wtf(TAG, "Unknown problem when trying to register for GPS updates", t);
            }
            started = true;
            return START_STICKY;
        } else
           return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory() {
        // stop tracking
        super.onLowMemory();
        stopSelf();
        started = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // todo should we use a bound interface for stopping the action or a broadcast?
        // todo MapsActivity could perhaps bind to this service and we can remove the tracking aspect from there
        return null;
    }

    private void readStations() {
        BahnhofsDbAdapter bahnhofsDbAdapter = new BahnhofsDbAdapter(this);

        try {
            bahnhofsDbAdapter.open();
            nearStations = bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos.latitude, myPos.longitude, false);
            nearStations.addAll(bahnhofsDbAdapter.getBahnhoefeByLatLngRectangle(myPos.latitude, myPos.longitude, true));
        } catch (Exception e) {
            Log.e (TAG,"Datenbank konnte nicht geöffnet werden", e);
        } finally {
            bahnhofsDbAdapter.close();;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service gets destroyed");
        try {
            stopLocationUpdates();
        } catch (Throwable t) {
            Log.wtf(TAG, "Unknown problem when trying to de-register from GPS updates", t);
        }
        started = false;
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Received new location: " + location);
        myPos = new LatLng(location.getLatitude(), location.getLongitude());

        Log.d(TAG, "Reading matching stations from local database");
        readStations();

        // check if a user notification is appropriate
        checkNearestStation ();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        if (LocationManager.GPS_PROVIDER.equals(s)) {
            Log.d(TAG, "GPS status change:" + i);
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        // not interesting, it will just start to work
        Log.i(TAG, "Location provider enabled: " + s);
    }

    @Override
    public void onProviderDisabled(String s) {
        if (LocationManager.GPS_PROVIDER.equals(s)) {
            Log.w(TAG, "GPS provider is disabled, no notifications will happen");
        }
    }

    private void checkNearestStation() {
        double minDist = 3e3;
        Bahnhof nearest = null;
        for (Bahnhof bahnhof: nearStations) {
            double dist = calcDistance (bahnhof);
            if (dist < minDist) {
                nearest = bahnhof;
                minDist = dist;
            }
        }
        if (nearest != null && minDist < MIN_NOTIFICATION_DISTANCE) {
            notifyNearest(nearest, minDist);
            Log.i(TAG, "Issued notification to user");
        } else {
            Log.d(TAG, "No notification - nearest station was " + minDist + " km away: " + nearest);
        }
    }

    private void notifyNearest(Bahnhof nearest, double minDist) {
        // Build intent for notification content
        //Intent viewIntent = new Intent(this, ViewEventActivity.class);
        //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
        //PendingIntent viewPendingIntent =
        //        PendingIntent.getActivity(this, 0, viewIntent, 0);
        new Connection(nearest, minDist).execute();
    }


    private Notification buildNotificationWithoutPhoto(Bahnhof nearest, double minDist) {
        // Build an intent for an action to view a map
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        Uri geoUri = Uri.parse("geo:" + nearest.getLat() + "," + nearest.getLon());
        mapIntent.setData(geoUri);
        PendingIntent mapPendingIntent =
                PendingIntent.getActivity(this, 0, mapIntent, 0);

        // Build an intent for an action to see station details
        Intent detailIntent = new Intent(this, DetailsActivity.class);
        detailIntent.putExtra("bahnhofName",nearest.getTitle());
        detailIntent.putExtra("bahnhofNr", String.valueOf(nearest.getId()));
        detailIntent.putExtra("position", new LatLng(nearest.getLat(), nearest.getLon()));
        PendingIntent detailPendingIntent =
                PendingIntent.getActivity(this, 0, detailIntent, 0);

        // Build an intent for an action to take a picture
        // todo this is just fake - you need to startActivityForResult. Perhaps launch DetailsActivity.
        Intent fotoIntent = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
        Uri file = Uri.fromFile(DetailsActivity.getOutputMediaFile(String.valueOf(nearest.getId()),
                sharedPreferences.getString(getString(R.string.NICKNAME),"default")));
        fotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        PendingIntent fotoPendingIntent =
                PendingIntent.getActivity(this, 0, fotoIntent, 0);

        DecimalFormat format = new DecimalFormat ("#0.00");
        String shortText = "Bahnhof " + nearest.getTitle() + " in " + format.format(minDist) + " km";
        String longText = "Bahnhof: " + nearest.getTitle() +
                "\nEntfernung: " + format.format(minDist) +
                " km\nKoordinaten: " + format.format(nearest.getLat()) + "; " + format.format(nearest.getLon()) +
                "\nLetzte Änderung: " + SimpleDateFormat.getDateInstance().format(new Date(nearest.getDatum())) +
                (nearest.getPhotoflag() != null ? "\nPhoto: " + nearest.getPhotoflag() : "");

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(longText);

        // Create a WearableExtender to add functionality for wearables
        Bitmap bitmap = getBitmap(R.drawable.ic_logotrain);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true)
                        .setBackground(bitmap);

        NotificationCompat.Builder notificationBuilder =
               new NotificationCompat.Builder(this)
                       .setSmallIcon(R.drawable.ic_logotrain)
                       .setContentTitle("Bahnhof in der Nähe")
                       .setContentText(shortText)
                       .setContentIntent(detailPendingIntent)
                       .addAction(R.drawable.ic_directions_white_24dp,
                               "Karte", mapPendingIntent)
                       .addAction(R.drawable.ic_photo_camera_white_48px, "Foto", fotoPendingIntent)
                       .extend(wearableExtender)
                       .setStyle(bigStyle)
                       .setVibrate(new long[]{300, 100, 300, 100, 300})
                       .setColor(0x0000ffff)
                       .setPriority(NotificationCompat.PRIORITY_HIGH);
        return notificationBuilder.build();
    }


    private Notification buildNotificationWithPhoto(Bahnhof nearest, double minDist) {
        // Build an intent for an action to view a map
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        Uri geoUri = Uri.parse("geo:" + nearest.getLat() + "," + nearest.getLon());
        mapIntent.setData(geoUri);
        PendingIntent mapPendingIntent =
                PendingIntent.getActivity(this, 0, mapIntent, 0);

        // Build an intent for an action to see station details
        Intent detailIntent = new Intent(this, DetailsActivity.class);
        detailIntent.putExtra("bahnhofName",nearest.getTitle());
        detailIntent.putExtra("bahnhofNr", String.valueOf(nearest.getId()));
        detailIntent.putExtra("position", new LatLng(nearest.getLat(), nearest.getLon()));
        PendingIntent detailPendingIntent =
                PendingIntent.getActivity(this, 0, detailIntent, 0);

        DecimalFormat format = new DecimalFormat ("#0.00");
        String shortText = "Bahnhof " + nearest.getTitle() + " in " + format.format(minDist) + " km";
        String longText = "Bahnhof: " + nearest.getTitle() +
                "\nEntfernung: " + format.format(minDist) +
                " km\nKoordinaten: " + format.format(nearest.getLat()) + "; " + format.format(nearest.getLon()) +
                "\nLetzte Änderung: " + SimpleDateFormat.getDateInstance().format(new Date(nearest.getDatum())) +
                (nearest.getPhotoflag() != null ? "\nPhoto: ja" : "");

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(longText);

        // Create a WearableExtender to add functionality for wearables
        // Fetch the station photo
        Bitmap bitmap = null;
        String template = "http://www.deutschlands-bahnhoefe.de/images/%s.jpg";
        InputStream is = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.outWidth = 640;
            URL url = new URL(String.format(template, nearest.getId()));
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            is = httpConnection.getInputStream();
            if (httpConnection.getResponseCode() == 200) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            } else {
                Log.e(TAG, "Error downloading photo: "+ httpConnection.getHeaderField("Status"));
            }
        } catch (MalformedURLException e) {
            Log.wtf(TAG, "URL not well formed", e);
            bitmap = null;
        } catch (IOException e) {
            Log.e(TAG, "Could not download photo");
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close channel to photo download");
                }
            }
        }

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true)
                        .setBackground(bitmap);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_logotrain)
                        .setContentTitle("Bahnhof in der Nähe")
                        .setContentText(shortText)
                        .setContentIntent(detailPendingIntent)
                        .addAction(R.drawable.ic_directions_white_24dp,
                                "Karte", mapPendingIntent)
                        .extend(wearableExtender)
                        .setStyle(bigStyle)
                        .setVibrate(new long[]{300})
                        .setColor(0x00ff0000)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
        return notificationBuilder.build();
    }

    private Bitmap getBitmap(int id) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = getApplicationContext().getDrawable(id);
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

    /**
     * Calculate the distance between the given station and our current position (myLatitude, myLongitude)
     * @param bahnhof the station to calculate the distance to
     * @return the distance
     */
    private double calcDistance(Bahnhof bahnhof) {
        // Wir nähern für glatte Oberflächen, denn wir sind an Abständen kleiner 1km interessiert
        double lateralDiff = myPos.latitude - bahnhof.getLat();
        double longDiff = (Math.abs(myPos.latitude) < 89.99d) ?
                (myPos.longitude - bahnhof.getLon()) / Math.cos(myPos.latitude) :
                0.0d; // at the poles, longitude doesn't matter
        // simple Pythagoras now.
        double distance = Math.sqrt(Math.pow(lateralDiff, 2.0d) + Math.pow(longDiff, 2.0d)) * EARTH_CIRCUMFERENCE / 360.0d;
        return distance;
    }

    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(GPS_PROVIDER,
                    FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,
                    100.0f,
                    this);
        } catch (SecurityException se) {
            Log.w(TAG, "Cannot register location updates", se);
            Toast.makeText(this, "Bitte einmal \"in der Nähe\" aufrufen", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException se) {
            Log.w(TAG, "Cannot deregister from locationManager", se);
        }
    }
}
