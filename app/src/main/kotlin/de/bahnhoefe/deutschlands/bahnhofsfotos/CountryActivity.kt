package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityCountryBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemCountryBinding
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

    private lateinit var previousSelectedCountries: Set<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityCountryBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.lstCountries) { v, windowInsets ->
            val innerPadding = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                innerPadding.left,
                innerPadding.top,
                innerPadding.right,
                innerPadding.bottom
            )
            windowInsets
        }
        setContentView(binding.root)
        previousSelectedCountries = preferencesService.countryCodes
        countryAdapter = CountryAdapter(
            this,
            dbAdapter.allCountries,
            previousSelectedCountries.toMutableSet()
        )
        binding.lstCountries.adapter = countryAdapter
        binding.lstCountries.setOnItemClickListener { _, view, _, _ ->
            val itemBinding: ItemCountryBinding = view.tag as ItemCountryBinding
            itemBinding.checkCountry.isChecked = !itemBinding.checkCountry.isChecked
            itemBinding.checkCountry.callOnClick()
        }
    }

    override fun onPause() {
        val selectedCountries = countryAdapter.selectedCountries
        if (previousSelectedCountries != selectedCountries) {
            preferencesService.countryCodes = selectedCountries
            preferencesService.lastUpdate = 0L
        }
        super.onPause()
    }
}