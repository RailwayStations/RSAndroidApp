package de.bahnhoefe.deutschlands.bahnhofsfotos;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
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

    @GET("/stations")
    Call<List<Bahnhof>> getStations(@Query("country") String... countries);

    @GET("/countries")
    Call<List<Country>> getCountries();

    @GET("/{country}/stats")
    Call<Statistic> getStatistic(@Path("country") String country);

    @GET("/{country}/photographers.json")
    Call<HighScore> getHighScore(@Path("country") String country);

    @GET("/myProfile")
    Call<Profile> getProfile(@Header("Email") String email, @Header("Upload-Token") String uploadToken);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/myProfile")
    Call<Void> saveProfile(@Header("Email") String email, @Header("Upload-Token") String uploadToken, @Body Profile profile);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/registration")
    Call<Void> registration(@Body Profile profile);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/registration/withGoogleIdToken")
    Call<Profile> registrationWithGoogleIdToken(@Header("Google-Id-Token") String googleIdToken, @Body Profile profile);

    @POST("/photoUpload")
    Call<Void> photoUpload(@Header("Email") String email, @Header("Upload-Token") String uploadToken,
                           @Header("Station-Id") String stationId,
                           @Header("Country") String countryCode,
                           @Body RequestBody file);

}
