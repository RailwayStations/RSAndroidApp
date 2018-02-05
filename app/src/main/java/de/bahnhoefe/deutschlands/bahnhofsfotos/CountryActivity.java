package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CountryAdapter;

public class CountryActivity extends AppCompatActivity {

    private CountryAdapter countryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country);

        final BaseApplication baseApplication = (BaseApplication) getApplication();
        BahnhofsDbAdapter dbAdapter = baseApplication.getDbAdapter();

        final Cursor cursor = dbAdapter.getCountryList();
        countryAdapter = new CountryAdapter(this, cursor, 0);
        final ListView listView = (ListView) findViewById(R.id.lstCountries);

        assert listView != null;
        listView.setAdapter(countryAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listview, View view, int position, long id) {
                countryAdapter.setSelectedIndex(position);
                countryAdapter.notifyDataSetChanged();
                countryAdapter.getView(position, view, listView, cursor);
            }


        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        BaseApplication baseApplication = BaseApplication.getInstance();
        String prefCountry = baseApplication.getCountryShortCode();
        String selectedCountry = countryAdapter.getSelectedCountry();

        if (!prefCountry.equals(selectedCountry)) {
            baseApplication.setCountryShortCode(selectedCountry);
            baseApplication.setLastUpdate(0L);
        }
        super.onBackPressed();
    }

}
