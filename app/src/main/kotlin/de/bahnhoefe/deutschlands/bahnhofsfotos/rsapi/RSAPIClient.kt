package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.gson.GsonBuilder
import de.bahnhoefe.deutschlands.bahnhofsfotos.BuildConfig
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
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
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

private val TAG = RSAPIClient::class.java.simpleName

class RSAPIClient(
    private val preferencesService: PreferencesService,
    private val clientId: String,
    val redirectUri: String,
) {
    private var api: RSAPI
    private var token: Token? = null
    private val pkce = PKCEUtil()

    init {
        val accessToken = preferencesService.accessToken
        if (accessToken != null) {
            token = Token(
                accessToken,
                "Bearer"
            )
        }
        api = createRSAPI()
    }

    val isLoggedIn: Boolean
        get() = hasToken()

    fun setBaseUrl(baseUrl: String) {
        preferencesService.apiUrl = baseUrl
        api = createRSAPI()
    }

    @get:Throws(NoSuchAlgorithmException::class)
    private val pkceCodeChallenge: String
        get() = pkce.codeChallenge

    private val pkceCodeVerifier: String
        get() = pkce.codeVerifier

    private fun createRSAPI(): RSAPI {
        val retrofit = Retrofit.Builder()
            .baseUrl(preferencesService.apiUrl)
            .client(createOkHttpClient())
            .addConverterFactory(createGsonConverterFactory())
            .build()

        return retrofit.create(RSAPI::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(AcceptLanguageInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply<HttpLoggingInterceptor> {
                setLevel(Level.BODY)
            })
        }

        return builder.build()
    }

    private fun createGsonConverterFactory(): GsonConverterFactory {
        val gson = GsonBuilder().apply {
            registerTypeAdapter(HighScore::class.java, HighScoreDeserializer())
            registerTypeAdapter(License::class.java, LicenseDeserializer())
        }
        return GsonConverterFactory.create(gson.create())
    }

    private class AcceptLanguageInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
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
        private val userAgent =
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + "); Android " + Build.VERSION.RELEASE + "/" + Build.VERSION.SDK_INT

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            return chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
            )
        }
    }

    fun getCountries(): Call<List<Country>> {
        return api.getCountries()
    }

    fun runUpdateCountriesAndStations(
        context: Context,
        dbAdapter: DbAdapter, // TODO: RSAPIClient should not call database
        listener: ResultListener,
    ) {
        getCountries().enqueue(object : Callback<List<Country>> {
            override fun onResponse(
                call: Call<List<Country>>,
                response: retrofit2.Response<List<Country>>
            ) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    dbAdapter.insertCountries(body)
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
        val countryCodes = preferencesService.countryCodes
        val overallSuccess = AtomicBoolean(true)
        val runningRequestCount = AtomicInteger(
            countryCodes.size
        )
        countryCodes.forEach(Consumer { countryCode: String ->
            api.getPhotoStationsByCountry(countryCode).enqueue(object : Callback<PhotoStations?> {
                override fun onResponse(
                    call: Call<PhotoStations?>,
                    response: retrofit2.Response<PhotoStations?>
                ) {
                    val stationList = response.body()
                    if (response.isSuccessful && stationList != null) {
                        dbAdapter.insertStations(
                            stationList,
                            countryCode
                        )
                        preferencesService.lastUpdate = System.currentTimeMillis()
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

    fun getPhotoStationById(country: String, id: String): Call<PhotoStations> {
        return api.getPhotoStationById(country, id)
    }

    fun getPhotoStationsByCountry(country: String): Call<PhotoStations> {
        return api.getPhotoStationsByCountry(country)
    }

    fun getPublicInbox(): Call<List<PublicInbox>> {
        return api.getPublicInbox()
    }

    fun getHighScore(): Call<HighScore> {
        return api.getHighScore()
    }

    fun getHighScore(country: String?): Call<HighScore> {
        return api.getHighScore(country)
    }

    fun reportProblem(problemReport: ProblemReport): Call<InboxResponse> {
        return api.reportProblem(userAuthorization!!, problemReport)
    }

    private val userAuthorization: String?
        get() = if (hasToken()) {
            token!!.tokenType + " " + token!!.accessToken
        } else null

    fun photoUpload(
        stationId: String?, countryCode: String?,
        stationTitle: String?, latitude: Double?,
        longitude: Double?, comment: String?,
        active: Boolean?, file: RequestBody?
    ): Call<InboxResponse> {
        return api.photoUpload(
            userAuthorization!!,
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

    fun queryUploadState(stateQueries: List<InboxStateQuery>): Call<List<InboxStateQuery>> {
        return api.queryUploadState(userAuthorization!!, stateQueries)
    }

    fun deleteInboxEntry(id: Long): Call<Void> {
        return api.deleteInboxEntry(userAuthorization!!, id)
    }

    fun getProfile(): Call<Profile> {
        return api.getProfile(userAuthorization!!)
    }

    fun saveProfile(profile: Profile): Call<Void> {
        return api.saveProfile(userAuthorization!!, profile)
    }

    fun changePassword(newPassword: String): Call<Void> {
        return api.changePassword(userAuthorization!!, ChangePassword(newPassword))
    }

    fun deleteAccount(): Call<Void> {
        return api.deleteAccount(userAuthorization!!)
    }

    fun resendEmailVerification(): Call<Void> {
        return api.resendEmailVerification(userAuthorization!!)
    }

    fun getStatistic(country: String?): Call<Statistic> {
        return api.getStatistic(country)
    }

    fun requestAccessToken(code: String): Call<Token> {
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
        preferencesService.accessToken = token?.accessToken
    }

    fun hasToken(): Boolean {
        return token != null
    }

    fun clearToken() {
        preferencesService.accessToken = null
        token = null
    }

    @Throws(NoSuchAlgorithmException::class)
    fun createAuthorizeUri(): Uri {
        return Uri.parse(preferencesService.apiUrl + "oauth2/authorize" + "?client_id=" + clientId + "&code_challenge=" + pkceCodeChallenge + "&code_challenge_method=S256&scope=all&response_type=code&redirect_uri=" + redirectUri)
    }

    fun interface ResultListener {
        fun onResult(success: Boolean)
    }

}