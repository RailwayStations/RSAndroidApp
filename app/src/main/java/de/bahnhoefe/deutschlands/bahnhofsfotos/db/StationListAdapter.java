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

/**
 * Adapter for ListView of stations
 */
public class StationListAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;

    public StationListAdapter(final Context context, final Cursor c, final int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final ItemStationBinding binding = ItemStationBinding.inflate(mInflater, parent, false);
        final View view = binding.getRoot();
        view.setTag(binding);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        //If you want to have zebra lines color effect uncomment below code
        if (cursor.getPosition() % 2 == 1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor);
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
        }

        final ItemStationBinding binding = (ItemStationBinding) view.getTag();
        binding.txtState.setText(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.COUNTRY)).concat(": ").concat(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.ID))));
        binding.txtStationName.setText(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.TITLE)));
        binding.hasPhoto.setVisibility(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.PHOTO_URL)) != null? View.VISIBLE : View.INVISIBLE);
    }

}
