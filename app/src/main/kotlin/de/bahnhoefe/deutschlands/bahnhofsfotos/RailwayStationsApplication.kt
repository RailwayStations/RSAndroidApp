package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDex
import dagger.hilt.android.HiltAndroidApp
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ExceptionHandler

@HiltAndroidApp
class RailwayStationsApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)

        // handle crashes only outside the crash reporter activity/process
        if (!isCrashReportingProcess) {
            Thread.setDefaultUncaughtExceptionHandler(
                Thread.getDefaultUncaughtExceptionHandler()?.let { ExceptionHandler(this, it) }
            )
        }
    }

    private val isCrashReportingProcess: Boolean
        get() {
            var processName: String? = ""
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                // Using the same technique as Application.getProcessName() for older devices
                // Using reflection since ActivityThread is an internal API
                try {
                    @SuppressLint("PrivateApi") val activityThread =
                        Class.forName("android.app.ActivityThread")
                    @SuppressLint("DiscouragedPrivateApi") val getProcessName =
                        activityThread.getDeclaredMethod("currentProcessName")
                    processName = getProcessName.invoke(null) as String
                } catch (ignored: Exception) {
                }
            } else {
                processName = getProcessName()
            }
            return processName != null && processName.endsWith(":crash")
        }


    companion object {
        private val TAG = RailwayStationsApplication::class.java.simpleName

        fun toUri(uriString: String?): Uri? {
            try {
                return Uri.parse(uriString)
            } catch (ignored: Exception) {
                Log.e(TAG, "can't read Uri string $uriString")
            }
            return null
        }
    }
}