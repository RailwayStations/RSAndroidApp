package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityCountryBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CountryAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;

public class CountryActivity extends AppCompatActivity {

    private CountryAdapter countryAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityCountryBinding binding = ActivityCountryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final BaseApplication baseApplication = (BaseApplication) getApplication();
        final DbAdapter dbAdapter = baseApplication.getDbAdapter();

        final Cursor cursor = dbAdapter.getCountryList();
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
        final BaseApplication baseApplication = BaseApplication.getInstance();
        final Set<String> prefCountries = baseApplication.getCountryCodes();
        final Set<String> selectedCountries = countryAdapter.getSelectedCountries();

        if (!prefCountries.equals(selectedCountries)) {
            baseApplication.setCountryCodes(selectedCountries);
            baseApplication.setLastUpdate(0L);
        }
        super.onBackPressed();
    }

}
