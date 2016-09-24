package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.bahnhoefe.deutschlands.bahnhofsfotos.GalleryActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

//import static com.google.android.gms.analytics.internal.zzy.G;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.GalleryActivity.getScreenWidth;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.R.id.imageView;

/**
 * Created by android_oma on 24.07.16.
 */

public class GridViewAdapter extends BaseAdapter {

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

            Bitmap myBitmap = BitmapFactory.decodeFile(filepath[position]);

            int width = 100;
            int height = 100;
            Log.e("Screen width ", " " + width);
            Log.e("Screen height ", " " + height);
            Log.e("img width ", " " + myBitmap.getWidth());
            Log.e("img height ", " " + myBitmap.getHeight());

            float scaleHt = (float) width / myBitmap.getWidth();
            Log.e("Scaled percent ", " " + scaleHt);
            Bitmap scaled = Bitmap.createScaledBitmap(myBitmap, width, (int) (myBitmap.getHeight() * scaleHt), true);

            image.setImageBitmap(scaled);
            return vi;

    }
}
