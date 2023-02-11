package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ExceptionHandler;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PKCEUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;
import okhttp3.Interceptor;
import okhttp3.Response;

public class BaseApplication extends Application {

    private static final String TAG = BaseApplication.class.getSimpleName();
    private static final Boolean DEFAULT_FIRSTAPPSTART = false;
    private static final String DEFAULT = "";
    private static BaseApplication instance;

    public static final String DEFAULT_COUNTRY = "de";
    public static final String PREF_FILE = "APP_PREF_FILE";

    private DbAdapter dbAdapter;
    private RSAPIClient rsapiClient;
    private SharedPreferences preferences;
    private PKCEUtil pkce;

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

        // handle crashes only outside the crash reporter activity/process
        if (!isCrashReportingProcess()) {
            Thread.setDefaultUncaughtExceptionHandler(
                    new ExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler()));
        }
    }

    private boolean isCrashReportingProcess() {
        var processName = "";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Using the same technique as Application.getProcessName() for older devices
            // Using reflection since ActivityThread is an internal API
            try {
                @SuppressLint("PrivateApi")
                var activityThread = Class.forName("android.app.ActivityThread");
                @SuppressLint("DiscouragedPrivateApi")
                var getProcessName = activityThread.getDeclaredMethod("currentProcessName");
                processName = (String) getProcessName.invoke(null);
            } catch (Exception ignored) {
            }
        } else {
            processName = Application.getProcessName();
        }
        return processName != null && processName.endsWith(":crash");
    }

    private static void setInstance(@NonNull BaseApplication application) {
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
        var photoOwner = preferences.getAll().get(getString(R.string.PHOTO_OWNER));
        if ("YES".equals(photoOwner)) {
            setPhotoOwner(true);
        }

        rsapiClient = new RSAPIClient(getApiUrl(), getString(R.string.rsapiClientId), getAccessToken());
    }

    public String getApiUrl() {
        var apiUri = getUri(getString(R.string.API_URL));
        if (apiUri != null && Objects.requireNonNull(apiUri.getScheme()).matches("https?")) {
            var apiUrl = apiUri.toString();
            return apiUrl + (apiUrl.endsWith("/") ? "" : "/");
        }
        return "https://api.railway-stations.org/";
    }

    public void setApiUrl(String apiUrl) {
        putString(R.string.API_URL, apiUrl);
        rsapiClient.setBaseUrl(apiUrl);
    }

    private void putBoolean(int key, boolean value) {
        var editor = preferences.edit();
        editor.putBoolean(getString(key), value);
        editor.apply();
    }

    private void putString(int key, String value) {
        var editor = preferences.edit();
        editor.putString(getString(key), StringUtils.trimToNull(value));
        editor.apply();
    }

    private void putStringSet(int key, Set<String> value) {
        var editor = preferences.edit();
        editor.putStringSet(getString(key), value);
        editor.apply();
    }

    private void putLong(int key, long value) {
        var editor = preferences.edit();
        editor.putLong(getString(key), value);
        editor.apply();
    }

    private void putDouble(int key, double value) {
        var editor = preferences.edit();
        editor.putLong(getString(key), Double.doubleToRawLongBits(value));
        editor.apply();
    }

    private double getDouble(int key) {
        if (!preferences.contains(getString(key))) {
            return 0.0;
        }

        return Double.longBitsToDouble(preferences.getLong(getString(key), 0));
    }

    public void setCountryCodes(Set<String> countryCodes) {
        putStringSet(R.string.COUNTRIES, countryCodes);
    }

    public Set<String> getCountryCodes() {
        var oldCountryCode = preferences.getString(getString(R.string.COUNTRY), DEFAULT_COUNTRY);
        var stringSet = preferences.getStringSet(getString(R.string.COUNTRIES), new HashSet<>(Collections.singleton(oldCountryCode)));
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

    public boolean isEmailVerified() {
        return preferences.getBoolean(getString(R.string.PHOTO_OWNER), false);
    }

    public void setEmailVerified(boolean emailVerified) {
        putBoolean(R.string.PHOTO_OWNER, emailVerified);
    }

    public String getAccessToken() {
        return preferences.getString(getString(R.string.ACCESS_TOKEN), null);
    }

    public void setAccessToken(String apiToken) {
        putString(R.string.ACCESS_TOKEN, apiToken);
    }

    public StationFilter getStationFilter() {
        var photoFilter = getOptionalBoolean(R.string.STATION_FILTER_PHOTO);
        var activeFilter = getOptionalBoolean(R.string.STATION_FILTER_ACTIVE);
        var nicknameFilter = preferences.getString(getString(R.string.STATION_FILTER_NICKNAME), null);
        return new StationFilter(photoFilter, activeFilter, nicknameFilter);
    }

    private Boolean getOptionalBoolean(int key) {
        if (preferences.contains(getString(key))) {
            return Boolean.valueOf(preferences.getString(getString(key), "false"));
        }
        return null;
    }

    public void setStationFilter(StationFilter stationFilter) {
        putString(R.string.STATION_FILTER_PHOTO, stationFilter.hasPhoto() == null ? null : stationFilter.hasPhoto().toString());
        putString(R.string.STATION_FILTER_ACTIVE, stationFilter.isActive() == null ? null : stationFilter.isActive().toString());
        putString(R.string.STATION_FILTER_NICKNAME, stationFilter.getNickname());
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
        var latLong = new LatLong(getDouble(R.string.LAST_POSITION_LAT), getDouble(R.string.LAST_POSITION_LON));
        return new MapPosition(latLong, (byte) preferences.getLong(getString(R.string.LAST_POSITION_ZOOM), getZoomLevelDefault()));
    }

    public Location getLastLocation() {
        var location = new Location("");
        location.setLatitude(getDouble(R.string.LAST_POSITION_LAT));
        location.setLongitude(getDouble(R.string.LAST_POSITION_LON));
        return location;
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
        setEmailVerified(profile.isEmailVerified());
    }

    public Profile getProfile() {
        return Profile.builder()
                .license(getLicense())
                .photoOwner(getPhotoOwner())
                .anonymous(getAnonymous())
                .link(getPhotographerLink())
                .nickname(getNickname())
                .email(getEmail())
                .emailVerified(isEmailVerified())
                .build();
    }

    public RSAPIClient getRsapiClient() {
        return rsapiClient;
    }

    public String getMap() {
        return preferences.getString(getString(R.string.MAP_FILE), null);
    }

    public void setMap(String map) {
        putString(R.string.MAP_FILE, map);
    }

    private void putUri(int key, Uri uri) {
        putString(key, uri != null ? uri.toString() : null);
    }

    public Uri getMapDirectoryUri() {
        return getUri(getString(R.string.MAP_DIRECTORY));
    }

    private Uri getUri(String key) {
        return toUri(preferences.getString(key, null));
    }

    public Uri toUri(String uriString) {
        try {
            return Uri.parse(uriString);
        } catch (Exception ignored) {
            Log.e(TAG, "can't read Uri string " + uriString);
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

    public boolean getSortByDistance() {
        return preferences.getBoolean(getString(R.string.SORT_BY_DISTANCE), false);
    }

    public void setSortByDistance(boolean sortByDistance) {
        putBoolean(R.string.SORT_BY_DISTANCE, sortByDistance);
    }

    public String getRsapiClientId() {
        return getString(R.string.rsapiClientId);
    }

    public String getRsapiRedirectUri() {
        return getString(R.string.rsapiRedirectScheme) + "://" + getString(R.string.rsapiRedirectHost);
    }

    public String getPkceCodeChallenge() throws NoSuchAlgorithmException {
        pkce = new PKCEUtil();
        return pkce.getCodeChallenge();
    }

    public String getPkceCodeVerifier() {
        return pkce.getCodeVerifier();
    }

    public boolean isLoggedIn() {
        return rsapiClient.hasToken();
    }

    /* This interceptor adds a custom User-Agent. */
    public static class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        @NonNull
        public Response intercept(Interceptor.Chain chain) throws IOException {
            return chain.proceed(chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build());
        }
    }

}
