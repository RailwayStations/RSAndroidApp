package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.GridViewAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutboxActivity extends AppCompatActivity {

    private static final String TAG = OutboxActivity.class.getSimpleName();

    private GridViewAdapter adapter;
    private DbAdapter dbAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        setContentView(R.layout.activity_outbox);
        final TextView tvCountOfImages = findViewById(R.id.tvImageCount);
        final GridView grid = findViewById(R.id.gridview);

        final Cursor uploadsCursor = dbAdapter.getUploadsWithStations();
        final List<LocalPhoto> files = FileUtils.getLocalPhotos(this);
        adapter = new GridViewAdapter(OutboxActivity.this, files, uploadsCursor);
        grid.setAdapter(adapter);

        tvCountOfImages.setText(String.format(getResources().getString(R.string.count_of_images), files.size()));

        // Capture gridview item click
        grid.setOnItemClickListener((parent, view, position, id) -> {
            final Upload upload = dbAdapter.getUploadById(id);
            final Intent detailIntent = new Intent(OutboxActivity.this, DetailsActivity.class);
            detailIntent.putExtra(DetailsActivity.EXTRA_UPLOAD, upload);
            startActivityForResult(detailIntent, 0);
        });

        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            final String uploadId = String.valueOf(id);
            new SimpleDialogs().confirm(OutboxActivity.this, getResources().getString(R.string.delete_upload, uploadId), (dialog, which) -> {
                dbAdapter.deleteUpload(id);
                File file = FileUtils.getStoredMediaFile(OutboxActivity.this, id);
                FileUtils.deleteQuietly(file);
                adapter.notifyDataSetChanged();
            });
            return true;
        });

        baseApplication.getRSAPI().queryUploadState(RSAPI.Helper.getAuthorizationHeader(baseApplication.getEmail(), baseApplication.getPassword()), files).enqueue(new Callback<List<UploadStateQuery>>() {
            @Override
            public void onResponse(final Call<List<UploadStateQuery>> call, final Response<List<UploadStateQuery>> response) {
                final List<UploadStateQuery> stateQueries = response.body();
                int acceptedCount = 0;
                int otherOwnerCount = 0;
                if (stateQueries != null && files.size() == stateQueries.size()) {
                    for (int i = 0; i < files.size(); i++) {
                        final UploadState state = stateQueries.get(i).getState();
                        files.get(i).setState(state);
                        if (state == UploadState.ACCEPTED) {
                            acceptedCount++;
                        }
                        if (state == UploadState.OTHER_USER) {
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
            public void onFailure(final Call<List<UploadStateQuery>> call, final Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(OutboxActivity.this,
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
                public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
                    checkedItems[which] = isChecked;
                }
            })
            .setPositiveButton(R.string.button_ok_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final List<LocalPhoto> deleted = new ArrayList<>();
                    for (final LocalPhoto photo : files) {
                        if (checkedItems[0] && photo.getState() == UploadState.ACCEPTED) {
                            FileUtils.deleteQuietly(photo.getFile());
                            deleted.add(photo);
                        }
                        if (checkedItems[1] && photo.getState() == UploadState.OTHER_USER) {
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