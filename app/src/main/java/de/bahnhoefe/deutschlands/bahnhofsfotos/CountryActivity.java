package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityCountryBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CountryAdapter;

public class CountryActivity extends AppCompatActivity {

    private CountryAdapter countryAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final var binding = ActivityCountryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final var cursor = ((BaseApplication) getApplication()).getDbAdapter().getCountryList();
        countryAdapter = new CountryAdapter(this, cursor, 0);
        binding.lstCountries.setAdapter(countryAdapter);
        binding.lstCountries.setOnItemClickListener((listview, view, position, id) -> countryAdapter.getView(position, view, binding.lstCountries, cursor));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        final var baseApplication = BaseApplication.getInstance();
        final var selectedCountries = countryAdapter.getSelectedCountries();

        if (!baseApplication.getCountryCodes().equals(selectedCountries)) {
            baseApplication.setCountryCodes(selectedCountries);
            baseApplication.setLastUpdate(0L);
        }
        super.onBackPressed();
    }

}
