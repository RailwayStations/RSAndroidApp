package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import de.bahnhoefe.deutschlands.bahnhofsfotos.R

object ConnectionUtil {
    fun checkInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val currentNetwork = connectivityManager.activeNetwork
        val isConnected = connectivityManager.getNetworkCapabilities(currentNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (isConnected == false) {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show()
        }
        return isConnected == true
    }
}