package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.CustomAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;

import static de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter.DATABASE_TABLE_UPLOADS;

public class GridViewAdapter extends CursorAdapter {

    private final static String TAG = GridViewAdapter.class.getSimpleName();

    private final Activity activity;

    private static LayoutInflater inflater = null;
    private List<LocalPhoto> files;

    public GridViewAdapter(final Activity activity, final List<LocalPhoto> files, final Cursor uploadCursor) {
        super(activity, uploadCursor, 0);
        this.activity = activity;
        this.files = files;
        inflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(R.layout.gridview_item, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.state = view.findViewById(R.id.upload_state);
        holder.image = view.findViewById(R.id.imageViewItem);
        holder.text = view.findViewById(R.id.text);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        long id = cursor.getLong(cursor.getColumnIndex("_id"));
        String uploadTitle = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.TITLE));
        String stationTitle = cursor.getString(cursor.getColumnIndex("stationTitle"));
        String problemType = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.PROBLEM_TYPE));
        String uploadState = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.UPLOAD_STATE));

        holder.state.setText(context.getString(UploadState.valueOf(uploadState).getTextId()));

        File file = FileUtils.getStoredMediaFile(context, id);
        // holder.state.setTextColor(activity.getResources().getColor(localPhoto.getState().getColorId()));

        String text = id + ": " + (uploadTitle != null ? uploadTitle : stationTitle);
        if (problemType != null) {
            text += " - " + context.getString(ProblemType.valueOf(problemType).getMessageId());
        }

        holder.text.setText(text);
        /*if (localPhoto.isOldFile()) {
            text.setTextColor(activity.getResources().getColor(R.color.gridItemError));
        } else {
            text.setTextColor(activity.getResources().getColor(R.color.gridItem));
        }*/

        if (file != null && file.exists()) {
            new BitmapLoaderTask(file, holder.image).execute();
        }
    }

    static class ViewHolder {
        TextView state;
        TextView text;
        ImageView image;
    }

    public static class BitmapLoaderTask extends AsyncTask<Void, Void, Bitmap> {

        private final File file;
        private final WeakReference<ImageView> viewRef;

        BitmapLoaderTask(final File file, final ImageView view) {
            this.file = file;
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        protected Bitmap doInBackground(final Void... voids) {

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Constants.STORED_PHOTO_WIDTH / 100;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        @Override
        protected void onPostExecute(final Bitmap scaled) {
            if (scaled != null) {
                Log.d(TAG, "Decoded " + file);
                final ImageView image = viewRef.get();
                if (image != null) {
                    image.setImageBitmap(scaled);
                }
            } else {
                Log.e(TAG, "Cannot decode " + file);
            }
        }
    }

}
