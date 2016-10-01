package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

//import static com.google.android.gms.analytics.internal.zzy.G;

/**
 * Created by android_oma on 24.07.16.
 */

public class GridViewAdapter extends BaseAdapter {

    private final static String TAG = GridViewAdapter.class.getSimpleName();

    // Declare variables
    private Activity activity;
    private String[] filepath;
    private String[] filename;

    private static LayoutInflater inflater = null;

    public GridViewAdapter(Activity a, String[] fpath, String[] fname) {
        activity = a;
        filepath = fpath;
        filename = fname;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.d(TAG, "Constructed GridViewAdapter");
    }

    public int getCount() {
        return filepath.length;

    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        if (convertView == null)
            vi = inflater.inflate(R.layout.gridview_item, null);
        // Locate the TextView in gridview_item.xml
        TextView text = (TextView) vi.findViewById(R.id.text);
        // Locate the ImageView in gridview_item.xml
        ImageView image = (ImageView) vi.findViewById(R.id.imageViewItem);

        // Set file name to the TextView followed by the position
        text.setText(filename[position]);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = DetailsActivity.STORED_FOTO_WIDTH / 100;
        Bitmap scaled = BitmapFactory.decodeFile(filepath[position], options);

        if (scaled != null) {
            Log.d(TAG, "Decoded " + filepath[position]);
            image.setImageBitmap(scaled);
        } else {
            Log.e (TAG, "Cannot decode " + filepath[position]);
        }
        return vi;

    }
}
