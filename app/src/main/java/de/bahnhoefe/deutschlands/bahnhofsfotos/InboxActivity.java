package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.InboxAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.HighScoreItem;
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
        setContentView(R.layout.activity_inbox);

        final Call<List<PublicInbox>> inboxCall = ((BaseApplication)getApplication()).getRSAPI().getPublicInbox();
        inboxCall.enqueue(new Callback<List<PublicInbox>>() {
            @Override
            public void onResponse(final Call<List<PublicInbox>> call, final Response<List<PublicInbox>> response) {
                if (response.isSuccessful()) {
                    final ListView listView = findViewById(R.id.inbox_list);
                    assert listView != null;
                    adapter = new InboxAdapter(InboxActivity.this, response.body());
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        final PublicInbox inboxItem = response.body().get(position);
                        final Intent intent = new Intent(InboxActivity.this, MapsActivity.class);
                        intent.putExtra(MapsActivity.EXTRAS_LATITUDE, inboxItem.getLat());
                        intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, inboxItem.getLon());
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onFailure(final Call<List<PublicInbox>> call, final Throwable t) {
                Log.e(TAG, "Error loading public inbox", t);
                Toast.makeText(getBaseContext(), getString(R.string.error_loading_inbox) + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}