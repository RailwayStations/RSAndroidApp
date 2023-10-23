package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.gson.GsonBuilder
import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication
import de.bahnhoefe.deutschlands.bahnhofsfotos.BuildConfig
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ChangePassword
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore.HighScoreDeserializer
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License.LicenseDeserializer
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Token
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PKCEUtil
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain.proceed
import okhttp3.Interceptor.Chain.request
import okhttp3.OkHttpClient.Builder.addInterceptor
import okhttp3.OkHttpClient.Builder.build
import okhttp3.Request.Builder.build
import okhttp3.Request.Builder.header
import okhttp3.Request.newBuilder
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class RSAPIClient(
    private var baseUrl: String,
    private val clientId: String,
    accessToken: String?,
    val redirectUri: String
) {
    private var api: RSAPI
    private var token: Token? = null
    private var pkce: PKCEUtil? = null

    init {
        if (accessToken != null) {
            token = Token(
                accessToken,
                "Bearer"
            )
        }
        api = createRSAPI()
    }

    fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
        api = createRSAPI()
    }

    @get:Throws(NoSuchAlgorithmException::class)
    private val pkceCodeChallenge: String?
        private get() {
            pkce = PKCEUtil()
            return pkce.getCodeChallenge()
        }
    private val pkceCodeVerifier: String?
        private get() = if (pkce != null) pkce.getCodeVerifier() else null

    private fun createRSAPI(): RSAPI {
        val gson = GsonBuilder()
        gson.registerTypeAdapter(HighScore::class.java, HighScoreDeserializer())
        gson.registerTypeAdapter(License::class.java, LicenseDeserializer())
        val builder: Builder = Builder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(AcceptLanguageInterceptor())
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            builder.addInterceptor(loggingInterceptor)
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create(gson.create()))
            .build()
        return retrofit.create(RSAPI::class.java)
    }

    private class AcceptLanguageInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Chain): Response {
            return chain.proceed(
                chain.request().newBuilder()
                    .header("Accept-Language", Locale.getDefault().toLanguageTag())
                    .build()
            )
        }
    }

    /**
     * This interceptor adds a custom User-Agent.
     */
    class UserAgentInterceptor : Interceptor {
        private val USER_AGENT =
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + "); Android " + Build.VERSION.RELEASE + "/" + Build.VERSION.SDK_INT

        @Throws(IOException::class)
        override fun intercept(chain: Chain): Response {
            return chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
        }
    }

    val countries: Call<List<Country?>?>?
        get() = api.countries

    fun runUpdateCountriesAndStations(
        context: Context,
        baseApplication: BaseApplication?,
        listener: (Boolean) -> Unit
    ) {
        countries!!.enqueue(object : Callback<List<Country>?> {
            override fun onResponse(
                call: Call<List<Country>?>,
                response: retrofit2.Response<List<Country>?>
            ) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    baseApplication.getDbAdapter().insertCountries(body)
                }
            }

            override fun onFailure(call: Call<List<Country>?>, t: Throwable) {
                Log.e(TAG, "Error refreshing countries", t)
                Toast.makeText(
                    context,
                    context.getString(R.string.error_updating_countries) + t.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        val countryCodes = baseApplication.getCountryCodes()
        val overallSuccess = AtomicBoolean(true)
        val runningRequestCount = AtomicInteger(
            countryCodes!!.size
        )
        countryCodes!!.forEach(Consumer<String?> { countryCode: String? ->
            api.getPhotoStationsByCountry(countryCode).enqueue(object : Callback<PhotoStations?> {
                override fun onResponse(
                    call: Call<PhotoStations?>,
                    response: retrofit2.Response<PhotoStations?>
                ) {
                    val stationList = response.body()
                    if (response.isSuccessful && stationList != null) {
                        baseApplication.getDbAdapter().insertStations(stationList, countryCode)
                        baseApplication.setLastUpdate(System.currentTimeMillis())
                    }
                    if (!response.isSuccessful) {
                        overallSuccess.set(false)
                    }
                    onResult(response.isSuccessful)
                }

                override fun onFailure(call: Call<PhotoStations?>, t: Throwable) {
                    Log.e(TAG, "Error refreshing stations", t)
                    Toast.makeText(
                        context,
                        context.getString(R.string.station_update_failed) + t.message,
                        Toast.LENGTH_LONG
                    ).show()
                    onResult(false)
                }

                fun onResult(success: Boolean) {
                    val stillRunningRequests = runningRequestCount.decrementAndGet()
                    if (!success) {
                        overallSuccess.set(false)
                    }
                    if (stillRunningRequests == 0) {
                        listener.onResult(overallSuccess.get())
                    }
                }
            })
        })
    }

    fun getPhotoStationById(country: String?, id: String?): Call<PhotoStations?>? {
        return api.getPhotoStationById(country, id)
    }

    fun getPhotoStationsByCountry(country: String?): Call<PhotoStations?>? {
        return api.getPhotoStationsByCountry(country)
    }

    val publicInbox: Call<List<PublicInbox?>?>?
        get() = api.publicInbox
    val highScore: Call<HighScore?>?
        get() = api.highScore

    fun getHighScore(country: String?): Call<HighScore?>? {
        return api.getHighScore(country)
    }

    fun reportProblem(problemReport: ProblemReport?): Call<InboxResponse?>? {
        return api.reportProblem(userAuthorization, problemReport)
    }

    private val userAuthorization: String?
        private get() = if (hasToken()) {
            token!!.tokenType + " " + token!!.accessToken
        } else null

    fun photoUpload(
        stationId: String?, countryCode: String?,
        stationTitle: String?, latitude: Double?,
        longitude: Double?, comment: String?,
        active: Boolean?, file: RequestBody?
    ): Call<InboxResponse?>? {
        return api.photoUpload(
            userAuthorization,
            stationId,
            countryCode,
            stationTitle,
            latitude,
            longitude,
            comment,
            active,
            file
        )
    }

    fun queryUploadState(stateQueries: List<InboxStateQuery?>?): Call<List<InboxStateQuery?>?>? {
        return api.queryUploadState(userAuthorization, stateQueries)
    }

    val profile: Call<Profile?>?
        get() = api.getProfile(userAuthorization)

    fun saveProfile(profile: Profile?): Call<Void?>? {
        return api.saveProfile(userAuthorization, profile)
    }

    fun changePassword(newPassword: String?): Call<Void?>? {
        return api.changePassword(userAuthorization, ChangePassword(newPassword!!))
    }

    fun deleteAccount(): Call<Void?>? {
        return api.deleteAccount(userAuthorization)
    }

    fun resendEmailVerification(): Call<Void?>? {
        return api.resendEmailVerification(userAuthorization)
    }

    fun getStatistic(country: String?): Call<Statistic?>? {
        return api.getStatistic(country)
    }

    fun requestAccessToken(code: String?): Call<Token?>? {
        return api.requestAccessToken(
            code,
            clientId,
            "authorization_code",
            redirectUri,
            pkceCodeVerifier
        )
    }

    fun setToken(token: Token?) {
        this.token = token
    }

    fun hasToken(): Boolean {
        return token != null
    }

    fun clearToken() {
        token = null
    }

    @Throws(NoSuchAlgorithmException::class)
    fun createAuthorizeUri(): Uri {
        return Uri.parse(baseUrl + "oauth2/authorize" + "?client_id=" + clientId + "&code_challenge=" + pkceCodeChallenge + "&code_challenge_method=S256&scope=all&response_type=code&redirect_uri=" + redirectUri)
    }

    interface ResultListener {
        fun onResult(success: Boolean)
    }

    companion object {
        private val TAG = RSAPIClient::class.java.simpleName
    }
}