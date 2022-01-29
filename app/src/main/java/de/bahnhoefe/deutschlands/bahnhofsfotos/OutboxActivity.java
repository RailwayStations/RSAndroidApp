package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityOutboxBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.OutboxAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
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

        final ActivityOutboxBinding binding = ActivityOutboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new OutboxAdapter(OutboxActivity.this, dbAdapter.getOutbox());
        binding.lstUploads.setAdapter(adapter);

        // item click
        binding.lstUploads.setOnItemClickListener((parent, view, position, id) -> {
            final Upload upload = dbAdapter.getUploadById(id);
            final Intent detailIntent = new Intent(OutboxActivity.this, DetailsActivity.class);
            detailIntent.putExtra(DetailsActivity.EXTRA_UPLOAD, upload);
            startActivity(detailIntent);
        });

        binding.lstUploads.setOnItemLongClickListener((parent, view, position, id) -> {
            final String uploadId = String.valueOf(id);
            new SimpleDialogs().confirm(OutboxActivity.this, getResources().getString(R.string.delete_upload, uploadId), (dialog, which) -> {
                dbAdapter.deleteUpload(id);
                FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, id));
                adapter.changeCursor(dbAdapter.getOutbox());
            });
            return true;
        });

        final List<InboxStateQuery> query = new ArrayList<>();
        for (final Upload upload : dbAdapter.getPendingUploads(true)) {
            query.add(new InboxStateQuery(upload.getRemoteId()));
        }

        baseApplication.getRsapiClient().queryUploadState(query).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull final Call<List<InboxStateQuery>> call, @NonNull final Response<List<InboxStateQuery>> response) {
                final List<InboxStateQuery> stateQueries = response.body();
                if (stateQueries != null) {
                    dbAdapter.updateUploadStates(stateQueries);
                    adapter.changeCursor(dbAdapter.getOutbox());
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(@NonNull final Call<List<InboxStateQuery>> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(OutboxActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.outbox, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.nav_delete_processed_uploads) {
            deleteCompletedUploads();
            return true;
        }
        return super.onOptionsItemSelected(item);
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