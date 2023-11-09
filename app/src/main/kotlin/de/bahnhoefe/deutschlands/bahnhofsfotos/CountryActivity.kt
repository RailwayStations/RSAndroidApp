package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityCountryBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CountryAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import javax.inject.Inject

@AndroidEntryPoint
class CountryActivity : AppCompatActivity() {
    private lateinit var countryAdapter: CountryAdapter

    @Inject
    lateinit var dbAdapter: DbAdapter

    @Inject
    lateinit var preferencesService: PreferencesService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCountryBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        val cursor = dbAdapter.countryList
        countryAdapter = CountryAdapter(this, preferencesService.countryCodes, cursor!!, 0)
        binding.lstCountries.adapter = countryAdapter
        binding.lstCountries.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, view: View?, position: Int, _: Long ->
                countryAdapter.getView(
                    position,
                    view,
                    binding.lstCountries,
                    cursor
                )
                val selectedCountries = countryAdapter.selectedCountries
                if (preferencesService.countryCodes != selectedCountries) {
                    preferencesService.countryCodes = selectedCountries
                    preferencesService.lastUpdate = 0L
                }
            }
    }

}