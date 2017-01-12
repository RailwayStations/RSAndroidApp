package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;
import de.bahnhoefe.deutschlands.bahnhofsfotos.CountryActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MyDataActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

import static android.R.attr.country;
import static android.content.Context.MODE_PRIVATE;
import static android.media.CamcorderProfile.get;
import static com.facebook.FacebookSdk.getApplicationContext;
import static com.google.android.gms.analytics.internal.zzy.B;
import static com.google.android.gms.analytics.internal.zzy.c;
import static com.google.android.gms.analytics.internal.zzy.h;
import static com.google.android.gms.analytics.internal.zzy.i;
import static com.google.android.gms.analytics.internal.zzy.r;
import static java.lang.String.valueOf;
import static java.security.AccessController.getContext;

/**
 * Created by android_oma on 08.12.16.
 */

public class CountryAdapter extends CursorAdapter{
    TextView txtCountryShortCode,txtCountryName;
    RadioButton rdCountry;
    CheckBox checkCountry;
    private static final String DEFAULT_COUNTRY = "DE";
    private int selectedPosition = -1;
    private LayoutInflater mInflater;
    private String TAG = getClass().getSimpleName();
    private int sharedPrefPosition;




    public CountryAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View   view    =    mInflater.inflate(R.layout.item_country, parent, false);
        CountryAdapter.ViewHolder holder  =   new CountryAdapter.ViewHolder();
        holder.txtCountryShortCode   =   (TextView)  view.findViewById(R.id.txtCountryShortCode);
        holder.txtCountryName    =   (TextView)  view.findViewById(R.id.txtCountryName);
        holder.checkCountry = (CheckBox)view.findViewById(R.id.checkCountry);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        Resources res = context.getResources();

        SharedPreferences sharedPreferences = context.getSharedPreferences(res.getString(R.string.PREF_FILE),Context.MODE_PRIVATE);
        String prefCountry = sharedPreferences.getString(res.getString(R.string.COUNTRY),DEFAULT_COUNTRY);

        SharedPreferences.Editor editor = sharedPreferences.edit();


        //If you want to have zebra lines color effect uncomment below code
        if(cursor.getPosition()%2==1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor);
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
        }

        CountryAdapter.ViewHolder holder  =   (CountryAdapter.ViewHolder) view.getTag();
        holder.txtCountryShortCode.setText(cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE)));
        holder.txtCountryName.setText(cursor.getString(cursor.getColumnIndex(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME)));

        Log.i(TAG, cursor.getString(1) + " " + prefCountry);
        if(cursor.getString(1).equals(prefCountry)){
            holder.checkCountry.setChecked(true);
        } else if(selectedPosition == cursor.getPosition()){
            holder.checkCountry.setChecked(true);
            editor.putString(res.getString(R.string.COUNTRY),cursor.getString(1));
            editor.commit();
        }else{
            holder.checkCountry.setChecked(false);
        }

        holder.checkCountry.setOnClickListener(onStateChangedListener(holder.checkCountry, cursor.getPosition()));


    }

    private View.OnClickListener onStateChangedListener(final CheckBox checkCountry, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCountry.isChecked()) {
                    selectedPosition = position;

                } else {
                    selectedPosition = -1;
                }
                notifyDataSetChanged();

            }
        };

    }


    public void setSelectedIndex(int index){
        selectedPosition = index;
    }


    static class ViewHolder {
        TextView txtCountryShortCode;
        TextView txtCountryName;
        CheckBox checkCountry;
    }
}

