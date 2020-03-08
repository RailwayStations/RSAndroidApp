package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BaseApplication extends Application {

    private static final String TAG = BaseApplication.class.getSimpleName();
    private static final Boolean DEFAULT_FIRSTAPPSTART = false;
    private static final String DEFAULT = "";
    private static BaseApplication instance;

    public static final String DEFAULT_COUNTRY = "de";
    public static final String PREF_FILE = "APP_PREF_FILE";

    private DbAdapter dbAdapter;
    private RSAPI api;
    private SharedPreferences preferences;

    public BaseApplication() {
        setInstance(this);
    }

    public DbAdapter getDbAdapter() {
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
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();

        preferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        // migrate photo owner preference to boolean
        final Object photoOwner = preferences.getAll().get(getString(R.string.PHOTO_OWNER));
        if (photoOwner instanceof String && "YES".equals(photoOwner)) {
            setPhotoOwner(true);
        }

    }

    public String getApiUrl() {
        return preferences.getString(getString(R.string.API_URL), "https://api.railway-stations.org");
    }

    public void setApiUrl(String apiUrl) {
        putString(R.string.API_URL, apiUrl);
        this.api = null;
    }

    private void putBoolean(int key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(getString(key), value);
        editor.apply();
    }

    private void putString(int key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(key), StringUtils.trimToNull(value));
        editor.apply();
    }

    private void putStringSet(int key, Set<String> value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(getString(key), value);
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

    public void setCountryCodes(Set<String> countryCodes) {
        putStringSet(R.string.COUNTRIES, countryCodes);
    }

    public Set<String> getCountryCodes() {
        String oldCountryCode = preferences.getString(getString(R.string.COUNTRY), DEFAULT_COUNTRY);
        Set<String> stringSet = preferences.getStringSet(getString(R.string.COUNTRIES), new HashSet<>(Collections.singleton(oldCountryCode)));
        if (stringSet.isEmpty()) {
            stringSet = new HashSet<>(Collections.singleton(DEFAULT_COUNTRY));
        }
        return stringSet;
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
        putString(R.string.LICENCE, license != null ? license.toString() : License.UNKNOWN.toString());
    }

    public UpdatePolicy getUpdatePolicy() {
        return UpdatePolicy.byName(preferences.getString(getString(R.string.UPDATE_POLICY), License.UNKNOWN.toString()));
    }

    public void setUpdatePolicy(UpdatePolicy updatePolicy) {
        putString(R.string.UPDATE_POLICY, updatePolicy.toString());
    }

    public boolean getPhotoOwner() {
        return preferences.getBoolean(getString(R.string.PHOTO_OWNER), false);
    }

    public void setPhotoOwner(boolean photoOwner) {
        putBoolean(R.string.PHOTO_OWNER, photoOwner);
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

    public String getPassword() {
        return preferences.getString(getString(R.string.PASSWORD),
                preferences.getString(getString(R.string.UPLOAD_TOKEN), DEFAULT)); // for backward compatibility
    }

    public void setPassword(String password) {
        putString(R.string.UPLOAD_TOKEN, DEFAULT); // for backward compatibility
        putString(R.string.PASSWORD, password);
    }

    public PhotoFilter getPhotoFilter() {
        return PhotoFilter.valueOf(preferences.getString(getString(R.string.PHOTO_FILTER), PhotoFilter.ALL_STATIONS.toString()));
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

    public boolean getAnonymous() {
        return preferences.getBoolean(getString(R.string.ANONYMOUS), false);
    }

    public void setAnonymous(boolean anonymous) {
        putBoolean(R.string.ANONYMOUS, anonymous);
    }

    public void setProfile(Profile profile) {
        setLicense(profile.getLicense());
        setPhotoOwner(profile.isPhotoOwner());
        setAnonymous(profile.isAnonymous());
        setPhotographerLink(profile.getLink());
        setNickname(profile.getNickname());
        setEmail(profile.getEmail());
        setPassword(profile.getPassword());
    }

    public Profile getProfile() {
        Profile profile = new Profile();
        profile.setLicense(getLicense());
        profile.setPhotoOwner(getPhotoOwner());
        profile.setAnonymous(getAnonymous());
        profile.setLink(getPhotographerLink());
        profile.setNickname(getNickname());
        profile.setEmail(getEmail());
        profile.setPassword(getPassword());
        return profile;
    }

    public RSAPI getRSAPI() {
        if (api == null) {
            GsonBuilder gson = new GsonBuilder();
            gson.registerTypeAdapter(HighScore.class, new HighScore.HighScoreDeserializer());
            gson.registerTypeAdapter(License.class, new License.LicenseDeserializer());

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .addInterceptor(new UserAgentInterceptor(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + "); Android " + Build.VERSION.RELEASE + "/" + Build.VERSION.SDK_INT));

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(loggingInterceptor);
            }

            OkHttpClient okHttp = builder.build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getApiUrl())
                    .client(okHttp)
                    .addConverterFactory(GsonConverterFactory.create(gson.create()))
                    .build();

            api = retrofit.create(RSAPI.class);
        }
        return api;
    }

    public Uri getMapUri() {
        return getUri(getString(R.string.MAP_FILE));
    }

    public void setMapUri(Uri map) {
        putUri(R.string.MAP_FILE, map);
    }

    private void putUri(int key, Uri uri) {
        putString(key, uri != null ? uri.toString() : null);
    }

    public Uri getMapDirectoryUri() {
        return getUri(getString(R.string.MAP_DIRECTORY));
    }

    private Uri getUri(String key) {
        String value = preferences.getString(key, null);
        try {
            return Uri.parse(value);
        } catch (Exception ignored) {
            Log.e(TAG, "can't read Uri string " + value);
        }
        return null;
    }

    public void setMapDirectoryUri(Uri mapDirectory) {
        putUri(R.string.MAP_DIRECTORY, mapDirectory);
    }

    public Uri getMapThemeDirectoryUri() {
        return getUri(getString(R.string.MAP_THEME_DIRECTORY));
    }

    public void setMapThemeDirectoryUri(Uri mapThemeDirectory) {
        putUri(R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public Uri getMapThemeUri() {
        return getUri(getString(R.string.MAP_THEME));
    }

    public void setMapThemeUri(Uri mapTheme) {
        putUri(R.string.MAP_THEME, mapTheme);
    }

    /* This interceptor adds a custom User-Agent. */
    public static class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

}
