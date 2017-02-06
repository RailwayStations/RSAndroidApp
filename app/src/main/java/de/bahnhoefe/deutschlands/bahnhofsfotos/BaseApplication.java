package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;

/**
 * Created by android_oma on 08.01.17.
 */

public class BaseApplication extends Application  {


    private static final String DEFAULT_FIRSTAPPSTART = "0";
    private static BaseApplication instance;
    public static final String DEFAULT_COUNTRY = "DE";

    public BahnhofsDbAdapter getDbAdapter() {
        return dbAdapter;
    }

    private BahnhofsDbAdapter dbAdapter;

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

    public static BaseApplication getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        dbAdapter = new BahnhofsDbAdapter(this);
        dbAdapter.open();
        getCountryShortCode();
        getFirstAppStart();

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

    public SharedPreferences getPreferences() {
        return getSharedPreferences(getString(R.string.PREF_FILE),MODE_PRIVATE);
    }


    public void setCountryShortCode(String countryShortCode) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(getString(R.string.COUNTRY),countryShortCode);
        editor.apply();
    }


    public String getCountryShortCode() {

        return getPreferences().getString(getString(R.string.COUNTRY),DEFAULT_COUNTRY);
    }

    public void setFirstAppStart(String firstAppStart){
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(getString(R.string.FIRSTAPPSTART),firstAppStart);
        editor.apply();
    }

    public String getFirstAppStart() {

        return getPreferences().getString(getString(R.string.FIRSTAPPSTART),DEFAULT_FIRSTAPPSTART);
    }
}
