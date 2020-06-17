package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class ProviderApp {

    private String countryCode;
    private String type;
    private String name;
    private String url;

    /**
     * Tries to open the provider app if installed. If it is not installed or cannot be opened Google Play Store will be opened instead.
     *
     * @param context     activity context
     */
    public void openAppOrPlayStore(final Context context) {
        // Try to open App
        final boolean success = openApp(context);
        // Could not open App, open play store instead
        if (!success) {
            openUrl(context);
        }
    }

    /**
     * Open another app.
     *
     * @param context     activity context
     * @return true if likely successful, false if unsuccessful
     * @see https://stackoverflow.com/a/7596063/714965
     */
    @SuppressWarnings("JavadocReference")
    private boolean openApp(final Context context) {
        if (!isAndroid()) {
            return false;
        }
        final PackageManager manager = context.getPackageManager();
        try {
            final Uri uri = Uri.parse(url);
            final String packageName = uri.getQueryParameter("id");
            final Intent intent = manager.getLaunchIntentForPackage(packageName);
            if (intent == null) {
                return false;
            }
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(intent);
            return true;
        } catch (final ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * Build an intent for an action to view a provider app url.
     *
     * @param context     activity context
     */
    private void openUrl(final Context context) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);

        final Uri uri = Uri.parse(url);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public boolean isAndroid() {
        return "android".equals(type);
    }

    public boolean isWeb() {
        return "web".equals(type);
    }

    public boolean isCompatible() {
        return isAndroid() || isWeb();
    }

    public static boolean hasCompatibleProviderApps(final List<ProviderApp> providerApps) {
        for (final ProviderApp pa : providerApps) {
            if (pa.isCompatible()) {
                return true;
            }
        }
        return false;
    }

    public static List<ProviderApp> getCompatibleProviderApps(final List<ProviderApp> providerApps) {
        final List<ProviderApp> compatibleProviderApps = new ArrayList<>();
        for (final ProviderApp pa : providerApps) {
            if (pa.isCompatible()) {
                compatibleProviderApps.add(pa);
            }
        }
        return compatibleProviderApps;
    }

}