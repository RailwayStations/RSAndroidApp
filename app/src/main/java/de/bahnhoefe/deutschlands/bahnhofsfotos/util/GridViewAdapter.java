package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadStateQuery;

public class GridViewAdapter extends BaseAdapter {

    private final static String TAG = GridViewAdapter.class.getSimpleName();

    private final Activity activity;

    private static LayoutInflater inflater = null;
    private List<LocalPhoto> files;

    public GridViewAdapter(Activity a, List<LocalPhoto> files) {
        activity = a;
        this.files = files;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.d(TAG, "Constructed GridViewAdapter");
    }

    public int getCount() {
        return files.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        if (convertView == null) {
            vi = inflater.inflate(R.layout.gridview_item, null);
        }

        LocalPhoto localPhoto = files.get(position);
        TextView text = vi.findViewById(R.id.text);
        text.setText((localPhoto.getDisplayName()));
        if (localPhoto.isOldFile()) {
            text.setTextColor(activity.getResources().getColor(R.color.gridItemError));
        } else {
            text.setTextColor(activity.getResources().getColor(R.color.gridItem));
        }

        TextView state = vi.findViewById(R.id.upload_state);
        state.setText(activity.getString(R.string.upload_state, activity.getString(localPhoto.getState().getTextId())));
        state.setTextColor(activity.getResources().getColor(localPhoto.getState().getColorId()));

        new BitmapLoaderTask(localPhoto.getFile(), (ImageView) vi.findViewById(R.id.imageViewItem)).execute();
        return vi;
    }

    public static class BitmapLoaderTask extends AsyncTask<Void, Void, Bitmap> {

        private final File file;
        private final WeakReference<ImageView> viewRef;

        BitmapLoaderTask(File file, ImageView view) {
            this.file = file;
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = DetailsActivity.STORED_FOTO_WIDTH / 100;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        @Override
        protected void onPostExecute(Bitmap scaled) {
            if (scaled != null) {
                Log.d(TAG, "Decoded " + file);
                ImageView image = viewRef.get();
                if (image != null) {
                    image.setImageBitmap(scaled);
                }
            } else {
                Log.e(TAG, "Cannot decode " + file);
            }
        }
    }

}
