package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ChangePassword
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Token
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RSAPI {
    @GET("/countries")
    fun getCountries(): Call<List<Country>>

    @GET("/stats")
    fun getStatistic(@Query("country") country: String?): Call<Statistic>

    @GET("/photographers")
    fun getHighScore(@Query("country") country: String?): Call<HighScore>

    @GET("/photographers")
    fun getHighScore(): Call<HighScore>

    @GET("/myProfile")
    fun getProfile(@Header("Authorization") authorization: String): Call<Profile>

    @Headers("Content-Type: application/json")
    @POST("/myProfile")
    fun saveProfile(
        @Header("Authorization") authorization: String,
        @Body profile: Profile
    ): Call<Void>

    @Headers("Content-Type: application/json")
    @POST("/changePassword")
    fun changePassword(
        @Header("Authorization") authorization: String,
        @Body changePassword: ChangePassword
    ): Call<Void>

    @POST("/photoUpload")
    fun photoUpload(
        @Header("Authorization") authorization: String,
        @Header("Station-Id") stationId: String?,
        @Header("Country") countryCode: String?,
        @Header("Station-Title") stationTitle: String?,
        @Header("Latitude") latitude: Double?,
        @Header("Longitude") longitude: Double?,
        @Header("Comment") comment: String?,
        @Header("Active") active: Boolean?,
        @Body file: RequestBody?
    ): Call<InboxResponse>

    @Headers("Content-Type: application/json")
    @POST("/userInbox")
    fun queryUploadState(
        @Header("Authorization") authorization: String,
        @Body inboxStateQueries: List<InboxStateQuery>
    ): Call<List<InboxStateQuery>>

    @Headers("Content-Type: application/json")
    @POST("/reportProblem")
    fun reportProblem(
        @Header("Authorization") authorization: String,
        @Body problemReport: ProblemReport
    ): Call<InboxResponse>

    @GET("/publicInbox")
    fun getPublicInbox(): Call<List<PublicInbox>>

    @POST("/resendEmailVerification")
    fun resendEmailVerification(@Header("Authorization") authorization: String): Call<Void>

    @GET("/photoStationById/{country}/{id}")
    fun getPhotoStationById(
        @Path("country") country: String,
        @Path("id") id: String
    ): Call<PhotoStations>

    @GET("/photoStationsByCountry/{country}")
    fun getPhotoStationsByCountry(@Path("country") country: String): Call<PhotoStations>

    @FormUrlEncoded
    @POST("/oauth2/token")
    fun requestAccessToken(
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String
    ): Call<Token>

    @DELETE("/myProfile")
    fun deleteAccount(@Header("Authorization") authorization: String): Call<Void>

}