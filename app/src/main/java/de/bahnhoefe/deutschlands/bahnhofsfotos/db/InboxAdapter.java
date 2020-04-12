package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;

public class InboxAdapter extends ArrayAdapter<PublicInbox> {
    private final Activity context;
    private List<PublicInbox> publicInboxes;

    public InboxAdapter(final Activity context, final List<PublicInbox> publicInboxes) {
        super(context, R.layout.item_inbox, publicInboxes);
        this.publicInboxes = publicInboxes;
        this.context = context;
    }

    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            final LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.item_inbox, parent, false);

            // configure view holder
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = rowView.findViewById(R.id.txtStationName);
            viewHolder.id = rowView.findViewById(R.id.txtStationId);
            viewHolder.coords = rowView.findViewById(R.id.txtCoordinates);
            rowView.setTag(viewHolder);
        }

        // fill data
        final ViewHolder holder = (ViewHolder) rowView.getTag();
        final PublicInbox item = publicInboxes.get(position);
        holder.name.setText(item.getTitle());
        if (item.getStationId() != null) {
            holder.id.setText(item.getCountryCode().concat(":").concat(item.getStationId()));
        } else {
            holder.id.setText(R.string.missing_station);
        }
        holder.coords.setText(String.valueOf(item.getLat()).concat(",").concat(String.valueOf(item.getLon())));

        return rowView;
    }

    private static class ViewHolder {
        public TextView name;
        public TextView id;
        public TextView coords;
    }

}
