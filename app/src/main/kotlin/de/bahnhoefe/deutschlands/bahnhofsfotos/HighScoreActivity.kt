package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityHighScoreBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.HighScoreAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScore
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class HighScoreActivity : AppCompatActivity() {
    private var adapter: HighScoreAdapter? = null
    private lateinit var binding: ActivityHighScoreBinding

    @Inject
    lateinit var dbAdapter: DbAdapter

    @Inject
    lateinit var preferencesService: PreferencesService

    @Inject
    lateinit var rsapiClient: RSAPIClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHighScoreBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        val firstSelectedCountry = preferencesService.countryCodes.iterator().next()
        val countries = ArrayList(dbAdapter.allCountries)
        countries.sort()
        countries.add(0, Country("", getString(R.string.all_countries)))
        var selectedItem = 0
        for (country in countries) {
            if (country!!.code == firstSelectedCountry) {
                selectedItem = countries.indexOf(country)
            }
        }
        val countryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            countries.toTypedArray()
        )
        binding.countries.adapter = countryAdapter
        binding.countries.setSelection(selectedItem)
        binding.countries.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                loadHighScore(preferencesService, rsapiClient, parent.selectedItem as Country)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadHighScore(
        preferencesService: PreferencesService,
        rsapiClient: RSAPIClient,
        selectedCountry: Country
    ) {
        val highScoreCall =
            if (selectedCountry.code.isEmpty()) rsapiClient.getHighScore() else rsapiClient.getHighScore(
                selectedCountry.code
            )
        highScoreCall.enqueue(object : Callback<HighScore> {
            override fun onResponse(call: Call<HighScore>, response: Response<HighScore>) {
                if (response.isSuccessful) {
                    adapter = HighScoreAdapter(this@HighScoreActivity, response.body()!!.getItems())
                    binding.highscoreList.adapter = adapter
                    binding.highscoreList.onItemClickListener =
                        OnItemClickListener { adapter: AdapterView<*>, _: View?, position: Int, _: Long ->
                            val (name) = adapter.getItemAtPosition(position) as HighScoreItem
                            val stationFilter = preferencesService.stationFilter
                            stationFilter.nickname = name
                            preferencesService.stationFilter = stationFilter
                            val intent = Intent(this@HighScoreActivity, MapsActivity::class.java)
                            startActivity(intent)
                        }
                }
            }

            override fun onFailure(call: Call<HighScore>, t: Throwable) {
                Log.e(TAG, "Error loading highscore", t)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_loading_highscore) + t.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_high_score, menu)
        val manager = getSystemService(SEARCH_SERVICE) as SearchManager
        val search = menu.findItem(R.id.search).actionView as SearchView?
        search!!.setSearchableInfo(manager.getSearchableInfo(componentName))
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                Log.d(TAG, "onQueryTextSubmit ")
                if (adapter != null) {
                    adapter!!.filter.filter(s)
                    if (adapter!!.isEmpty) {
                        Toast.makeText(
                            this@HighScoreActivity,
                            R.string.no_records_found,
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@HighScoreActivity,
                            resources.getQuantityString(
                                R.plurals.records_found,
                                adapter!!.count,
                                adapter!!.count
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                Log.d(TAG, "onQueryTextChange ")
                if (adapter != null) {
                    adapter!!.filter.filter(s)
                }
                return false
            }
        })
        return true
    }

    companion object {
        private const val TAG = "HighScoreActivity"
    }
}