package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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

    @GET("/{country}/stats")
    Call<Statistic> getStatistic(@Path("country") String country);

    @GET("/{country}/photographers.json")
    Call<HighScore> getHighScore(@Path("country") String country);

    @GET("/photographers.json")
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
    @POST("/registration")
    Call<Void> registration(@Body Profile profile);

    @POST("/resetPassword")
    Call<Void> resetPassword(@Header("NameOrEmail") String emailOrNickname);

    @POST("/changePassword")
    Call<Void> changePassword(@Header("Authorization") String authorization, @Header("New-Password") String newPassword);

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
    Call<InboxResponse> reportProblem(@Header("Authorization") String authorization, @Body ProblemReport problemReport);

    @GET("/publicInbox")
    Call<List<PublicInbox>> getPublicInbox();

    @POST("/resendEmailVerification")
    Call<Void> resendEmailVerification(@Header("Authorization") String authorization);

    static String getAuthorizationHeader(final String email, final String password) {
        final byte[] data = (email + ":" + password).getBytes(StandardCharsets.UTF_8);
        return "Basic " + Base64.encodeToString(data, Base64.NO_WRAP);
    }

    static void runUpdateCountriesAndStations(final Context context, final BaseApplication baseApplication, final ResultListener listener) {
        final RSAPI rsapi = baseApplication.getRSAPI();
        rsapi.getCountries().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull final Call<List<Country>> call, @NonNull final Response<List<Country>> response) {
                final var body = response.body();
                if (response.isSuccessful() && body != null) {
                    baseApplication.getDbAdapter().insertCountries(body);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<List<Country>> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error refreshing countries", t);
                Toast.makeText(context, context.getString(R.string.error_updating_countries) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        rsapi.getStations(baseApplication.getCountryCodes().toArray(new String[0])).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull final Call<List<Station>> call, @NonNull final Response<List<Station>> response) {
                final List<Station> stationList = response.body();
                if (response.isSuccessful() && stationList != null) {
                    baseApplication.getDbAdapter().insertStations(stationList, baseApplication.getCountryCodes());
                    baseApplication.setLastUpdate(System.currentTimeMillis());
                }
                listener.onResult(response.isSuccessful());
            }

            @Override
            public void onFailure(@NonNull final Call<List<Station>> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error refreshing stations", t);
                Toast.makeText(context, context.getString(R.string.station_update_failed) + t.getMessage(), Toast.LENGTH_LONG).show();
                listener.onResult(false);
            }
        });

    }

    interface ResultListener {
        void onResult(final boolean success);
    }

}
