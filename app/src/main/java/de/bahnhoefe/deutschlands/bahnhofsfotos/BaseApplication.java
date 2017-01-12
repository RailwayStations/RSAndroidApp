package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

/**
 * Created by android_oma on 08.01.17.
 */

public class BaseApplication extends Application  {


    private static BaseApplication instance;
    public static final String PREFS_FILE = "";
    public static final String DEFAULT_COUNTRY = "DE";
    public static SharedPreferences preferences;
    private String countryShortCode = "";

    public BaseApplication() {
        setInstance(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    private static void setInstance(@NonNull final BaseApplication application) {
        instance = application;
    }

    public static Application getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        getCountryShortCode();

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public static SharedPreferences getPreferences() {
        return preferences;
    }

    public static void setPreferences(SharedPreferences preferences) {
        BaseApplication.preferences = preferences;
    }

    public void setCountryShortCode(String countryShortCode) {
        this.countryShortCode = countryShortCode;
    }

    public String getCountryShortCode() {
        preferences = getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);
        countryShortCode = preferences.getString(getString(R.string.COUNTRY),DEFAULT_COUNTRY);
        return countryShortCode;
    }
}
