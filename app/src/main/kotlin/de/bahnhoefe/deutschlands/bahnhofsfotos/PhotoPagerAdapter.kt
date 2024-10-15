package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import de.bahnhoefe.deutschlands.bahnhofsfotos.PhotoPagerAdapter.PhotoViewHolder
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PageablePhoto

class PhotoPagerAdapter(private val context: Context) : RecyclerView.Adapter<PhotoViewHolder>() {
    private val pageablePhotos: MutableList<PageablePhoto> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun addPageablePhoto(pageablePhoto: PageablePhoto): Int {
        pageablePhotos.add(pageablePhoto)
        notifyDataSetChanged()
        return pageablePhotos.size - 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.photo_view_item, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val pageablePhoto = getPageablePhotoAtPosition(position)
        if (pageablePhoto == null) {
            holder.photoView.setImageResource(R.drawable.photo_missing)
        } else {
            holder.photoView.setImageBitmap(pageablePhoto.bitmap)
        }
    }

    fun getPageablePhotoAtPosition(position: Int): PageablePhoto? {
        return if (pageablePhotos.isEmpty()) null else pageablePhotos[position]
    }

    override fun getItemCount(): Int {
        return if (pageablePhotos.isEmpty()) 1 else pageablePhotos.size
    }

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var photoView: PhotoView = itemView.findViewById(R.id.photoView)
    }
}