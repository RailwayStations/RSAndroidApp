package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.content.Context
import android.database.Cursor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CursorAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemCountryBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.COUNTRIES

class CountryAdapter(private val context: Context, c: Cursor?, flags: Int) : CursorAdapter(
    context, c, flags
) {
    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val selectedCountries: MutableSet<String> =
        BaseApplication.instance.countryCodes.toMutableSet()

    companion object {
        private val TAG = CountryAdapter::class.java.simpleName
    }

    fun getView(selectedPosition: Int, convertView: View?, parent: ViewGroup?, cursor: Cursor?) {
        var rowView = convertView
        val binding: ItemCountryBinding
        if (rowView == null) {
            binding = ItemCountryBinding.inflate(layoutInflater, parent, false)
            rowView = binding.root
            if (selectedPosition % 2 == 1) {
                rowView.setBackgroundResource(R.drawable.item_list_backgroundcolor)
            } else {
                rowView.setBackgroundResource(R.drawable.item_list_backgroundcolor2)
            }
            rowView.setTag(binding)
        } else {
            binding = rowView.tag as ItemCountryBinding
        }
        val countryCode =
            cursor!!.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYSHORTCODE))
        binding.txtCountryShortCode.text = countryCode
        val countryName = getCountryName(
            countryCode,
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYNAME))
        )
        binding.txtCountryName.text = countryName
        val newCountry = cursor.getString(1)
        Log.i(TAG, newCountry)
        if (selectedCountries.contains(newCountry)) {
            binding.checkCountry.isChecked = false
            selectedCountries.remove(newCountry)
        } else {
            binding.checkCountry.isChecked = true
            selectedCountries.add(newCountry)
        }
    }

    private fun getCountryName(countryCode: String, defaultName: String): String {
        val strId =
            context.resources.getIdentifier("country_$countryCode", "string", context.packageName)
        return if (strId != 0) {
            context.getString(strId)
        } else defaultName
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val binding = ItemCountryBinding.inflate(layoutInflater, parent, false)
        val view = binding.root
        view.tag = binding
        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        //If you want to have zebra lines color effect uncomment below code
        if (cursor.position % 2 == 1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor)
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2)
        }
        val binding = view.tag as ItemCountryBinding
        val countryCode = cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYSHORTCODE))
        binding.txtCountryShortCode.text = countryCode
        val countryName = getCountryName(
            countryCode,
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYNAME))
        )
        binding.txtCountryName.text = countryName
        val newCountry = cursor.getString(1)
        Log.i(TAG, newCountry)
        binding.checkCountry.isChecked = selectedCountries.contains(newCountry)
        binding.checkCountry.setOnClickListener(
            onStateChangedListener(
                binding.checkCountry,
                cursor.position
            )
        )
    }

    private fun onStateChangedListener(
        checkCountry: CheckBox,
        position: Int
    ): View.OnClickListener {
        return View.OnClickListener { v: View? ->
            val cursor = getItem(position) as Cursor
            val country = cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYSHORTCODE))
            if (checkCountry.isChecked) {
                selectedCountries.add(country)
            } else {
                selectedCountries.remove(country)
            }
        }
    }

    fun getSelectedCountries(): Set<String?> {
        return selectedCountries
    }
}