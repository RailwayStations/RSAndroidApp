package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ReportProblemBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProblemReportActivity extends AppCompatActivity {

    private static final String TAG = ProblemReportActivity.class.getSimpleName();

    public static final String EXTRA_UPLOAD = "EXTRA_UPLOAD";
    public static final String EXTRA_STATION = "EXTRA_STATION";
    public static final String EXTRA_PHOTO_ID = "EXTRA_PHOTO_ID";

    private BaseApplication baseApplication;
    private RSAPIClient rsapiClient;
    private ReportProblemBinding binding;

    private Upload upload;
    private Station station;
    private Long photoId;
    private final ArrayList<String> problemTypes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ReportProblemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        baseApplication = (BaseApplication) getApplication();
        rsapiClient = baseApplication.getRsapiClient();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        problemTypes.add(getString(R.string.problem_please_specify));
        for (var type : ProblemType.values()) {
            problemTypes.add(getString(type.getMessageId()));
        }
        var adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, problemTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.problemType.setAdapter(adapter);

        binding.problemType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                if (position > 0) {
                    var type = ProblemType.values()[position - 1];
                    setCoordsVisible(type == ProblemType.WRONG_LOCATION);
                    setTitleVisible(type == ProblemType.WRONG_NAME);
                } else {
                    setCoordsVisible(false);
                    setTitleVisible(false);
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                setCoordsVisible(false);
                setTitleVisible(false);
            }
        });

        if (!baseApplication.isLoggedIn()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show();
            startActivity(new Intent(ProblemReportActivity.this, MyDataActivity.class));
            finish();
            return;
        }

        if (!baseApplication.getProfile().getEmailVerified()) {
            SimpleDialogs.confirmOk(this, R.string.email_unverified_for_problem_report, (dialog, view) -> {
                startActivity(new Intent(ProblemReportActivity.this, MyDataActivity.class));
                finish();
            });
            return;
        }

        onNewIntent(getIntent());
    }

    private void setCoordsVisible(boolean visible) {
        binding.tvNewCoords.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.etNewLatitude.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.etNewLongitude.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setTitleVisible(boolean visible) {
        binding.tvNewTitle.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.etNewTitle.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            upload = (Upload) intent.getSerializableExtra(EXTRA_UPLOAD);
            station = (Station) intent.getSerializableExtra(EXTRA_STATION);
            photoId = (Long) intent.getSerializableExtra(EXTRA_PHOTO_ID);

            if (upload != null && upload.isProblemReport()) {
                binding.etProblemComment.setText(upload.getComment());
                binding.etNewLatitude.setText(upload.getLat() != null ? upload.getLat().toString() : "");
                binding.etNewLongitude.setText(upload.getLon() != null ? upload.getLon().toString() : "");

                int selected = upload.getProblemType().ordinal() + 1;
                binding.problemType.setSelection(selected);

                if (station == null) {
                    station = baseApplication.getDbAdapter().getStationForUpload(upload);
                }

                fetchUploadStatus(upload);
            }

            if (station != null) {
                binding.tvStationTitle.setText(station.getTitle());
                binding.etNewTitle.setText(station.getTitle());
                binding.etNewLatitude.setText(String.valueOf(station.getLat()));
                binding.etNewLongitude.setText(String.valueOf(station.getLon()));
            }
        }
    }

    public void reportProblem(View view) {
        int selectedType = binding.problemType.getSelectedItemPosition();
        if (selectedType == 0) {
            Toast.makeText(getApplicationContext(), getString(R.string.problem_please_specify), Toast.LENGTH_LONG).show();
            return;
        }
        var type = ProblemType.values()[selectedType - 1];
        var comment = binding.etProblemComment.getText().toString();
        if (StringUtils.isBlank(comment)) {
            Toast.makeText(getApplicationContext(), getString(R.string.problem_please_comment), Toast.LENGTH_LONG).show();
            return;
        }

        Double lat = null;
        Double lon = null;
        if (binding.etNewLatitude.getVisibility() == View.VISIBLE) {
            lat = parseDouble(binding.etNewLatitude);
        }
        if (binding.etNewLongitude.getVisibility() == View.VISIBLE) {
            lon = parseDouble(binding.etNewLongitude);
        }
        if (type == ProblemType.WRONG_LOCATION && (lat == null || lon == null)) {
            Toast.makeText(getApplicationContext(), getString(R.string.problem_wrong_lat_lon), Toast.LENGTH_LONG).show();
            return;
        }
        var title = binding.etNewTitle.getText().toString();
        if (type == ProblemType.WRONG_NAME && (StringUtils.isBlank(title) || Objects.equals(station.getTitle(), title))) {
            Toast.makeText(getApplicationContext(), getString(R.string.problem_please_provide_corrected_title), Toast.LENGTH_LONG).show();
            return;
        }

        upload = new Upload(
                null,
                station.getCountry(),
                station.getId(),
                null,
                title,
                lat,
                lon,
                comment,
                null,
                type
        );

        upload = baseApplication.getDbAdapter().insertUpload(upload);

        var problemReport = new ProblemReport(
                station.getCountry(),
                station.getId(),
                comment,
                type,
                photoId,
                lat,
                lon,
                title);

        SimpleDialogs.confirmOkCancel(ProblemReportActivity.this, R.string.send_problem_report,
                (dialog, which) -> rsapiClient.reportProblem(problemReport).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<InboxResponse> call, @NonNull Response<InboxResponse> response) {
                        InboxResponse inboxResponse;
                        if (response.isSuccessful()) {
                            inboxResponse = response.body();
                        } else if (response.code() == 401) {
                            onUnauthorized();
                            return;
                        } else {
                            inboxResponse = new Gson().fromJson(response.errorBody().charStream(), InboxResponse.class);
                        }

                        upload.setRemoteId(inboxResponse.getId());
                        upload.setUploadState(inboxResponse.getState().getUploadState());
                        baseApplication.getDbAdapter().updateUpload(upload);
                        if (inboxResponse.getState() == InboxResponse.InboxResponseState.ERROR) {
                            SimpleDialogs.confirmOk(ProblemReportActivity.this,
                                    getString(InboxResponse.InboxResponseState.ERROR.getMessageId(), inboxResponse.getMessage()));
                        } else {
                            SimpleDialogs.confirmOk(ProblemReportActivity.this, inboxResponse.getState().getMessageId());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<InboxResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Error reporting problem", t);
                    }
                }));
    }

    private void onUnauthorized() {
        baseApplication.setAccessToken(null);
        rsapiClient.clearToken();
        Toast.makeText(ProblemReportActivity.this, R.string.authorization_failed, Toast.LENGTH_LONG).show();
        startActivity(new Intent(ProblemReportActivity.this, MyDataActivity.class));
        finish();
    }

    private Double parseDouble(final EditText editText) {
        try {
            return Double.parseDouble(String.valueOf(editText.getText()));
        } catch (Exception e) {
            Log.e(TAG, "error parsing double " + editText.getText(), e);
        }
        return null;
    }

    private void fetchUploadStatus(Upload upload) {
        if (upload == null || upload.getRemoteId() == null) {
            return;
        }

        var stateQuery = new InboxStateQuery(
                upload.getRemoteId(),
                upload.getCountry(),
                upload.getStationId());

        rsapiClient.queryUploadState(List.of(stateQuery)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<InboxStateQuery>> call, @NonNull Response<List<InboxStateQuery>> response) {
                if (response.isSuccessful()) {
                    var stateQueries = response.body();
                    if (stateQueries != null && !stateQueries.isEmpty()) {
                        var stateQuery = stateQueries.get(0);
                        binding.uploadStatus.setText(getString(R.string.upload_state, getString(stateQuery.getState().getTextId())));
                        binding.uploadStatus.setTextColor(getResources().getColor(stateQuery.getState().getColorId(), null));
                        upload.setUploadState(stateQuery.getState());
                        upload.setRejectReason(stateQuery.getRejectedReason());
                        upload.setCrc32(stateQuery.getCrc32());
                        upload.setRemoteId(stateQuery.getId());
                        baseApplication.getDbAdapter().updateUpload(upload);
                    }
                } else if (response.code() == 401) {
                    onUnauthorized();
                } else {
                    Log.w(TAG, "Upload states not processable");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<InboxStateQuery>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error retrieving upload state", t);
            }
        });

    }

    @Override
    public void onBackPressed() {
        navigateUp();
    }

    public void navigateUp() {
        var callingActivity = getCallingActivity(); // if MapsActivity was calling, then we don't want to rebuild the Backstack
        if (callingActivity == null) {
            var upIntent = NavUtils.getParentActivityIntent(this);
            assert upIntent != null;
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
                Log.v(TAG, "Recreate back stack");
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
            }
        }

        finish();
    }

}
