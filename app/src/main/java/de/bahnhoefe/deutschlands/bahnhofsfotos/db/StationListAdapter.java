package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
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
        final View view = mInflater.inflate(R.layout.item_station, parent, false);
        final ViewHolder holder = new ViewHolder();
        holder.txtId = (TextView) view.findViewById(R.id.txtState);
        holder.txtStationName = (TextView) view.findViewById(R.id.txtStationName);
        holder.hasPhoto = (ImageView) view.findViewById(R.id.hasPhoto);
        view.setTag(holder);
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

        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtId.setText(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.COUNTRY)).concat(": ").concat(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.ID))));
        holder.txtStationName.setText(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.TITLE)));
        holder.hasPhoto.setVisibility(cursor.getString(cursor.getColumnIndex(Constants.STATIONS.PHOTO_URL)) != null? View.VISIBLE : View.INVISIBLE);
    }

    static class ViewHolder {
        TextView txtId;
        TextView txtStationName;
        ImageView hasPhoto;
    }
}
