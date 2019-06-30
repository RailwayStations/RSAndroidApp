package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

public class CountryAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final String TAG = getClass().getSimpleName();
    private Set<String> selectedCountries = null;

    public CountryAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        BaseApplication baseApplication = BaseApplication.getInstance();
        selectedCountries = new HashSet<>(baseApplication.getCountryCodes());
    }

    // new refactored after https://www.youtube.com/watch?v=wDBM6wVEO70&feature=youtu.be&t=7m
    public View getView(int selectedPosition, View convertView, ViewGroup parent, Cursor cursor) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_country, parent, false);
            //If you want to have zebra lines color effect uncomment below code
            if (selectedPosition % 2 == 1) {
                convertView.setBackgroundResource(R.drawable.item_list_backgroundcolor);
            } else {
                convertView.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
            }
            holder = new ViewHolder();
            holder.checkCountry = (CheckBox) convertView.findViewById(R.id.checkCountry);
            holder.txtCountryShortCode = (TextView) convertView.findViewById(R.id.txtCountryShortCode);
            holder.txtCountryName = (TextView) convertView.findViewById(R.id.txtCountryName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.txtCountryShortCode.setText(cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE)));
        holder.txtCountryName.setText(cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME)));

        final String newCountry = cursor.getString(1);
        Log.i(TAG, newCountry);
        if (selectedCountries.contains(newCountry)) {
            holder.checkCountry.setChecked(false);
            selectedCountries.remove(newCountry);
        } else {
            holder.checkCountry.setChecked(true);
            selectedCountries.add(newCountry);
        }

        return convertView;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.item_country, parent, false);
        CountryAdapter.ViewHolder holder = new CountryAdapter.ViewHolder();
        holder.checkCountry = (CheckBox) view.findViewById(R.id.checkCountry);
        holder.txtCountryShortCode = (TextView) view.findViewById(R.id.txtCountryShortCode);
        holder.txtCountryName = (TextView) view.findViewById(R.id.txtCountryName);
        view.setTag(holder);
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

        CountryAdapter.ViewHolder holder = (CountryAdapter.ViewHolder) view.getTag();
        holder.txtCountryShortCode.setText(cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE)));
        holder.txtCountryName.setText(cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME)));

        final String newCountry = cursor.getString(1);
        Log.i(TAG, newCountry);
        holder.checkCountry.setChecked(selectedCountries.contains(newCountry));
        holder.checkCountry.setOnClickListener(onStateChangedListener(holder.checkCountry, cursor.getPosition()));
    }

    private View.OnClickListener onStateChangedListener(final CheckBox checkCountry, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = (Cursor) getItem(position);
                String country = cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE));
                if (checkCountry.isChecked()) {
                    selectedCountries.add(country);
                } else {
                    selectedCountries.remove(country);
                }
            }
        };
    }

    public Set<String> getSelectedCountries() {
        return selectedCountries;
    }

    private static class ViewHolder {
        CheckBox checkCountry;
        TextView txtCountryShortCode;
        TextView txtCountryName;
    }
}

