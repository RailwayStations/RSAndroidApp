package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.GridViewAdapter;

public class GalleryActivity extends AppCompatActivity {
    private static final String TAG = "GalleryActivity";
    // Declare variables
    private String[] filePathStrings;
    private String[] fileNameStrings;
    private File[] listFile;
    private GridView grid;
    private GridViewAdapter adapter;
    private File file;
    private BahnhofsDbAdapter dbAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        setContentView(R.layout.activity_gallery);

        // Check for SD Card
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.error_no_sdcard_found, Toast.LENGTH_LONG).show();
        } else {
            // Locate the image folder in the SD Card
            file = new File(Environment.getExternalStorageDirectory(), Constants.PHOTO_DIRECTORY);
            // Creates a new folder if no folder with name Bahnhofsfotos exist
            file.mkdirs();
        }

        if (file.isDirectory()) {
            listFile = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !name.startsWith(".");
                }
            });
            // Creates a String array for FilePathStrings
            filePathStrings = new String[listFile.length];

            // Creates a String array for FileNameStrings
            fileNameStrings = new String[listFile.length];

            for (int i = 0; i < listFile.length; i++) {
                // Get the path of the image file
                filePathStrings[i] = listFile[i].getAbsolutePath();
                // Get the name image file
                fileNameStrings[i] = listFile[i].getName();
                //Toast.makeText(this, FileNameStrings[i], Toast.LENGTH_LONG).show();
            }
        }

        int countOfImages = listFile.length;
        String strCountOfImagesFormat = getResources().getString(R.string.count_of_images);
        String strCountOfImagesMsg = String.format(strCountOfImagesFormat, countOfImages);

        TextView tvCountOfImages = (TextView) findViewById(R.id.tvImageCount);
        tvCountOfImages.setText(strCountOfImagesMsg);

        // Locate the GridView in gridview_main.xml
        grid = (GridView) findViewById(R.id.gridview);
        // Pass String arrays to LazyAdapter Class
        adapter = new GridViewAdapter(this, filePathStrings, fileNameStrings);
        // connect LazyAdapter to the GridView
        grid.setAdapter(adapter);

        // Capture gridview item click
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fileName = fileNameStrings[position].substring(0, fileNameStrings[position].length() - 4);
                boolean shown = false;
                Intent detailIntent = new Intent(GalleryActivity.this, DetailsActivity.class);

                String[] nameParts = fileName.split("[_]");
                if (nameParts.length >= 2) {
                    if (nameParts.length == 2) {
                        String stationId = nameParts[1];
                        Bahnhof station = dbAdapter.fetchBahnhofByBahnhofId(stationId);

                        if (station != null) {
                            detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF, station);
                        }
                    } else {
                        try {
                            Double latitude = Double.parseDouble(nameParts[1]);
                            Double longitude = Double.parseDouble(nameParts[2]);
                            detailIntent.putExtra(DetailsActivity.EXTRA_LATITUDE, latitude);
                            detailIntent.putExtra(DetailsActivity.EXTRA_LONGITUDE, longitude);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Error extracting coordinates from filename: " + fileName);
                        }
                    }
                } else {
                    nameParts = fileName.split("[-]"); // fallback to old naming convention
                    if (nameParts.length == 2) {
                        String stationId = nameParts[1];
                        Bahnhof station = dbAdapter.fetchBahnhofByBahnhofId(stationId);

                        if (station != null) {
                            detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF, station);
                        }
                    }
                }

                if (detailIntent.hasExtra(DetailsActivity.EXTRA_BAHNHOF) || detailIntent.hasExtra(DetailsActivity.EXTRA_LATITUDE)) {
                    startActivity(detailIntent);
                    shown = true;
                }
                if (!shown) {
                    Toast.makeText(GalleryActivity.this,
                            R.string.cannot_associate_photo_to_station,
                            Toast.LENGTH_LONG).show();
                }
            }

        });

    }

}