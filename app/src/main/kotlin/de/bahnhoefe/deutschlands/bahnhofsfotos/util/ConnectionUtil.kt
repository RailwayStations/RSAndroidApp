package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import de.bahnhoefe.deutschlands.bahnhofsfotos.R

object ConnectionUtil {
    fun checkInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting
        if (!isConnected) {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show()
        }
        return isConnected
    }
}