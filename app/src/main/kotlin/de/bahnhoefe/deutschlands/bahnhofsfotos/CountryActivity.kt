package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.R
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityCountryBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CountryAdapter

class CountryActivity : AppCompatActivity() {
    private lateinit var countryAdapter: CountryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCountryBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        val cursor = (application as BaseApplication).dbAdapter.countryList
        countryAdapter = CountryAdapter(this, cursor, 0)
        binding.lstCountries.adapter = countryAdapter
        binding.lstCountries.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, view: View?, position: Int, _: Long ->
                countryAdapter.getView(
                    position,
                    view,
                    binding.lstCountries,
                    cursor
                )
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val baseApplication: BaseApplication = BaseApplication.instance
        val selectedCountries = countryAdapter.selectedCountries
        if (baseApplication.countryCodes != selectedCountries) {
            baseApplication.countryCodes = selectedCountries
            baseApplication.lastUpdate = 0L
        }
        super.onBackPressed()
    }
}