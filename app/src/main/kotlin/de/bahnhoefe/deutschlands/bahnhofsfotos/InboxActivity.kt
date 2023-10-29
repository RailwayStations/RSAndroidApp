package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityInboxBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.InboxAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PublicInbox
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class InboxActivity : AppCompatActivity() {
    private var adapter: InboxAdapter? = null

    @Inject
    lateinit var rsapiClient: RSAPIClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInboxBinding.inflate(
            layoutInflater
        )
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
                            intent.putExtra(MapsActivity.EXTRAS_LATITUDE, lat)
                            intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, lon)
                            intent.putExtra(
                                MapsActivity.EXTRAS_MARKER,
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

    companion object {
        private val TAG = InboxActivity::class.java.simpleName
    }
}