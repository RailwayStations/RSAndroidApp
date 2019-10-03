package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

public class ProviderApp {

    private static final String PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=";

    /**
     * Tries to open the provider app if installed. If it is not installed or cannot be opened Google Play Store will be opened instead.
     *
     * @param context     activity context
     * @param packageName play store package name
     */
    public static void openAppOrPlayStore(Context context, String packageName) {
        // Try to open App
        boolean success = openApp(context, packageName);
        // Could not open App, open play store instead
        if (!success) {
            ProviderApp.openUrl(context, packageName);
        }
    }

    /**
     * Open another app.
     *
     * @param context     activity context
     * @param packageName the full package name of the app to open
     * @return true if likely successful, false if unsuccessful
     * @see https://stackoverflow.com/a/7596063/714965
     */
    @SuppressWarnings("JavadocReference")
    private static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent intent = manager.getLaunchIntentForPackage(packageName);
            if (intent == null) {
                return false;
            }
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * Build an intent for an action to view a provider app on the play store.
     */
    private static void openUrl(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        Uri uri = Uri.parse(PLAY_STORE_LINK + packageName);
        intent.setData(uri);
        context.startActivity(intent);
    }
}