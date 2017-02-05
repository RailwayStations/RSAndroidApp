package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.FacebookSdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

import static android.content.Context.MODE_PRIVATE;
import static android.os.Build.VERSION_CODES.M;
import static com.facebook.FacebookSdk.getApplicationContext;
import static com.facebook.FacebookSdk.getApplicationId;
import static com.google.android.gms.analytics.internal.zzy.c;
import static com.google.android.gms.analytics.internal.zzy.d;

/**
 * Created by pelzi on 17.09.16.
 * // todo Cache einbauen
 */
public class BahnhofsFotoFetchTask extends AsyncTask<Integer, Void, URL> {
    private final static String TAG = BahnhofsFotoFetchTask.class.getSimpleName();
    private final BitmapAvailableHandler handler;
    //private String descriptorUrlPattern = "https://railway-stations.org/bahnhoefe/de/bhfnr/%d/bahnhofsfotos.json";
    private String descriptorUrlPattern = "";
    private String license;
    private Uri authorReference;
    private String author;
    private static final String DEFAULT_COUNTRY = "DE";
    //private Context context;
    private String countryShortCode;
    private Context context;
    SharedPreferences sharedPreferences;



    public BahnhofsFotoFetchTask(BitmapAvailableHandler handler, Context context) {
        this.handler = handler;
        sharedPreferences = context.getSharedPreferences("APP_PREF_FILE",Context.MODE_PRIVATE);
    }

    private String buildDescriptorUrlPattern() {
        countryShortCode = sharedPreferences.getString("APP_PREF_COUNTRY",DEFAULT_COUNTRY);
        descriptorUrlPattern = "https://railway-stations.org/bahnhoefe/" + countryShortCode.toLowerCase() + "/bhfnr/%d/bahnhofsfotos.json";
        Log.d(TAG, "descriptorUrlPattern: " + descriptorUrlPattern);
        return descriptorUrlPattern;
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
        handler.onBitmapAvailable(null);
    }

    @Override
    protected void onPostExecute(URL url) {
        super.onPostExecute(url);
        if (url != null) {
            Log.i(TAG, "Fetching photo from " + url);
            // fetch bitmap asynchronously, call onBitmapAvailable if ready
            BitmapCache.getInstance().getFoto(handler, url);
        } else {
            handler.onBitmapAvailable(null);
        }
    }

    @Override
    protected void onPreExecute() {
        buildDescriptorUrlPattern();
    }

    @Override
    protected URL doInBackground(Integer... integers) {
        Integer bahnhofsNr = integers[0];
        InputStream is = null;
        try {
            Log.i(TAG, "Fetching Photo descriptor for station nr.: " + bahnhofsNr);
            URL descriptorUrl = new URL(String.format(descriptorUrlPattern, bahnhofsNr));
            HttpURLConnection httpConnection = (HttpURLConnection) descriptorUrl.openConnection();
            is = httpConnection.getInputStream();
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String contentType = httpConnection.getContentType();
                if (!"application/json".equals(contentType)) {
                    Log.e(TAG, "Supplied URL does not appear to be a JSON resource (type=" + contentType + ")");
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
                    String jsonString = "";
                    String nextLine = null;
                    do {
                        nextLine = in.readLine();
                        if (nextLine != null)
                            jsonString += "\n" + nextLine;
                    } while (nextLine != null);
                    JSONObject object = new JSONArray(jsonString).getJSONObject(0);
                    URL photoURL = new URL(object.getString("bahnhofsfoto"));
                    setAuthorReference(Uri.parse(object.getString("fotograf")));
                    setAuthor(object.getString("fotograf-title"));
                    setLicense(object.getString("lizenz"));
                    return photoURL;
                }
            } else {
                Log.e(TAG, "Error downloading descriptor: " + httpConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not download descriptor");
        } catch (JSONException e) {
            Log.wtf(TAG, "Parse error on returned json", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close channel to photo download");
                }
            }
        }
        return null;
    }


    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Uri getAuthorReference() {
        return authorReference;
    }

    public void setAuthorReference(Uri author) {
        this.authorReference = author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }
}
