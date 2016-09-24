package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by android_oma on 09.09.16.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle data payload of FCM messages.
        Log.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        Log.d(TAG, "FCM Notification Message: " + remoteMessage.getNotification());
        Log.d(TAG, "FCM Data Message: " + remoteMessage.getData());
    }
}
