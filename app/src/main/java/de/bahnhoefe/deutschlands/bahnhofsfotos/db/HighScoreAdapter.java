package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;

public class HighScoreAdapter extends ArrayAdapter<HighScoreItem> {
    private final Activity context;
    private final HighScoreItem[] highScore;

    public HighScoreAdapter(final Activity context, final HighScoreItem[] highScore) {
        super(context, R.layout.item_highscore, highScore);
        this.highScore = highScore;
        this.context = context;
    }

    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            final LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.item_highscore, parent, false);

            // configure view holder
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.photos = (TextView) rowView.findViewById(R.id.highscore_photos);
            viewHolder.name = (TextView) rowView.findViewById(R.id.highscore_name);
            viewHolder.award = (ImageView) rowView.findViewById(R.id.highscore_award);
            rowView.setTag(viewHolder);
        }

        // fill data
        final ViewHolder holder = (ViewHolder) rowView.getTag();
        final HighScoreItem item = highScore[position];
        holder.name.setText(item.getName());
        holder.photos.setText(String.valueOf(item.getPhotos()));
        switch (position) {
            case 0:
                holder.award.setImageResource(R.drawable.ic_crown_gold);
                holder.award.setVisibility(View.VISIBLE);
                break;
            case 1:
                holder.award.setImageResource(R.drawable.ic_crown_silver);
                holder.award.setVisibility(View.VISIBLE);
                break;
            case 2:
                holder.award.setImageResource(R.drawable.ic_crown_bronze);
                holder.award.setVisibility(View.VISIBLE);
                break;
            default:
                holder.award.setVisibility(View.INVISIBLE);
                break;
        }

        if(position % 2 == 1) {
            rowView.setBackgroundResource(R.drawable.item_list_backgroundcolor);
        } else {
            rowView.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
        }

        return rowView;
    }

    private static class ViewHolder {
        public TextView name;
        public TextView photos;
        public ImageView award;
    }

}
