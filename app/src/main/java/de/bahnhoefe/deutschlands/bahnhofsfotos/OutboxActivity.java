package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.OutboxAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutboxActivity extends AppCompatActivity {

    private static final String TAG = OutboxActivity.class.getSimpleName();

    private OutboxAdapter adapter;
    private DbAdapter dbAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        setContentView(R.layout.activity_outbox);
        final ListView list = findViewById(R.id.lstUploads);

        final Cursor uploadsCursor = dbAdapter.getOutbox();
        adapter = new OutboxAdapter(OutboxActivity.this, uploadsCursor);
        list.setAdapter(adapter);

        // item click
        list.setOnItemClickListener((parent, view, position, id) -> {
            final Upload upload = dbAdapter.getUploadById(id);
            final Intent detailIntent = new Intent(OutboxActivity.this, DetailsActivity.class);
            detailIntent.putExtra(DetailsActivity.EXTRA_UPLOAD, upload);
            startActivityForResult(detailIntent, 0);
        });

        list.setOnItemLongClickListener((parent, view, position, id) -> {
            final String uploadId = String.valueOf(id);
            new SimpleDialogs().confirm(OutboxActivity.this, getResources().getString(R.string.delete_upload, uploadId), (dialog, which) -> {
                dbAdapter.deleteUpload(id);
                FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, id));
                adapter.changeCursor(dbAdapter.getOutbox());
            });
            return true;
        });

        final List<InboxStateQuery> query = new ArrayList<>();
        for (final Upload upload : dbAdapter.getPendingUploads()) {
            query.add(new InboxStateQuery(upload.getRemoteId()));
        }

        baseApplication.getRSAPI().queryUploadState(RSAPI.Helper.getAuthorizationHeader(baseApplication.getEmail(), baseApplication.getPassword()), query).enqueue(new Callback<List<InboxStateQuery>>() {
            @Override
            public void onResponse(final Call<List<InboxStateQuery>> call, final Response<List<InboxStateQuery>> response) {
                final List<InboxStateQuery> stateQueries = response.body();
                if (stateQueries != null) {
                    dbAdapter.updateUploadStates(stateQueries);
                    adapter.changeCursor(dbAdapter.getOutbox());
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
                deleteCompletedUploads();
            }

            @Override
            public void onFailure(final Call<List<InboxStateQuery>> call, final Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(OutboxActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
                deleteCompletedUploads();
            }
        });
    }

    private void deleteCompletedUploads() {
        final List<Upload> uploads = dbAdapter.getCompletedUploads();
        if (uploads.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.confirm_delete_processed_uploads)
            .setPositiveButton(R.string.button_ok_text, (dialog, which) -> {
                for (final Upload upload : uploads) {
                    dbAdapter.deleteUpload(upload.getId());
                    FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, upload.getId()));
                }
                adapter.changeCursor(dbAdapter.getOutbox());
            })
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        adapter.changeCursor(dbAdapter.getOutbox());
    }

}