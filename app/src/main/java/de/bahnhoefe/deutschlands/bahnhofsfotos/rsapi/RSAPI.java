package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RSAPI {

    String TAG = RSAPI.class.getSimpleName();

    @GET("/stations")
    Call<List<Station>> getStations(@Query("country") String... countries);

    @GET("/countries")
    Call<List<Country>> getCountries();

    @GET("/stats")
    Call<Statistic> getStatistic(@Query("country") String country);

    @GET("/photographers")
    Call<HighScore> getHighScore(@Query("country") String country);

    @GET("/photographers")
    Call<HighScore> getHighScore();

    @GET("/myProfile")
    Call<Profile> getProfile();

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/myProfile")
    Call<Void> saveProfile(@Body Profile profile);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/registration")
    Call<Void> registration(@Body Profile profile);

    @POST("/resetPassword")
    Call<Void> resetPassword(@Header("NameOrEmail") String emailOrNickname);

    @POST("/changePassword")
    Call<Void> changePassword(@Header("New-Password") String newPassword);

    @POST("/photoUpload")
    Call<InboxResponse> photoUpload(@Header("Station-Id") String stationId,
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
    Call<List<InboxStateQuery>> queryUploadState(@Body List<InboxStateQuery> inboxStateQueries);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/reportProblem")
    Call<InboxResponse> reportProblem(@Body ProblemReport problemReport);

    @GET("/publicInbox")
    Call<List<PublicInbox>> getPublicInbox();

    @POST("/resendEmailVerification")
    Call<Void> resendEmailVerification();

    @GET("/photoStationById/{country}/{id}")
    Call<PhotoStations> getPhotoStationById(@Path("country") String country, @Path("id") String id);

    @GET("/photoStationsByCountry/{country}")
    Call<PhotoStations> getPhotoStationsByCountry(@Path("country") String country);

}
