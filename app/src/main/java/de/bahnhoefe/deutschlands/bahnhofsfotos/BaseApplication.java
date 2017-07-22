package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;

public class BaseApplication extends Application {

    private static final Boolean DEFAULT_FIRSTAPPSTART = false;
    private static final String DEFAULT = "";
    private static BaseApplication instance;

    public static final String DEFAULT_COUNTRY = "DE";
    public static final String PREF_FILE = "APP_PREF_FILE";

    private BahnhofsDbAdapter dbAdapter;
    private SharedPreferences preferences;

    public BaseApplication() {
        setInstance(this);
    }

    public BahnhofsDbAdapter getDbAdapter() {
        return dbAdapter;
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

        preferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
    }

    public void setCountryShortCode(String countryShortCode) {
        putString(R.string.COUNTRY, countryShortCode);
    }

    public String getCountryShortCode() {
        return preferences.getString(getString(R.string.COUNTRY), DEFAULT_COUNTRY);
    }

    public void setFirstAppStart(boolean firstAppStart) {
        putBoolean(R.string.FIRSTAPPSTART, firstAppStart);
    }

    public boolean getFirstAppStart() {
        return preferences.getBoolean(getString(R.string.FIRSTAPPSTART), DEFAULT_FIRSTAPPSTART);
    }

    public boolean subscribtionStatus() {
        return preferences.getBoolean(getString(R.string.FRIENDLY_ENGAGE_TOPIC), false);
    }

    public void saveSubscribtionStatus(boolean status) {
        putBoolean(R.string.FRIENDLY_ENGAGE_TOPIC, status);
    }

    private void putBoolean(int key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(getString(key), value);
        editor.apply();
    }

    private void putString(int key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(key), value);
        editor.apply();
    }

    public String getLicense() {
        return preferences.getString(getString(R.string.LICENCE), DEFAULT);
    }

    public String getPhotoOwner() {
        return preferences.getString(getString(R.string.PHOTO_OWNER), DEFAULT);
    }

    public String getLinking() {
        return preferences.getString(getString(R.string.LINKING), DEFAULT);
    }

    public String getPhotographerLink() {
        return preferences.getString(getString(R.string.LINK_TO_PHOTOGRAPHER), DEFAULT);
    }

    public String getNickname() {
        return preferences.getString(getString(R.string.NICKNAME), DEFAULT);
    }

    public String getEmail() {
        return preferences.getString(getString(R.string.EMAIL), DEFAULT);
    }

    public String getUploadToken() {
        return preferences.getString(getString(R.string.UPLOAD_TOKEN), DEFAULT);
    }

    public void setLicense(String license) {
        putString(R.string.LICENCE, license);
    }

    public void setPhotoOwner(String photoOwner) {
        putString(R.string.PHOTO_OWNER, photoOwner);
    }

    public void setLinking(String linking) {
        putString(R.string.LINKING, linking);
    }

    public void setPhotographerLink(String photographerLink) {
        putString(R.string.LINK_TO_PHOTOGRAPHER, photographerLink);
    }

    public void setNickname(String nickname) {
        putString(R.string.NICKNAME, nickname);
    }

    public void setEmail(String email) {
        putString(R.string.EMAIL, email);
    }

    public void setUploadToken(String uploadToken) {
        putString(R.string.UPLOAD_TOKEN, uploadToken);
    }

}
