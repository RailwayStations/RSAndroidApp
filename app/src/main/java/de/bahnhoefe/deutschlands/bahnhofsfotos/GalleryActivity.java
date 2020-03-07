package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.GridViewAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = GalleryActivity.class.getSimpleName();

    private GridView grid;
    private GridViewAdapter adapter;
    private BahnhofsDbAdapter dbAdapter;
    private TextView tvCountOfImages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        setContentView(R.layout.activity_gallery);
        tvCountOfImages = findViewById(R.id.tvImageCount);

        grid = findViewById(R.id.gridview);

        final List<LocalPhoto> files = FileUtils.getLocalPhotos(this);
        adapter = new GridViewAdapter(GalleryActivity.this, files);
        grid.setAdapter(adapter);

        tvCountOfImages.setText(String.format(getResources().getString(R.string.count_of_images), files.size()));

        // Capture gridview item click
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocalPhoto photo = files.get(position);
                boolean shown = false;
                Intent detailIntent = new Intent(GalleryActivity.this, DetailsActivity.class);

                if (photo.getId() != null) {
                    Bahnhof station = dbAdapter.getBahnhofByKey(photo.getCountryCode(), photo.getId());

                    if (station != null) {
                        detailIntent.putExtra(DetailsActivity.EXTRA_BAHNHOF, station);
                    }
                } else if (photo.hasCoords()) {
                    detailIntent.putExtra(DetailsActivity.EXTRA_LATITUDE, photo.getLat());
                    detailIntent.putExtra(DetailsActivity.EXTRA_LONGITUDE, photo.getLon());
                }

                if (detailIntent.hasExtra(DetailsActivity.EXTRA_BAHNHOF) || detailIntent.hasExtra(DetailsActivity.EXTRA_LATITUDE)) {
                    startActivityForResult(detailIntent, 0);
                    shown = true;
                }
                if (!shown) {
                    Toast.makeText(GalleryActivity.this,
                            R.string.cannot_associate_photo_to_station,
                            Toast.LENGTH_LONG).show();
                }
            }

        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final LocalPhoto photo = files.get(position);
                new SimpleDialogs().confirm(GalleryActivity.this, getResources().getString(R.string.delete_local_photo, photo.getDisplayName()), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileUtils.deleteQuietly(photo.getFile());
                        files.remove(photo);
                        adapter.notifyDataSetChanged();
                    }
                });
                return true;
            }
        });

        baseApplication.getRSAPI().queryUploadState(RSAPI.Helper.getAuthorizationHeader(baseApplication.getEmail(), baseApplication.getPassword()), files).enqueue(new Callback<List<UploadStateQuery>>() {
            @Override
            public void onResponse(Call<List<UploadStateQuery>> call, Response<List<UploadStateQuery>> response) {
                List<UploadStateQuery> stateQueries = response.body();
                int acceptedCount = 0;
                int otherOwnerCount = 0;
                if (stateQueries != null && files.size() == stateQueries.size()) {
                    for (int i = 0; i < files.size(); i++) {
                        UploadStateQuery.UploadStateState state = stateQueries.get(i).getState();
                        files.get(i).setState(state);
                        if (state == UploadStateQuery.UploadStateState.ACCEPTED) {
                            acceptedCount++;
                        }
                        if (state == UploadStateQuery.UploadStateState.OTHER_USER) {
                            otherOwnerCount++;
                        }
                    }
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
                adapter.notifyDataSetChanged();
                if (acceptedCount > 0 || otherOwnerCount > 0) {
                    deleteObsoleteLocalPhotosWithConfirmation(files);
                }
            }

            @Override
            public void onFailure(Call<List<UploadStateQuery>> call, Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(GalleryActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteObsoleteLocalPhotosWithConfirmation(final List<LocalPhoto> files) {
        final boolean[] checkedItems = new boolean[2];
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.confirm_delete_local_photos)
            .setMultiChoiceItems(R.array.local_photo_delete_options, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checkedItems[which] = isChecked;
                }
            })
            .setPositiveButton(R.string.button_ok_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    List<LocalPhoto> deleted = new ArrayList<>();
                    for (LocalPhoto photo : files) {
                        if (checkedItems[0] && photo.getState() == UploadStateQuery.UploadStateState.ACCEPTED) {
                            FileUtils.deleteQuietly(photo.getFile());
                            deleted.add(photo);
                        }
                        if (checkedItems[1] && photo.getState() == UploadStateQuery.UploadStateState.OTHER_USER) {
                            FileUtils.deleteQuietly(photo.getFile());
                            deleted.add(photo);
                        }
                    }
                    files.removeAll(deleted);
                    adapter.notifyDataSetChanged();
                }
            })
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show();
    }

}