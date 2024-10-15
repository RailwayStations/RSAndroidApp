package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemHighscoreBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem
import java.util.Locale

class HighScoreAdapter(private val context: Activity, private var highScore: List<HighScoreItem>) :
    ArrayAdapter<HighScoreItem?>(context, R.layout.item_highscore, highScore) {
        
    private var filter: HighScoreFilter? = null

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        // reuse views
        val binding: ItemHighscoreBinding
        if (rowView == null) {
            binding = ItemHighscoreBinding.inflate(
                context.layoutInflater, parent, false
            )
            rowView = binding.root
            rowView.tag = binding
        } else {
            binding = rowView.tag as ItemHighscoreBinding
        }
        val (name, photoCount, position1) = highScore[position]
        binding.highscoreName.text = name
        binding.highscorePhotos.text = "$photoCount"
        binding.highscorePosition.text = "$position1."
        when (position1) {
            1 -> {
                binding.highscoreAward.setImageResource(R.drawable.ic_crown_gold)
                binding.highscoreAward.visibility = View.VISIBLE
                binding.highscorePosition.visibility = View.GONE
            }

            2 -> {
                binding.highscoreAward.setImageResource(R.drawable.ic_crown_silver)
                binding.highscoreAward.visibility = View.VISIBLE
                binding.highscorePosition.visibility = View.GONE
            }

            3 -> {
                binding.highscoreAward.setImageResource(R.drawable.ic_crown_bronze)
                binding.highscoreAward.visibility = View.VISIBLE
                binding.highscorePosition.visibility = View.GONE
            }

            else -> {
                binding.highscoreAward.visibility = View.GONE
                binding.highscorePosition.visibility = View.VISIBLE
            }
        }
        if (position % 2 == 1) {
            rowView.setBackgroundResource(R.drawable.item_list_backgroundcolor)
        } else {
            rowView.setBackgroundResource(R.drawable.item_list_backgroundcolor2)
        }
        return rowView
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = HighScoreFilter(highScore)
        }
        return filter!!
    }

    private inner class HighScoreFilter(originalItems: List<HighScoreItem>) : Filter() {
        private val originalItems: MutableList<HighScoreItem> = ArrayList()

        init {
            this.originalItems.addAll(originalItems)
        }

        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filterResults = FilterResults()
            val search = constraint.toString().lowercase(Locale.getDefault())
            val tempList = originalItems
                .filter { (name): HighScoreItem ->
                    name!!.lowercase(Locale.getDefault()).contains(search)
                }
                .toList()
            filterResults.values = tempList
            filterResults.count = tempList.size
            return filterResults
        }

        override fun publishResults(contraint: CharSequence, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            highScore = results.values as List<HighScoreItem>
            clear()
            addAll(highScore)
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}