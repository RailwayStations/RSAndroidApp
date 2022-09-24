package de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;
import de.bahnhoefe.deutschlands.bahnhofsfotos.BuildConfig;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RSAPIClient {

    private static final String TAG = RSAPIClient.class.getSimpleName();

    private RSAPI api;
    private String baseUrl;
    private String username;
    private String password;

    public RSAPIClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        api = createRSAPI();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        this.api = createRSAPI();
    }

    private RSAPI createRSAPI() {
        var gson = new GsonBuilder();
        gson.registerTypeAdapter(HighScore.class, new HighScore.HighScoreDeserializer());
        gson.registerTypeAdapter(License.class, new License.LicenseDeserializer());

        var builder = new OkHttpClient.Builder()
                .addInterceptor(new BaseApplication.UserAgentInterceptor(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + "); Android " + Build.VERSION.RELEASE + "/" + Build.VERSION.SDK_INT))
                .addInterceptor(chain -> {
                    if (username != null && password != null) {
                        Request.Builder builder1 = chain.request().newBuilder().header("Authorization",
                                Credentials.basic(username, password));
                        Request newRequest = builder1.build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(chain.request());
                });

        if (BuildConfig.DEBUG) {
            var loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create(gson.create()))
                .build();

        return retrofit.create(RSAPI.class);
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public boolean hasCredentials() {
        return username != null && password != null;
    }

    public void clearCredentials() {
        this.username = null;
        this.password = null;
    }

    public Call<List<Country>> getCountries() {
        return api.getCountries();
    }

    public void runUpdateCountriesAndStations(Context context, BaseApplication baseApplication, ResultListener listener) {
        getCountries().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Country>> call, @NonNull Response<List<Country>> response) {
                var body = response.body();
                if (response.isSuccessful() && body != null) {
                    baseApplication.getDbAdapter().insertCountries(body);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Country>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error refreshing countries", t);
                Toast.makeText(context, context.getString(R.string.error_updating_countries) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        api.getStations(baseApplication.getCountryCodes().toArray(new String[0])).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Station>> call, @NonNull Response<List<Station>> response) {
                List<Station> stationList = response.body();
                if (response.isSuccessful() && stationList != null) {
                    baseApplication.getDbAdapter().insertStations(stationList, baseApplication.getCountryCodes());
                    baseApplication.setLastUpdate(System.currentTimeMillis());
                }
                listener.onResult(response.isSuccessful());
            }

            @Override
            public void onFailure(@NonNull Call<List<Station>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error refreshing stations", t);
                Toast.makeText(context, context.getString(R.string.station_update_failed) + t.getMessage(), Toast.LENGTH_LONG).show();
                listener.onResult(false);
            }
        });

    }

    public Call<List<PublicInbox>> getPublicInbox() {
        return api.getPublicInbox();
    }

    public Call<HighScore> getHighScore() {
        return api.getHighScore();
    }

    public Call<HighScore> getHighScore(String country) {
        return api.getHighScore(country);
    }

    public Call<InboxResponse> reportProblem(ProblemReport problemReport) {
        return api.reportProblem(problemReport);
    }

    public Call<InboxResponse> photoUpload(String stationId, String countryCode,
                                           String stationTitle, Double latitude,
                                           Double longitude, String comment,
                                           Boolean active, RequestBody file) {
        return api.photoUpload(stationId, countryCode, stationTitle, latitude, longitude, comment, active, file);
    }

    public Call<List<InboxStateQuery>> queryUploadState(List<InboxStateQuery> stateQueries) {
        return api.queryUploadState(stateQueries);
    }

    public Call<Profile> getProfile() {
        return api.getProfile();
    }

    public Call<Void> registration(Profile profile) {
        return api.registration(profile);
    }

    public Call<Void> saveProfile(Profile profile) {
        return api.saveProfile(profile);
    }

    public Call<Void> resetPassword(String emailOrNickname) {
        return api.resetPassword(emailOrNickname);
    }

    public Call<Void> changePassword(String newPassword) {
        return api.changePassword(newPassword);
    }

    public Call<Void> resendEmailVerification() {
        return api.resendEmailVerification();
    }

    public Call<Statistic> getStatistic(String country) {
        return api.getStatistic(country);
    }

    public interface ResultListener {
        void onResult(boolean success);
    }

}
