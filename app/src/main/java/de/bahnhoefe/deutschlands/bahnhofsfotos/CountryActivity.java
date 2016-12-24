package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CountryAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CustomAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

import static android.R.attr.country;
import static android.R.attr.defaultValue;
import static android.R.attr.id;
import static com.google.android.gms.analytics.internal.zzy.l;

public class CountryActivity extends AppCompatActivity {
    private BahnhofsDbAdapter dbAdapter;
    CountryAdapter countryAdapter;
    ListView listView;
    Cursor cursor;
    private String TAG = getClass().getSimpleName();


    private static final String DEFAULT = "DE";
    //private String countryShortCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country);

        dbAdapter = new BahnhofsDbAdapter(this);
        dbAdapter.open();

        cursor = dbAdapter.getCountryList();
        countryAdapter = new CountryAdapter(this, cursor,0);
        listView = (ListView) findViewById(R.id.lstCountries);
        listView.setAdapter(countryAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listview, View view, int position, long id) {
                Country country = dbAdapter.fetchCountriesByRowId(id);
                countryAdapter.setSelectedIndex(position);
                countryAdapter.notifyDataSetChanged();
                countryAdapter.bindView(listview,CountryActivity.this,cursor);
            }


        });


    }
    @Override
    protected void onStop() {
        super.onStop();
       /* SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);
        countryShortCode = sharedPreferences.getString(getString(R.string.COUNTRY),DEFAULT);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
    }
}
