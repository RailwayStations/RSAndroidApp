package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;

import java.util.HashSet;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemCountryBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

public class CountryAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final String TAG = getClass().getSimpleName();
    private final Set<String> selectedCountries;

    public CountryAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        selectedCountries = new HashSet<>(BaseApplication.getInstance().getCountryCodes());
    }

    // new refactored after https://www.youtube.com/watch?v=wDBM6wVEO70&feature=youtu.be&t=7m
    public void getView(int selectedPosition, View convertView, ViewGroup parent, Cursor cursor) {
        ItemCountryBinding binding;
        if (convertView == null) {
            binding = ItemCountryBinding.inflate(mInflater, parent, false);
            convertView = binding.getRoot();

            //If you want to have zebra lines color effect uncomment below code
            if (selectedPosition % 2 == 1) {
                convertView.setBackgroundResource(R.drawable.item_list_backgroundcolor);
            } else {
                convertView.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
            }

            convertView.setTag(binding);
        } else {
            binding = (ItemCountryBinding) convertView.getTag();
        }

        binding.txtCountryShortCode.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE)));
        binding.txtCountryName.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYNAME)));

        var newCountry = cursor.getString(1);
        Log.i(TAG, newCountry);
        if (selectedCountries.contains(newCountry)) {
            binding.checkCountry.setChecked(false);
            selectedCountries.remove(newCountry);
        } else {
            binding.checkCountry.setChecked(true);
            selectedCountries.add(newCountry);
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        var binding = ItemCountryBinding.inflate(mInflater, parent, false);
        var view = binding.getRoot();
        view.setTag(binding);
        return view;
    }

    // wird nicht benutzt, ersetzt durch getView()
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        //If you want to have zebra lines color effect uncomment below code
        if (cursor.getPosition() % 2 == 1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor);
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
        }

        var binding = (ItemCountryBinding) view.getTag();
        binding.txtCountryShortCode.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE)));
        binding.txtCountryName.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYNAME)));

        var newCountry = cursor.getString(1);
        Log.i(TAG, newCountry);
        binding.checkCountry.setChecked(selectedCountries.contains(newCountry));
        binding.checkCountry.setOnClickListener(onStateChangedListener(binding.checkCountry, cursor.getPosition()));
    }

    private View.OnClickListener onStateChangedListener(CheckBox checkCountry, int position) {
        return v -> {
            var cursor = (Cursor) getItem(position);
            var country = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE));
            if (checkCountry.isChecked()) {
                selectedCountries.add(country);
            } else {
                selectedCountries.remove(country);
            }
        };
    }

    public Set<String> getSelectedCountries() {
        return selectedCountries;
    }

}

