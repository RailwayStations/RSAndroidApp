package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PageablePhoto;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    private final List<PageablePhoto> pageablePhotos = new ArrayList<>();

    private final Context context;

    public PhotoPagerAdapter(final Context context) {
        this.context = context;
    }

    public void addPageablePhoto(final PageablePhoto pageablePhoto) {
        pageablePhotos.add(pageablePhoto);
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
        var pageablePhoto = getPageablePhotoAtPosition(position);
        if (pageablePhoto == null) {
            holder.photoView.setImageResource(R.drawable.photo_missing);
        } else {
            holder.photoView.setImageBitmap(pageablePhoto.getBitmap());
        }
    }

    public PageablePhoto getPageablePhotoAtPosition(final int position) {
        return pageablePhotos.isEmpty() ? null : pageablePhotos.get(position);
    }

    @Override
    public int getItemCount() {
        return pageablePhotos.isEmpty() ? 1 : pageablePhotos.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        PhotoView photoView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }

}
