package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemStationBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.STATIONS

class StationListAdapter(context: Context, cursor: Cursor?, flags: Int) :
    CursorAdapter(context, cursor, flags) {
    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val binding = ItemStationBinding.inflate(mInflater, parent, false)
        val view = binding.root
        view.tag = binding
        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        //If you want to have zebra lines color effect uncomment below code
        if (cursor.position % 2 == 1) {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor)
        } else {
            view.setBackgroundResource(R.drawable.item_list_backgroundcolor2)
        }
        val binding = view.tag as ItemStationBinding
        binding.txtStationKey.text = getStationkey(cursor)
        binding.txtStationName.text = cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.TITLE))
        binding.hasPhoto.visibility =
            if (cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.PHOTO_URL)) != null) View.VISIBLE else View.INVISIBLE
    }

    private fun getStationkey(cursor: Cursor) =
        cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.COUNTRY)) + ": " + cursor.getString(
            cursor.getColumnIndexOrThrow(STATIONS.ID)
        )
}