package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewImageActivity extends AppCompatActivity {


    private static final String TAG = ViewImageActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Declare Variable
        TextView text;
        ImageView imageview;

        super.onCreate(savedInstanceState);
        // Get the view from view_image.xml
        setContentView(R.layout.view_image);


        // Retrieve data from MainActivity on GridView item click
        Intent i = getIntent();

        // Get the position
        int position = i.getExtras().getInt("position");

        // Get String arrays FilePathStrings
        String[] filepath = i.getStringArrayExtra("filepath");

        // Get String arrays FileNameStrings
        String[] filename = i.getStringArrayExtra("filename");

        // Locate the TextView in view_image.xml
        text = (TextView) findViewById(R.id.imagetext);

        // Locate the ImageView in view_image.xml
        imageview = (ImageView) findViewById(R.id.full_image_view);
        if (imageview == null)
            throw new IllegalStateException("Could not find full_image_view");

        // Load the text into the TextView followed by the position
        text.setText(filename[position]);

        Bitmap scaled = BitmapFactory.decodeFile(filepath[position]);

        Log.e(TAG, "img width " + scaled.getWidth());

        imageview.setImageBitmap(scaled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
