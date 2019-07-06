package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.HighScoreAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HighScoreActivity extends AppCompatActivity {

    private static final String TAG = "HighScoreActivity";
    private HighScoreAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_score);

        final BaseApplication baseApplication = (BaseApplication) getApplication();
        Set<String> countryCodes = baseApplication.getCountryCodes();
        final Set<Country> countries = baseApplication.getDbAdapter().fetchCountriesByCountryCodes(countryCodes);

        Spinner countrySpinner = findViewById(R.id.countries);
        ArrayAdapter<Country> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countries.toArray(new Country[0]));
        countrySpinner.setAdapter(countryAdapter);
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadHighScore(baseApplication, (Country)parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing todo
            }
        });

        loadHighScore(baseApplication, (Country) countrySpinner.getSelectedItem());
    }

    private void loadHighScore(BaseApplication baseApplication, Country selectedCountry) {
        baseApplication.getRSAPI().getHighScore(selectedCountry.getCode()).enqueue(new Callback<HighScore>() {
            @Override
            public void onResponse(Call<HighScore> call, Response<HighScore> response) {
                if (response.isSuccessful()) {
                    final ListView listView = findViewById(R.id.highscore_list);
                    assert listView != null;
                    adapter = new HighScoreAdapter(HighScoreActivity.this, response.body().getItems());
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
                            HighScoreItem highScoreItem = (HighScoreItem) adapter.getItemAtPosition(position);
                            BaseApplication baseApplication = (BaseApplication) getApplication();
                            baseApplication.setPhotoFilter(PhotoFilter.NICKNAME);
                            baseApplication.setNicknameFilter(highScoreItem.getName());
                            Intent intent = new Intent(HighScoreActivity.this, MapsActivity.class);
                            startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<HighScore> call, Throwable t) {
                Log.e(TAG, "Error loading highscore", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_loading_highscore) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_high_score, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit ");
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                    if (adapter.isEmpty()) {
                        Toast.makeText(HighScoreActivity.this, R.string.no_records_found, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(HighScoreActivity.this, getResources().getQuantityString(R.plurals.records_found, adapter.getCount(), adapter.getCount()), Toast.LENGTH_LONG).show();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "onQueryTextChange ");
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                }
                return false;
            }

        });

        return true;
    }

}
