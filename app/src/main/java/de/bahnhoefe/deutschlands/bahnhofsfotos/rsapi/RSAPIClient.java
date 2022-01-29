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

    public RSAPIClient(final String baseUrl, final String username, final String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        api = createRSAPI();
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        this.api = createRSAPI();
    }

    private RSAPI createRSAPI() {
        final var gson = new GsonBuilder();
        gson.registerTypeAdapter(HighScore.class, new HighScore.HighScoreDeserializer());
        gson.registerTypeAdapter(License.class, new License.LicenseDeserializer());

        final var builder = new OkHttpClient.Builder()
                .addInterceptor(new BaseApplication.UserAgentInterceptor(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + "); Android " + Build.VERSION.RELEASE + "/" + Build.VERSION.SDK_INT))
                .addInterceptor(chain -> {
                    if (username != null && password != null) {
                        final Request.Builder builder1 = chain.request().newBuilder().header("Authorization",
                                Credentials.basic(username, password));
                        final Request newRequest = builder1.build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(chain.request());
                });

        if (BuildConfig.DEBUG) {
            final var loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create(gson.create()))
                .build();

        return retrofit.create(RSAPI.class);
    }

    public void setCredentials(final String  username, final String password) {
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

    public void runUpdateCountriesAndStations(final Context context, final BaseApplication baseApplication, final ResultListener listener) {
        api.getCountries().enqueue(new Callback<>() {
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

        api.getStations(baseApplication.getCountryCodes().toArray(new String[0])).enqueue(new Callback<>() {
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

    public Call<List<PublicInbox>> getPublicInbox() {
        return api.getPublicInbox();
    }

    public Call<HighScore> getHighScore() {
        return api.getHighScore();
    }

    public Call<HighScore> getHighScore(final String country) {
        return api.getHighScore(country);
    }

    public Call<InboxResponse> reportProblem(final ProblemReport problemReport) {
        return api.reportProblem(problemReport);
    }

    public Call<InboxResponse> photoUpload(final String stationId, final String countryCode,
                                           final String stationTitle, final Double latitude,
                                           final Double longitude, final String comment,
                                           final Boolean active, final RequestBody file) {
        return api.photoUpload(stationId, countryCode, stationTitle, latitude, longitude, comment, active, file);
    }

    public Call<List<InboxStateQuery>> queryUploadState(final List<InboxStateQuery> stateQueries) {
        return api.queryUploadState(stateQueries);
    }

    public Call<Profile> getProfile() {
        return api.getProfile();
    }

    public Call<Void> registration(final Profile profile) {
        return api.registration(profile);
    }

    public Call<Void> saveProfile(final Profile profile) {
        return api.saveProfile(profile);
    }

    public Call<Void> resetPassword(final String emailOrNickname) {
        return api.resetPassword(emailOrNickname);
    }

    public Call<Void> changePassword(final String newPassword) {
        return api.changePassword(newPassword);
    }

    public Call<Void> resendEmailVerification() {
        return api.resendEmailVerification();
    }

    public Call<Statistic> getStatistic(final String country) {
        return api.getStatistic(country);
    }

    public interface ResultListener {
        void onResult(final boolean success);
    }

}
