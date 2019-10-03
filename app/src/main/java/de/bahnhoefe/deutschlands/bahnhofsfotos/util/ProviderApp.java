package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

public class ProviderApp {

    private static final String PLAYSTORE_ID_PREFIX = "?id=";

    /**
     * Tries to open the provider app if installed. If it is not installed or cannot be opened Google Play Store will be opened instead.
     *
     * @param context                current Context, like Activity, App, or Service
     * @param providerAndroidAppLink play store link, beginning with "https://play.google.com/store/apps/details?id="
     */
    public static void openAppOrPlayStore(Context context, String providerAndroidAppLink) {
        // Try to open App
        boolean success = ProviderApp.openAppFromUrl(context, providerAndroidAppLink);
        // Could not open App, open play store instead
        if (!success) {
            ProviderApp.openUrl(context, providerAndroidAppLink);
        }
    }

    /**
     * Parses a link and tries to find the apps packageName. On success the app will be opened.
     *
     * @param context                current Context, like Activity, App, or Service
     * @param providerAndroidAppLink play store link, beginning with "https://play.google.com/store/apps/details?id="
     * @return true if likely successful, false if unsuccessful
     */
    private static boolean openAppFromUrl(Context context, String providerAndroidAppLink) {
        // Try to open App
        int index = providerAndroidAppLink.indexOf(PLAYSTORE_ID_PREFIX);
        boolean success = false;
        if (index > 0) {
            try {
                String packageName = providerAndroidAppLink.substring(index + PLAYSTORE_ID_PREFIX.length());
                success = openApp(context, packageName);
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        }
        return success;
    }

    /**
     * Open another app.
     *
     * @param context     current Context, like Activity, App, or Service
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
    private static void openUrl(Context context, String providerAndroidApp) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        Uri uri = Uri.parse(providerAndroidApp);
        intent.setData(uri);
        context.startActivity(intent);
    }
}