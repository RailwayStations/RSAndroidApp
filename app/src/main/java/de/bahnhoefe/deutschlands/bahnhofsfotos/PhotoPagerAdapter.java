package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    private List<Bitmap> bitmaps = new ArrayList<>();

    private final Context context;

    public PhotoPagerAdapter(final Context context) {
        this.context = context;
    }

    public void setBitmaps(List<Bitmap> bitmaps) {
        this.bitmaps = bitmaps;
        notifyDataSetChanged();
    }

    public void addPhotoUri(final Bitmap bitmap) {
        bitmaps.add(bitmap);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.photo_view_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PhotoViewHolder holder, final int position) {
        if (bitmaps.isEmpty()) {
            holder.photoView.setImageResource(R.drawable.photo_missing);
        } else {
            holder.photoView.setImageBitmap(bitmaps.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return bitmaps.isEmpty() ? 1 : bitmaps.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        PhotoView photoView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }

}
