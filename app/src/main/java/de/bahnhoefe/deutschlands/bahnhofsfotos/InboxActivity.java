package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityInboxBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.InboxAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxActivity extends AppCompatActivity {

    private static final String TAG = InboxActivity.class.getSimpleName();

    private InboxAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivityInboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Call<List<PublicInbox>> inboxCall = ((BaseApplication)getApplication()).getRsapiClient().getPublicInbox();
        inboxCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PublicInbox>> call, @NonNull Response<List<PublicInbox>> response) {
                var body = response.body();
                if (response.isSuccessful() && body != null) {
                    adapter = new InboxAdapter(InboxActivity.this, body);
                    binding.inboxList.setAdapter(adapter);
                    binding.inboxList.setOnItemClickListener((parent, view, position, id) -> {
                        var inboxItem = body.get(position);
                        var intent = new Intent(InboxActivity.this, MapsActivity.class);
                        intent.putExtra(MapsActivity.EXTRAS_LATITUDE, inboxItem.getLat());
                        intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, inboxItem.getLon());
                        intent.putExtra(MapsActivity.EXTRAS_MARKER, inboxItem.getStationId() == null ? R.drawable.marker_missing : R.drawable.marker_red);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PublicInbox>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading public inbox", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_loading_inbox) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}