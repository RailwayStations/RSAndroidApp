package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy
import org.apache.commons.lang3.StringUtils
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition

class PreferencesService(private val context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, MODE_PRIVATE)
    private var encryptedPreferences: SharedPreferences

    init {
        // Creates the instance for the encrypted preferences.
        encryptedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Unable to create EncryptedSharedPreferences, fallback to unencrypted preferences",
                e
            )
            preferences
        }

        // migrate access token from unencrypted to encrypted preferences
        if (encryptedPreferences != preferences
            && !encryptedPreferences.contains(context.getString(R.string.ACCESS_TOKEN))
            && preferences.contains(context.getString(R.string.ACCESS_TOKEN))
        ) {
            preferences.getString(context.getString(R.string.ACCESS_TOKEN), null)?.let {
                encryptedPreferences.edit().putString(context.getString(R.string.ACCESS_TOKEN), it)
                    .apply()
                preferences.edit().remove(context.getString(R.string.ACCESS_TOKEN)).apply()
            }
        }
    }

    var apiUrl: String
        get() {
            val apiUri = preferences.getString(context.getString(R.string.API_URL), null)
            return getValidatedApiUrlString(apiUri)
        }
        set(apiUrl) {
            val validatedUrl = getValidatedApiUrlString(apiUrl)
            putString(R.string.API_URL, validatedUrl)
        }

    private fun putBoolean(key: Int, value: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(context.getString(key), value)
        editor.apply()
    }

    private fun putString(key: Int, value: String?) {
        val editor = preferences.edit()
        editor.putString(context.getString(key), StringUtils.trimToNull(value))
        editor.apply()
    }

    private fun putStringSet(key: Int, value: Set<String>?) {
        val editor = preferences.edit()
        editor.putStringSet(context.getString(key), value)
        editor.apply()
    }

    private fun putLong(key: Int, value: Long) {
        val editor = preferences.edit()
        editor.putLong(context.getString(key), value)
        editor.apply()
    }

    private fun putDouble(key: Int, value: Double) {
        val editor = preferences.edit()
        editor.putLong(context.getString(key), java.lang.Double.doubleToRawLongBits(value))
        editor.apply()
    }

    private fun getDouble(key: Int): Double {
        return if (!preferences.contains(context.getString(key))) {
            0.0
        } else java.lang.Double.longBitsToDouble(preferences.getLong(context.getString(key), 0))
    }

    var countryCodes: Set<String>
        get() {
            val oldCountryCode =
                preferences.getString(context.getString(R.string.COUNTRY), DEFAULT_COUNTRY)
            var stringSet = preferences.getStringSet(
                context.getString(R.string.COUNTRIES),
                HashSet(setOf(oldCountryCode))
            )
            if (stringSet!!.isEmpty()) {
                stringSet = HashSet(setOf(DEFAULT_COUNTRY))
            }
            return stringSet
        }
        set(countryCodes) {
            putStringSet(R.string.COUNTRIES, countryCodes)
        }
    var firstAppStart: Boolean
        get() = preferences.getBoolean(
            context.getString(R.string.FIRSTAPPSTART),
            DEFAULT_FIRSTAPPSTART
        )
        set(firstAppStart) {
            putBoolean(R.string.FIRSTAPPSTART, firstAppStart)
        }
    var license: License?
        get() = License.byName(
            preferences.getString(
                context.getString(R.string.LICENCE),
                License.UNKNOWN.toString()
            )!!
        )
        set(license) {
            putString(R.string.LICENCE, license?.toString() ?: License.UNKNOWN.toString())
        }
    var updatePolicy: UpdatePolicy
        get() = UpdatePolicy.byName(
            preferences.getString(
                context.getString(R.string.UPDATE_POLICY),
                License.UNKNOWN.toString()
            )!!
        )
        set(updatePolicy) {
            putString(R.string.UPDATE_POLICY, updatePolicy.toString())
        }
    private var photoOwner: Boolean
        get() = preferences.getBoolean(context.getString(R.string.PHOTO_OWNER), false)
        set(photoOwner) {
            putBoolean(R.string.PHOTO_OWNER, photoOwner)
        }
    private var photographerLink: String?
        get() = preferences.getString(context.getString(R.string.LINK_TO_PHOTOGRAPHER), DEFAULT)
        set(photographerLink) {
            putString(R.string.LINK_TO_PHOTOGRAPHER, photographerLink)
        }
    var nickname: String?
        get() = preferences.getString(context.getString(R.string.NICKNAME), DEFAULT)
        set(nickname) {
            putString(R.string.NICKNAME, nickname)
        }
    var email: String?
        get() = preferences.getString(context.getString(R.string.EMAIL), DEFAULT)
        set(email) {
            putString(R.string.EMAIL, email)
        }
    private var isEmailVerified: Boolean
        get() = preferences.getBoolean(context.getString(R.string.PHOTO_OWNER), false)
        set(emailVerified) {
            putBoolean(R.string.PHOTO_OWNER, emailVerified)
        }
    var accessToken: String?
        get() = encryptedPreferences.getString(context.getString(R.string.ACCESS_TOKEN), null)
        set(apiToken) {
            val editor = encryptedPreferences.edit()
            editor.putString(
                context.getString(R.string.ACCESS_TOKEN),
                StringUtils.trimToNull(apiToken)
            )
            editor.apply()
        }
    var stationFilter: StationFilter
        get() {
            val photoFilter = getOptionalBoolean(R.string.STATION_FILTER_PHOTO)
            val activeFilter = getOptionalBoolean(R.string.STATION_FILTER_ACTIVE)
            val nicknameFilter =
                preferences.getString(context.getString(R.string.STATION_FILTER_NICKNAME), null)
            return StationFilter(photoFilter, activeFilter, nicknameFilter)
        }
        set(stationFilter) {
            putString(
                R.string.STATION_FILTER_PHOTO,
                if (stationFilter.hasPhoto() == null) null else stationFilter.hasPhoto()
                    .toString()
            )
            putString(
                R.string.STATION_FILTER_ACTIVE,
                if (stationFilter.isActive == null) null else stationFilter.isActive.toString()
            )
            putString(R.string.STATION_FILTER_NICKNAME, stationFilter.nickname)
        }

    private fun getOptionalBoolean(key: Int): Boolean? {
        return if (preferences.contains(context.getString(key))) {
            java.lang.Boolean.valueOf(preferences.getString(context.getString(key), "false"))
        } else null
    }

    var lastUpdate: Long
        get() = preferences.getLong(context.getString(R.string.LAST_UPDATE), 0L)
        set(lastUpdate) {
            putLong(R.string.LAST_UPDATE, lastUpdate)
        }
    var isLocationUpdates: Boolean
        get() = preferences.getBoolean(context.getString(R.string.LOCATION_UPDATES), true)
        set(locationUpdates) {
            putBoolean(R.string.LOCATION_UPDATES, locationUpdates)
        }
    var lastMapPosition: MapPosition
        get() {
            val latLong = LatLong(
                getDouble(R.string.LAST_POSITION_LAT),
                getDouble(R.string.LAST_POSITION_LON)
            )
            return MapPosition(
                latLong,
                preferences.getLong(
                    context.getString(R.string.LAST_POSITION_ZOOM),
                    zoomLevelDefault.toLong()
                ).toByte()
            )
        }
        set(lastMapPosition) {
            putDouble(R.string.LAST_POSITION_LAT, lastMapPosition.latLong.latitude)
            putDouble(R.string.LAST_POSITION_LON, lastMapPosition.latLong.longitude)
            putLong(R.string.LAST_POSITION_ZOOM, lastMapPosition.zoomLevel.toLong())
        }
    val lastLocation: Location
        get() {
            val location = Location("")
            location.latitude = getDouble(R.string.LAST_POSITION_LAT)
            location.longitude = getDouble(R.string.LAST_POSITION_LON)
            return location
        }
    val zoomLevelDefault: Byte
        /**
         * @return the default starting zoom level if nothing is encoded in the map file.
         */
        get() = 12.toByte()
    var anonymous: Boolean
        get() = preferences.getBoolean(context.getString(R.string.ANONYMOUS), false)
        set(anonymous) {
            putBoolean(R.string.ANONYMOUS, anonymous)
        }
    var profile: Profile
        get() = Profile(
            nickname,
            license,
            photoOwner,
            anonymous,
            photographerLink,
            email,
            isEmailVerified
        )
        set(profile) {
            license = profile.license
            photoOwner = profile.photoOwner
            anonymous = profile.anonymous
            photographerLink = profile.link
            nickname = profile.nickname
            email = profile.email
            isEmailVerified = profile.emailVerified
        }
    var map: String?
        get() = preferences.getString(context.getString(R.string.MAP_FILE), null)
        set(map) {
            putString(R.string.MAP_FILE, map)
        }

    private fun putUri(key: Int, uri: Uri?) {
        putString(key, uri?.toString())
    }

    val mapDirectoryUri: Uri?
        get() = getUri(context.getString(R.string.MAP_DIRECTORY))

    private fun getUri(key: String): Uri? {
        return toUri(preferences.getString(key, null))
    }

    fun setMapDirectoryUri(mapDirectory: Uri?) {
        putUri(R.string.MAP_DIRECTORY, mapDirectory)
    }

    val mapThemeDirectoryUri: Uri?
        get() = getUri(context.getString(R.string.MAP_THEME_DIRECTORY))

    fun setMapThemeDirectoryUri(mapThemeDirectory: Uri?) {
        putUri(R.string.MAP_THEME_DIRECTORY, mapThemeDirectory)
    }

    val mapThemeUri: Uri?
        get() = getUri(context.getString(R.string.MAP_THEME))

    fun setMapThemeUri(mapTheme: Uri?) {
        putUri(R.string.MAP_THEME, mapTheme)
    }

    var sortByDistance: Boolean
        get() = preferences.getBoolean(context.getString(R.string.SORT_BY_DISTANCE), false)
        set(sortByDistance) {
            putBoolean(R.string.SORT_BY_DISTANCE, sortByDistance)
        }

    companion object {
        private val TAG = PreferencesService::class.java.simpleName
        private const val DEFAULT_FIRSTAPPSTART = false
        private const val DEFAULT = ""
        const val DEFAULT_COUNTRY = "de"
        const val PREF_FILE = "APP_PREF_FILE"

        private fun getValidatedApiUrlString(apiUrl: String?): String {
            val uri = toUri(apiUrl)
            uri?.let {
                val scheme = it.scheme
                if (scheme != null && scheme.matches("https?".toRegex())) {
                    return apiUrl + if (apiUrl!!.endsWith("/")) "" else "/"
                }
            }
            return "https://api.railway-stations.org/"
        }

        fun toUri(uriString: String?): Uri? {
            try {
                return Uri.parse(uriString)
            } catch (ignored: Exception) {
                Log.e(TAG, "can't read Uri string $uriString")
            }
            return null
        }
    }
}