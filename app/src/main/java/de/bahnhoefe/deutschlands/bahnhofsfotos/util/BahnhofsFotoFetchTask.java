package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pelzi on 17.09.16.
 */
public class BahnhofsFotoFetchTask extends AsyncTask<Integer, Void, URL> {
    private final static String TAG = BahnhofsFotoFetchTask.class.getSimpleName();
    private final BitmapAvailableHandler handler;
    private final static String descriptorUrlPattern = "http://www.deutschlands-bahnhoefe.org/bahnhofsfotos-cc0/%d/bahnhofsnr.json";
    BitmapFactory.Options options;
    private String license;
    private Uri author;

    public BahnhofsFotoFetchTask(BitmapAvailableHandler handler, BitmapFactory.Options bitmapOptionsForScreen) {
        this.handler = handler;
        this.options = bitmapOptionsForScreen;
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
            BitmapCache.getInstance().getFoto(handler, url, options);
        } else {
            handler.onBitmapAvailable(null);
        }
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
                    URL photoURL = new URL (object.getString("bahnhofsfoto"));
                    setAuthor(Uri.parse(object.getString("fotograf")));
                    setLicense(object.getString("lizenz"));
                    return photoURL;
                }
            } else {
                Log.e(TAG, "Error downloading descriptor: "+ httpConnection.getResponseCode());
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

    public Uri getAuthor() {
        return author;
    }

    public void setAuthor(Uri author) {
        this.author = author;
    }
}
