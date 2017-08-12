package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.HighScoreAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import org.json.JSONObject;

public class HighScoreActivity extends AppCompatActivity {

    private static final String TAG = "HighScoreActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_score);

        if (ConnectionUtil.checkInternetConnection(this)) {
            new JSONHighscoreTask(((BaseApplication) getApplication()).getCountryShortCode()).execute();
        }

    }


    public class JSONHighscoreTask extends AsyncTask<Void, String, List<HighScoreItem>> {

        private final String countryCode;

        protected JSONHighscoreTask(final String countryCode) {
            this.countryCode = countryCode;
        }

        protected List<HighScoreItem> doInBackground(final Void... params) {
            BufferedReader reader = null;
            final List<HighScoreItem> highScore = new ArrayList<>();

            try {
                final URL url = new URL(String.format("%s/%s/photographers.json", Constants.API_START_URL, countryCode.toLowerCase()));
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                final InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                final StringBuilder buffer = new StringBuilder();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                final String finalJson = buffer.toString();
                int position = 0;

                final JSONObject photographers = new JSONObject(finalJson);
                Log.i(TAG, "Parsed " + photographers.length() + " photographers");

                final Iterator<String> photographerNames = photographers.keys();
                int lastPhotos = 0;
                while (photographerNames.hasNext()) {
                    final String name = photographerNames.next();
                    final int photos = photographers.getInt(name);
                    if (lastPhotos == 0 || lastPhotos > photos) {
                        position++;
                    }
                    lastPhotos = photos;
                    highScore.add(new HighScoreItem(name, photos, position));
                }
            } catch (final Exception e) {
                Log.e(TAG, "Error loading HighScore", e);
            }

            return highScore;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(final List<HighScoreItem> highScore) {
            final ListView listView = (ListView) findViewById(R.id.highscore_list);
            assert listView != null;
            listView.setAdapter(new HighScoreAdapter(HighScoreActivity.this, highScore.toArray(new HighScoreItem[0])));
        }
    }



}
