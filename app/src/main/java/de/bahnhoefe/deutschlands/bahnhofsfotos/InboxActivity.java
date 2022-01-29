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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityInboxBinding binding = ActivityInboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Call<List<PublicInbox>> inboxCall = ((BaseApplication)getApplication()).getRsapiClient().getPublicInbox();
        inboxCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull final Call<List<PublicInbox>> call, @NonNull final Response<List<PublicInbox>> response) {
                final List<PublicInbox> body = response.body();
                if (response.isSuccessful() && body != null) {
                    adapter = new InboxAdapter(InboxActivity.this, body);
                    binding.inboxList.setAdapter(adapter);
                    binding.inboxList.setOnItemClickListener((parent, view, position, id) -> {
                        final PublicInbox inboxItem = body.get(position);
                        final Intent intent = new Intent(InboxActivity.this, MapsActivity.class);
                        intent.putExtra(MapsActivity.EXTRAS_LATITUDE, inboxItem.getLat());
                        intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, inboxItem.getLon());
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull final Call<List<PublicInbox>> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error loading public inbox", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_loading_inbox) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}