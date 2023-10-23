package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemStationBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

public class StationListAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;

    public StationListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        var binding = ItemStationBinding.inflate(mInflater, parent, false);
        var view = binding.getRoot();
        view.setTag(binding);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        //If you want to have zebra lines color effect uncomment below code
        if (cursor.getPosition() % 2 == 1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor);
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
        }

        var binding = (ItemStationBinding) view.getTag();
        binding.txtState.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.COUNTRY)).concat(": ").concat(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.ID))));
        binding.txtStationName.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.TITLE)));
        binding.hasPhoto.setVisibility(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTO_URL)) != null? View.VISIBLE : View.INVISIBLE);
    }

}
