package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

/**
 * Created by android_oma on 09.09.16.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";
    private static final String FRIENDLY_ENGAGE_TOPIC = "friendly_engage";
    //private static final String FRIENDLY_ENGAGE_TOPIC = "for_tests";
    private RequestQueue queue;
    private TokenObject tokenObject;

    /**
     * The Application's current Instance ID token is no longer valid and thus a new one must be requested.
     */
    @Override
    public void onTokenRefresh() {
        // If you need to handle the generation of a token, initially or after a refresh this is
        // where you should do that.
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "FCM Token: " + token);

        // Once a token is generated, we subscribe to topic.
        FirebaseMessaging.getInstance().subscribeToTopic(FRIENDLY_ENGAGE_TOPIC);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.PREF_FILE),MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.FRIENDLY_ENGAGE_TOPIC),true);
        editor.apply();

       // sendTheRegisteredTokenToWebServer(token);
        }

  /*  private void sendTheRegisteredTokenToWebServer(final String token){
        queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringPostRequest = new StringRequest(Request.Method.POST, Helper.PATH_TO_SERVER_TOKEN_STORAGE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                tokenObject = gson.fromJson(response, TokenObject.class);
                if (null == tokenObject) {
                    Toast.makeText(getApplicationContext(), "Can't send a token to the server", Toast.LENGTH_LONG).show();
                    //mySharedPreference.saveNotificationSubscription(false);
                } else {
                    Toast.makeText(getApplicationContext(), "Token successfully send to server", Toast.LENGTH_LONG).show();

                    //mySharedPreference.saveNotificationSubscription(true);
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put(Helper.TOKEN_TO_SERVER, token);
                return params;
            }
        };
        queue.add(stringPostRequest);
    }*/
    }

