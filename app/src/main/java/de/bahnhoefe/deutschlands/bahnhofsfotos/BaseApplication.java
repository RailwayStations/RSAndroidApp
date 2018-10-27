package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Linking;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoOwner;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

public class BaseApplication extends Application {

    private static final String TAG = BaseApplication.class.getSimpleName();
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

    private void putLong(int key, long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(getString(key), value);
        editor.apply();
    }

    private void putDouble(int key, double value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(getString(key), Double.doubleToRawLongBits(value));
        editor.apply();
    }

    private double getDouble(int key, final double defaultValue) {
        if ( !preferences.contains(getString(key)))
            return defaultValue;

        return Double.longBitsToDouble(preferences.getLong(getString(key), 0));
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

    public License getLicense() {
        return License.byName(preferences.getString(getString(R.string.LICENCE), License.UNKNOWN.toString()));
    }

    public void setLicense(License license) {
        putString(R.string.LICENCE, license.toString());
    }

    public UpdatePolicy getUpdatePolicy() {
        return UpdatePolicy.byName(preferences.getString(getString(R.string.UPDATE_POLICY), License.UNKNOWN.toString()));
    }

    public void setUpdatePolicy(UpdatePolicy updatePolicy) {
        putString(R.string.UPDATE_POLICY, updatePolicy.toString());
    }

    public PhotoOwner getPhotoOwner() {
        return PhotoOwner.byName(preferences.getString(getString(R.string.PHOTO_OWNER), PhotoOwner.UNKNOWN.toString()));
    }

    public void setPhotoOwner(PhotoOwner photoOwner) {
        putString(R.string.PHOTO_OWNER, photoOwner.toString());
    }

    public Linking getLinking() {
        return Linking.byName(preferences.getString(getString(R.string.LINKING), Linking.UNKNOWN.toString()));
    }

    public void setLinking(Linking linking) {
        putString(R.string.LINKING, linking.toString());
    }

    public String getPhotographerLink() {
        return preferences.getString(getString(R.string.LINK_TO_PHOTOGRAPHER), DEFAULT);
    }

    public void setPhotographerLink(String photographerLink) {
        putString(R.string.LINK_TO_PHOTOGRAPHER, photographerLink);
    }

    public String getNickname() {
        return preferences.getString(getString(R.string.NICKNAME), DEFAULT);
    }

    public void setNickname(String nickname) {
        putString(R.string.NICKNAME, nickname);
    }

    public String getEmail() {
        return preferences.getString(getString(R.string.EMAIL), DEFAULT);
    }

    public void setEmail(String email) {
        putString(R.string.EMAIL, email);
    }

    public String getUploadToken() {
        return preferences.getString(getString(R.string.UPLOAD_TOKEN), DEFAULT);
    }

    public void setUploadToken(String uploadToken) {
        putString(R.string.UPLOAD_TOKEN, uploadToken);
    }

    public PhotoFilter getPhotoFilter() {
        return PhotoFilter.valueOf(preferences.getString(getString(R.string.PHOTO_FILTER), PhotoFilter.STATIONS_WITHOUT_PHOTO.toString()));
    }

    public void setPhotoFilter(PhotoFilter photoFilter) {
        putString(R.string.PHOTO_FILTER, photoFilter.toString());
    }

    public String getNicknameFilter() {
        return preferences.getString(getString(R.string.NICKNAME_FILTER), getNickname());
    }

    public void setNicknameFilter(String nicknameFilter) {
        putString(R.string.NICKNAME_FILTER, nicknameFilter);
    }

    public long getLastUpdate() {
        return preferences.getLong(getString(R.string.LAST_UPDATE), 0L);
    }

    public void setLastUpdate(long lastUpdate) {
        putLong(R.string.LAST_UPDATE, lastUpdate);
    }

    public void setLocationUpdates(boolean locationUpdates) {
        putBoolean(R.string.LOCATION_UPDATES, locationUpdates);
    }

    public boolean isLocationUpdates() {
        return preferences.getBoolean(getString(R.string.LOCATION_UPDATES), true);
    }

    public void setLastMapPosition(MapPosition lastMapPosition) {
        putDouble(R.string.LAST_POSITION_LAT, lastMapPosition.latLong.latitude);
        putDouble(R.string.LAST_POSITION_LON, lastMapPosition.latLong.longitude);
        putLong(R.string.LAST_POSITION_ZOOM, lastMapPosition.zoomLevel);
    }

    public MapPosition getLastMapPosition() {
        LatLong latLong = new LatLong(getDouble(R.string.LAST_POSITION_LAT, 0.0), getDouble(R.string.LAST_POSITION_LON, 0.0));
        MapPosition mapPosition = new MapPosition(latLong, (byte)preferences.getLong(getString(R.string.LAST_POSITION_ZOOM), getZoomLevelDefault()));
        return mapPosition;
    }

    /**
     * @return the default starting zoom level if nothing is encoded in the map file.
     */
    public byte getZoomLevelDefault() {
        return (byte) 12;
    }

}
