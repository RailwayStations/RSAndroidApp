package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadStateQuery;
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
    Call<Profile> getProfile(@Header("Authorization") String authorization);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/myProfile")
    Call<Void> saveProfile(@Header("Authorization") String authorization, @Body Profile profile);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/registration")
    Call<Void> registration(@Body Profile profile);

    @POST("/resetPassword")
    Call<Void> resetPassword(@Header("NameOrEmail") String emailOrNickname);

    @POST("/changePassword")
    Call<Void> changePassword(@Header("Authorization") String authorization, @Header("New-Password") String newPassword);

    @POST("/photoUpload")
    Call<Void> photoUpload(@Header("Authorization") String authorization,
                           @Header("Station-Id") String stationId,
                           @Header("Country") String countryCode,
                           @Header("Station-Title") String stationTitle,
                           @Header("Latitude") Double latitude,
                           @Header("Longitude") Double longitude,
                           @Header("Comment") String comment,
                           @Body RequestBody file);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("/photoUpload/queryState")
    Call<List<UploadStateQuery>> queryUploadState(@Header("Authorization") String authorization,
                           @Body List<LocalPhoto> uploadStateQueries);

    class Helper {
        static String getAuthorizationHeader(String email, String password) {
            byte[] data = new byte[0];
            try {
                data = (email + ":" + password).getBytes("UTF-8");
            } catch (UnsupportedEncodingException ignored) {
            }
            return "Basic " + Base64.encodeToString(data, Base64.NO_WRAP);
        }
    }

}
