package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemCountryBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country

class CountryAdapter(
    context: Context,
    val countries: List<Country>,
    selectedCountryCodes: Set<String>,
) : ArrayAdapter<Country>(context, R.layout.item_country, countries) {

    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val selectedCountries: MutableSet<String> = selectedCountryCodes.toMutableSet()
    private val countryNamesByCode: Map<String, String>

    init {
        val countryCodes = context.resources.getStringArray(R.array.country_codes)
        val countryNames = context.resources.getStringArray(R.array.country_names)
        countryNamesByCode = countryCodes.mapIndexed { index, code ->
            Pair(code, countryNames[index])
        }
            .toMap()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        if (rowView == null) {
            val binding = ItemCountryBinding.inflate(layoutInflater, parent, false)
            rowView = binding.root
            rowView.setTag(binding)
        }

        val binding = rowView.tag as ItemCountryBinding
        val item = countries[position]
        binding.txtCountryShortCode.text = item.code
        val countryName = getCountryName(item.code, item.name)
        binding.txtCountryName.text = countryName
        binding.checkCountry.isChecked = selectedCountries.contains(item.code)
        binding.checkCountry.setOnClickListener(
            onStateChangedListener(
                binding.checkCountry,
                position
            )
        )


        return rowView
    }

    private fun getCountryName(countryCode: String, defaultName: String): String {
        return countryNamesByCode[countryCode] ?: defaultName
    }

    private fun onStateChangedListener(checkBox: CheckBox, position: Int): View.OnClickListener {
        return View.OnClickListener {
            val country: Country = countries[position]
            if (checkBox.isChecked) {
                selectedCountries.add(country.code)
            } else {
                selectedCountries.remove(country.code)
            }
            notifyDataSetChanged()
        }
    }
}