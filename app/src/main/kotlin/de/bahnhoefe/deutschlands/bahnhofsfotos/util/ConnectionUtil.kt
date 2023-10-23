package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class ConnectionUtil {

    public static boolean checkInternetConnection(Context context) {
        var cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        var activeNetwork = cm.getActiveNetworkInfo();
        var isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
        }

        return isConnected;
    }

}
