package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.GridViewAdapter;

public class GalleryActivity extends AppCompatActivity {
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
            Toast.makeText(this, "Error! No SDCARD Found!", Toast.LENGTH_LONG).show();
        } else {
            // Locate the image folder in the SD Card
            file = new File(Environment.getExternalStorageDirectory(), "Bahnhofsfotos");
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
                String fileName = fileNameStrings[position];
                String[] nameParts = fileName.split("[-.]");
                boolean shown = false;
                if (nameParts.length == 3) {
                    long stationId = Long.valueOf(nameParts[1]);
                    Bahnhof station = dbAdapter.fetchBahnhofByBahnhofId(stationId);
                    if (station != null) {
                        Intent detailIntent = new Intent(GalleryActivity.this, DetailsActivity.class);
                        detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF, station);
                        startActivity(detailIntent);
                        shown = true;
                    }
                }
                if (!shown) {
                    Toast.makeText(GalleryActivity.this,
                            "Kann dieses Foto keinem Bahnhof mehr zuordnen",
                            Toast.LENGTH_LONG).show();
                }
            }

        });

    }

}