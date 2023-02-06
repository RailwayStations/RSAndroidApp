package de.bahnhoefe.deutschlands.bahnhofsfotos;

import static java.util.stream.Collectors.toList;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        var binding = ActivityOutboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new OutboxAdapter(OutboxActivity.this, dbAdapter.getOutbox());
        binding.lstUploads.setAdapter(adapter);

        // item click
        binding.lstUploads.setOnItemClickListener((parent, view, position, id) -> {
            var upload = dbAdapter.getUploadById(id);
            Intent intent;
            if (upload.isProblemReport()) {
                intent = new Intent(OutboxActivity.this, ProblemReportActivity.class);
                intent.putExtra(ProblemReportActivity.EXTRA_UPLOAD, upload);
            } else {
                intent = new Intent(OutboxActivity.this, UploadActivity.class);
                intent.putExtra(UploadActivity.EXTRA_UPLOAD, upload);
            }
            startActivity(intent);
        });

        binding.lstUploads.setOnItemLongClickListener((parent, view, position, id) -> {
            var uploadId = String.valueOf(id);
            SimpleDialogs.confirmOkCancel(OutboxActivity.this, getResources().getString(R.string.delete_upload, uploadId), (dialog, which) -> {
                dbAdapter.deleteUpload(id);
                FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, id));
                adapter.changeCursor(dbAdapter.getOutbox());
            });
            return true;
        });

        var query = dbAdapter.getPendingUploads(true).stream()
                .map(upload -> InboxStateQuery.builder().id(upload.getRemoteId()).build())
                .collect(toList());

        baseApplication.getRsapiClient().queryUploadState(query).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<InboxStateQuery>> call, @NonNull Response<List<InboxStateQuery>> response) {
                if (response.isSuccessful()) {
                    var stateQueries = response.body();
                    if (stateQueries != null) {
                        dbAdapter.updateUploadStates(stateQueries);
                        adapter.changeCursor(dbAdapter.getOutbox());
                    }
                } else if (response.code() == 401) {
                    baseApplication.setAccessToken(null);
                    baseApplication.getRsapiClient().clearToken();
                    Toast.makeText(OutboxActivity.this, R.string.authorization_failed, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(OutboxActivity.this, MyDataActivity.class));
                    finish();
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<InboxStateQuery>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
                Toast.makeText(OutboxActivity.this,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.outbox, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_delete_processed_uploads) {
            deleteCompletedUploads();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCompletedUploads() {
        var uploads = dbAdapter.getCompletedUploads();
        if (uploads.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.confirm_delete_processed_uploads)
                .setPositiveButton(R.string.button_ok_text, (dialog, which) -> {
                    for (Upload upload : uploads) {
                        dbAdapter.deleteUpload(upload.getId());
                        FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, upload.getId()));
                    }
                    adapter.changeCursor(dbAdapter.getOutbox());
                })
                .setNegativeButton(R.string.button_cancel_text, null)
                .create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        adapter.changeCursor(dbAdapter.getOutbox());
    }

}