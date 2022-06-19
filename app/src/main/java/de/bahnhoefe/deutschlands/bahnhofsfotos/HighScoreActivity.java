package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityHighScoreBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.HighScoreAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HighScoreActivity extends AppCompatActivity {

    private static final String TAG = "HighScoreActivity";
    private HighScoreAdapter adapter;
    private ActivityHighScoreBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHighScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        var baseApplication = (BaseApplication) getApplication();
        var firstSelectedCountry = baseApplication.getCountryCodes().iterator().next();
        var countries = baseApplication.getDbAdapter().getAllCountries();
        countries.add(0, new Country(getString(R.string.all_countries), "", null, null, null, null));
        int selectedItem = 0;
        for (var country : countries) {
            if (country.getCode().equals(firstSelectedCountry)) {
                selectedItem = countries.indexOf(country);
            }
        }

        var countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countries.toArray(new Country[0]));
        binding.countries.setAdapter(countryAdapter);
        binding.countries.setSelection(selectedItem);
        binding.countries.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadHighScore(baseApplication, (Country)parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadHighScore(BaseApplication baseApplication, Country selectedCountry) {
        var rsapi = baseApplication.getRsapiClient();
        Call<HighScore> highScoreCall = selectedCountry.getCode().isEmpty() ? rsapi.getHighScore() : rsapi.getHighScore(selectedCountry.getCode());
        highScoreCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<HighScore> call, @NonNull Response<HighScore> response) {
                if (response.isSuccessful()) {
                    adapter = new HighScoreAdapter(HighScoreActivity.this, response.body().getItems());
                    binding.highscoreList.setAdapter(adapter);
                    binding.highscoreList.setOnItemClickListener((adapter, v, position, arg3) -> {
                        HighScoreItem highScoreItem = (HighScoreItem) adapter.getItemAtPosition(position);
                        StationFilter stationFilter = baseApplication.getStationFilter();
                        stationFilter.setNickname(highScoreItem.getName());
                        baseApplication.setStationFilter(stationFilter);
                        Intent intent = new Intent(HighScoreActivity.this, MapsActivity.class);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<HighScore> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading highscore", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_loading_highscore) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_high_score, menu);

        var manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        var search = (SearchView) menu.findItem(R.id.search).getActionView();
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
