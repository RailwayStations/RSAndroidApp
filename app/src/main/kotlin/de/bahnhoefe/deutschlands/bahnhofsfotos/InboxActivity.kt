package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityInboxBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.InboxAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

private val TAG = InboxActivity::class.java.simpleName

@AndroidEntryPoint
class InboxActivity : AppCompatActivity() {
    private var adapter: InboxAdapter? = null

    @Inject
    lateinit var rsapiClient: RSAPIClient

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityInboxBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)
        val inboxCall = rsapiClient.getPublicInbox()
        inboxCall.enqueue(object : Callback<List<PublicInbox>> {
            override fun onResponse(
                call: Call<List<PublicInbox>>,
                response: Response<List<PublicInbox>>
            ) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    adapter = InboxAdapter(this@InboxActivity, body)
                    binding.inboxList.adapter = adapter
                    binding.inboxList.onItemClickListener =
                        OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                            val (_, _, stationId, lat, lon) = body[position]
                            val intent = Intent(this@InboxActivity, MapsActivity::class.java)
                            intent.putExtra(EXTRAS_MAPS_LATITUDE, lat)
                            intent.putExtra(EXTRAS_MAPS_LONGITUDE, lon)
                            intent.putExtra(
                                EXTRAS_MAPS_MARKER,
                                if (stationId == null) R.drawable.marker_missing else R.drawable.marker_red
                            )
                            startActivity(intent)
                        }
                }
            }

            override fun onFailure(call: Call<List<PublicInbox>>, t: Throwable) {
                Log.e(TAG, "Error loading public inbox", t)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_loading_inbox) + t.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

}