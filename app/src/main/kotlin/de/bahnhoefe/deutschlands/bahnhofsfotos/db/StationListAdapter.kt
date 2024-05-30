package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemStationBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.Stations

class StationListAdapter(context: Context, cursor: Cursor?, flags: Int) :
    CursorAdapter(context, cursor, flags) {
    private val mInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val binding = ItemStationBinding.inflate(mInflater, parent, false)
        val view = binding.root
        view.tag = binding
        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        if (cursor.position % 2 == 1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor)
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2)
        }
        (view.tag as ItemStationBinding).apply {
            txtStationKey.text = getStationkey(cursor)
            txtStationName.text = cursor.getString(cursor.getColumnIndexOrThrow(Stations.TITLE))
            hasPhoto.visibility =
                if (cursor.getString(cursor.getColumnIndexOrThrow(Stations.PHOTO_URL)) != null) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun getStationkey(cursor: Cursor) =
        cursor.getString(cursor.getColumnIndexOrThrow(Stations.COUNTRY)) + ": " + cursor.getString(
            cursor.getColumnIndexOrThrow(Stations.ID)
        )
}