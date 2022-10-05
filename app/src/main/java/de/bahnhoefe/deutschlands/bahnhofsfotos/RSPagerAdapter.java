package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;

public class RSPagerAdapter extends RecyclerView.Adapter<RSPagerAdapter.PhotoViewHolder> {

    private static final int[] drawables = {R.drawable.ghost_station};

    private Context context;

    public RSPagerAdapter(final Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.photo_view_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PhotoViewHolder holder, final int position) {
        holder.photoView.setImageResource(drawables[position]);
    }

    @Override
    public int getItemCount() {
        return drawables.length;
    }


    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        PhotoView photoView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }

}
