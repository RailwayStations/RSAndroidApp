package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static java.util.stream.Collectors.toList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.List;

public class ProviderApp {

    private String countryCode;
    private String type;
    private String name;
    private String url;

    public ProviderApp() {

    }

    public ProviderApp(final String type, final String name, final String url) {
        this.type = type;
        this.name = name;
        this.url = url;
    }

    /**
     * Tries to open the provider app if installed. If it is not installed or cannot be opened Google Play Store will be opened instead.
     *
     * @param context activity context
     */
    public void openAppOrPlayStore(Context context) {
        // Try to open App
        boolean success = openApp(context);
        // Could not open App, open play store instead
        if (!success) {
            openUrl(context);
        }
    }

    /**
     * Open another app.
     *
     * @param context activity context
     * @return true if likely successful, false if unsuccessful
     * @see https://stackoverflow.com/a/7596063/714965
     */
    @SuppressWarnings("JavadocReference")
    private boolean openApp(Context context) {
        if (!isAndroid()) {
            return false;
        }
        var manager = context.getPackageManager();
        try {
            String packageName = Uri.parse(url).getQueryParameter("id");
            assert packageName != null;
            var intent = manager.getLaunchIntentForPackage(packageName);
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
     * Build an intent for an action to view a provider app url.
     *
     * @param context activity context
     */
    private void openUrl(Context context) {
        var intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
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

    public static boolean hasCompatibleProviderApps(List<ProviderApp> providerApps) {
        return providerApps.stream().anyMatch(ProviderApp::isCompatible);
    }

    public static List<ProviderApp> getCompatibleProviderApps(List<ProviderApp> providerApps) {
        return providerApps.stream()
                .filter(ProviderApp::isCompatible)
                .collect(toList());
    }

}