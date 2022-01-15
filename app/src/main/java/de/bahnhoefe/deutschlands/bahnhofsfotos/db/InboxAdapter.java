package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemInboxBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;

public class InboxAdapter extends ArrayAdapter<PublicInbox> {
    private final Activity context;
    private final List<PublicInbox> publicInboxes;

    public InboxAdapter(final Activity context, final List<PublicInbox> publicInboxes) {
        super(context, R.layout.item_inbox, publicInboxes);
        this.publicInboxes = publicInboxes;
        this.context = context;
    }

    @Override
    @NonNull
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        var rowView = convertView;
        // reuse views
        final ItemInboxBinding binding;
        if (rowView == null) {
            binding = ItemInboxBinding.inflate(context.getLayoutInflater(), parent, false);
            rowView = binding.getRoot();
            rowView.setTag(binding);
        } else {
            binding = (ItemInboxBinding) rowView.getTag();
        }

        // fill data
        final var item = publicInboxes.get(position);
        binding.txtStationName.setText(item.getTitle());
        if (item.getStationId() != null) {
            binding.txtStationId.setText(item.getCountryCode().concat(":").concat(item.getStationId()));
        } else {
            binding.txtStationId.setText(R.string.missing_station);
        }
        binding.txtCoordinates.setText(String.valueOf(item.getLat()).concat(",").concat(String.valueOf(item.getLon())));

        return rowView;
    }

}
