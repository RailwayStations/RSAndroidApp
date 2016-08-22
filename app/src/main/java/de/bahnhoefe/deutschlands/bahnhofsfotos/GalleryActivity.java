package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import de.bahnhoefe.deutschlands.bahnhofsfotos.util.GridViewAdapter;

public class GalleryActivity extends AppCompatActivity {
    // Declare variables
    private String[] FilePathStrings;
    private String[] FileNameStrings;
    private File[] listFile;
    GridView grid;
    GridViewAdapter adapter;
    File file;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Check for SD Card
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "Error! No SDCARD Found!", Toast.LENGTH_LONG)
                    .show();
        } else {
            // Locate the image folder in the SD Card
            file = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "Bahnhofsfotos");
            // Creates a new folder if no folder with name Bahnhofsfotos exist
            file.mkdirs();
        }


        if (file.isDirectory()) {
            listFile = file.listFiles();
            // Creates a String array for FilePathStrings
            FilePathStrings = new String[listFile.length];



            // Creates a String array for FileNameStrings
            FileNameStrings = new String[listFile.length];

            for (int i = 0; i < listFile.length; i++) {
                // Get the path of the image file
                FilePathStrings[i] = listFile[i].getAbsolutePath();
                // Get the name image file
                FileNameStrings[i] = listFile[i].getName();
                //Toast.makeText(this, FileNameStrings[i], Toast.LENGTH_LONG).show();
            }
        }

        int countOfImages = listFile.length;
        String strCountOfImagesFormat = getResources().getString(R.string.count_of_images);
        String strCountOfImagesMsg = String.format(strCountOfImagesFormat, countOfImages);

        TextView tvCountOfImages = (TextView)findViewById(R.id.tvImageCount);
        tvCountOfImages.setText(strCountOfImagesMsg);

        // Locate the GridView in gridview_main.xml
        grid = (GridView) findViewById(R.id.gridview);
        // Pass String arrays to LazyAdapter Class
        adapter = new GridViewAdapter(this, FilePathStrings, FileNameStrings);
        // connect LazyAdapter to the GridView
        grid.setAdapter(adapter);

        // Capture gridview item click
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent i = new Intent(GalleryActivity.this, ViewImageActivity.class);
                // Pass String arrays FilePathStrings
                i.putExtra("filepath", FilePathStrings);
                // Pass String arrays FileNameStrings
                i.putExtra("filename", FileNameStrings);
                // Pass click position
                i.putExtra("position", position);
                startActivity(i);

            }

        });

    }
    public static float getScreenWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float pxWidth = outMetrics.widthPixels;
        return pxWidth;
    }
}