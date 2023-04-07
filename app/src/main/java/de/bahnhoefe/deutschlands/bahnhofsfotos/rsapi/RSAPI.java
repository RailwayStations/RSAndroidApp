package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ChangePassword;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Token;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RSAPI {

    String TAG = RSAPI.class.getSimpleName();

    @GET("/countries")
    Call<List<Country>> getCountries();

    @GET("/stats")
    Call<Statistic> getStatistic(@Query("country") String country);

    @GET("/photographers")
    Call<HighScore> getHighScore(@Query("country") String country);

    @GET("/photographers")
    Call<HighScore> getHighScore();

    @GET("/myProfile")
    Call<Profile> getProfile(@Header("Authorization") String authorization);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/myProfile")
    Call<Void> saveProfile(@Header("Authorization") String authorization, @Body Profile profile);

    @Headers({
            "Content-Type: application/json"
    })

    @POST("/changePassword")
    Call<Void> changePassword(@Header("Authorization") String authorization, @Body ChangePassword changePassword);

    @POST("/photoUpload")
    Call<InboxResponse> photoUpload(@Header("Authorization") String authorization,
                                    @Header("Station-Id") String stationId,
                                    @Header("Country") String countryCode,
                                    @Header("Station-Title") String stationTitle,
                                    @Header("Latitude") Double latitude,
                                    @Header("Longitude") Double longitude,
                                    @Header("Comment") String comment,
                                    @Header("Active") Boolean active,
                                    @Body RequestBody file);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/userInbox")
    Call<List<InboxStateQuery>> queryUploadState(@Header("Authorization") String authorization,
                                                 @Body List<InboxStateQuery> inboxStateQueries);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/reportProblem")
    Call<InboxResponse> reportProblem(@Header("Authorization") String authorization,
                                      @Body ProblemReport problemReport);

    @GET("/publicInbox")
    Call<List<PublicInbox>> getPublicInbox();

    @POST("/resendEmailVerification")
    Call<Void> resendEmailVerification();

    @GET("/photoStationById/{country}/{id}")
    Call<PhotoStations> getPhotoStationById(@Path("country") String country, @Path("id") String id);

    @GET("/photoStationsByCountry/{country}")
    Call<PhotoStations> getPhotoStationsByCountry(@Path("country") String country);

    @FormUrlEncoded
    @POST("/oauth2/token")
    Call<Token> requestAccessToken(@Field("code") String code, @Field("client_id") String clientId, @Field("grant_type") String grantType, @Field("redirect_uri") String redirectUri, @Field("code_verifier") String codeVerifier);

    @FormUrlEncoded
    @POST("/oauth2/revoke")
    Call<Token> revokeToken(@Field("token") String accessToken, @Field("token_type_hint") String tokenTypeHint);

    @DELETE("/myProfile")
    Call<Void> deleteAccount(@Header("Authorization") String authorization);
    
}
