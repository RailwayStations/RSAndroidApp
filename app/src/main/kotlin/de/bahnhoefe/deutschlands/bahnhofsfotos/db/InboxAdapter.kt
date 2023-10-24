package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemInboxBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox

class InboxAdapter(private val context: Activity, private val publicInboxes: List<PublicInbox>) :
    ArrayAdapter<PublicInbox?>(
        context, R.layout.item_inbox, publicInboxes
    ) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        // reuse views
        val binding: ItemInboxBinding
        if (rowView == null) {
            binding = ItemInboxBinding.inflate(
                context.layoutInflater, parent, false
            )
            rowView = binding.root
            rowView.setTag(binding)
        } else {
            binding = rowView.tag as ItemInboxBinding
        }

        // fill data
        val (title, countryCode, stationId, lat, lon) = publicInboxes[position]
        binding.txtStationName.text = title
        if (stationId != null) {
            binding.txtStationId.text = "$countryCode:$stationId"
        } else {
            binding.txtStationId.setText(R.string.missing_station)
        }
        binding.txtCoordinates.text = lat.toString() + "," + lon.toString()
        return rowView
    }
}