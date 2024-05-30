package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ReportProblemBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs.confirmOk
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import org.apache.commons.lang3.StringUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

private val TAG = ProblemReportActivity::class.java.simpleName
const val EXTRA_PROBLEM_UPLOAD = "EXTRA_PROBLEM_UPLOAD"
const val EXTRA_PROBLEM_STATION = "EXTRA_PROBLEM_STATION"
const val EXTRA_PROBLEM_PHOTO_ID = "EXTRA_PROBLEM_PHOTO_ID"

@AndroidEntryPoint
class ProblemReportActivity : AppCompatActivity() {

    @Inject
    lateinit var dbAdapter: DbAdapter

    @Inject
    lateinit var preferencesService: PreferencesService

    @Inject
    lateinit var rsapiClient: RSAPIClient

    private lateinit var binding: ReportProblemBinding
    private var upload: Upload? = null
    private var station: Station? = null
    private var photoId: Long? = null
    private val problemTypes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ReportProblemBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        problemTypes.add(getString(R.string.problem_please_specify))
        for (type in ProblemType.entries) {
            problemTypes.add(getString(type.messageId))
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, problemTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.problemType.adapter = adapter
        binding.problemType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    val type = ProblemType.entries[position - 1]
                    setCoordsVisible(type === ProblemType.WRONG_LOCATION)
                    setTitleVisible(type === ProblemType.WRONG_NAME)
                } else {
                    setCoordsVisible(false)
                    setTitleVisible(false)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setCoordsVisible(false)
                setTitleVisible(false)
            }
        }
        if (!rsapiClient.isLoggedIn) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show()
            startActivity(Intent(this@ProblemReportActivity, MyDataActivity::class.java))
            finish()
            return
        }
        if (!preferencesService.profile.emailVerified) {
            confirmOk(
                this,
                R.string.email_unverified_for_problem_report
            ) { _: DialogInterface?, _: Int ->
                startActivity(Intent(this@ProblemReportActivity, MyDataActivity::class.java))
                finish()
            }
            return
        }
        binding.buttonReportProblem.setOnClickListener { reportProblem() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        onNewIntent(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setCoordsVisible(visible: Boolean) {
        binding.tvNewCoords.visibility = if (visible) View.VISIBLE else View.GONE
        binding.etNewLatitude.visibility = if (visible) View.VISIBLE else View.GONE
        binding.etNewLongitude.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setTitleVisible(visible: Boolean) {
        binding.tvNewTitle.visibility = if (visible) View.VISIBLE else View.GONE
        binding.etNewTitle.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(EXTRA_PROBLEM_PHOTO_ID)) {
            photoId = intent.getLongExtra(EXTRA_PROBLEM_PHOTO_ID, -1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            upload = intent.getSerializableExtra(EXTRA_PROBLEM_UPLOAD, Upload::class.java)
            station = intent.getSerializableExtra(EXTRA_PROBLEM_STATION, Station::class.java)
        } else {
            @Suppress("DEPRECATION")
            upload = intent.getSerializableExtra(EXTRA_PROBLEM_UPLOAD) as Upload?
            @Suppress("DEPRECATION")
            station = intent.getSerializableExtra(EXTRA_PROBLEM_STATION) as Station?
        }
        if (upload != null && upload!!.isProblemReport) {
            binding.etProblemComment.setText(upload!!.comment)
            binding.etNewLatitude.setText(if (upload!!.lat != null) upload!!.lat.toString() else "")
            binding.etNewLongitude.setText(if (upload!!.lon != null) upload!!.lon.toString() else "")
            val selected = upload!!.problemType!!.ordinal + 1
            binding.problemType.setSelection(selected)
            if (station == null) {
                station = dbAdapter.getStationForUpload(upload!!)
            }
            fetchUploadStatus(upload)
        }
        if (station != null) {
            binding.tvStationTitle.text = station!!.title
            binding.etNewTitle.setText(station!!.title)
            binding.etNewLatitude.setText(station!!.lat.toString())
            binding.etNewLongitude.setText(station!!.lon.toString())
        }
    }

    private fun reportProblem() {
        val selectedType = binding.problemType.selectedItemPosition
        if (selectedType == 0) {
            Toast.makeText(
                applicationContext,
                getString(R.string.problem_please_specify),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val type = ProblemType.entries[selectedType - 1]
        val comment = binding.etProblemComment.text.toString()
        if (StringUtils.isBlank(comment)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.problem_please_comment),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        var lat: Double? = null
        var lon: Double? = null
        if (binding.etNewLatitude.visibility == View.VISIBLE) {
            lat = parseDouble(binding.etNewLatitude)
        }
        if (binding.etNewLongitude.visibility == View.VISIBLE) {
            lon = parseDouble(binding.etNewLongitude)
        }
        if (type === ProblemType.WRONG_LOCATION && (lat == null || lon == null)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.problem_wrong_lat_lon),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val title = binding.etNewTitle.text.toString()
        if (type === ProblemType.WRONG_NAME && (StringUtils.isBlank(title) || station!!.title == title)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.problem_please_provide_corrected_title),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        upload = Upload(
            null,
            station!!.country,
            station!!.id,
            null,
            title,
            lat,
            lon,
            comment,
            null,
            type
        )
        upload = dbAdapter.insertUpload(upload!!)
        val problemReport = ProblemReport(
            station!!.country,
            station!!.id,
            comment,
            type,
            photoId,
            lat,
            lon,
            title
        )
        SimpleDialogs.confirmOkCancel(
            this@ProblemReportActivity, R.string.send_problem_report
        ) { _: DialogInterface?, _: Int ->
            rsapiClient.reportProblem(problemReport)
                .enqueue(object : Callback<InboxResponse> {
                    override fun onResponse(
                        call: Call<InboxResponse>,
                        response: Response<InboxResponse>
                    ) {
                        val inboxResponse: InboxResponse = if (response.isSuccessful) {
                            response.body()!!
                        } else if (response.code() == 401) {
                            onUnauthorized()
                            return
                        } else {
                            Gson().fromJson(
                                response.errorBody()!!.charStream(),
                                InboxResponse::class.java
                            )
                        }
                        upload!!.remoteId = inboxResponse.id
                        upload!!.uploadState = inboxResponse.state.uploadState
                        dbAdapter.updateUpload(upload!!)
                        if (inboxResponse.state === InboxResponse.InboxResponseState.ERROR) {
                            confirmOk(
                                this@ProblemReportActivity,
                                getString(
                                    InboxResponse.InboxResponseState.ERROR.messageId,
                                    inboxResponse.message
                                )
                            )
                        } else {
                            confirmOk(this@ProblemReportActivity, inboxResponse.state.messageId)
                        }
                    }

                    override fun onFailure(call: Call<InboxResponse?>, t: Throwable) {
                        Log.e(TAG, "Error reporting problem", t)
                    }
                })
        }
    }

    private fun onUnauthorized() {
        rsapiClient.clearToken()
        Toast.makeText(this@ProblemReportActivity, R.string.authorization_failed, Toast.LENGTH_LONG)
            .show()
        startActivity(Intent(this@ProblemReportActivity, MyDataActivity::class.java))
        finish()
    }

    private fun parseDouble(editText: EditText): Double? {
        try {
            return editText.text.toString().toDouble()
        } catch (e: Exception) {
            Log.e(TAG, "error parsing double " + editText.text, e)
        }
        return null
    }

    private fun fetchUploadStatus(upload: Upload?) {
        if (upload?.remoteId == null) {
            return
        }
        val stateQuery = InboxStateQuery(
            upload.remoteId,
            upload.country,
            upload.stationId
        )
        rsapiClient.queryUploadState(listOf(stateQuery))
            .enqueue(object : Callback<List<InboxStateQuery>> {
                override fun onResponse(
                    call: Call<List<InboxStateQuery>>,
                    response: Response<List<InboxStateQuery>>
                ) {
                    if (response.isSuccessful) {
                        val remoteStateQueries = response.body()
                        if (!remoteStateQueries.isNullOrEmpty()) {
                            val remoteStateQuery = remoteStateQueries[0]
                            binding.uploadStatus.text =
                                getString(
                                    R.string.upload_state,
                                    getString(remoteStateQuery.state.textId)
                                )
                            binding.uploadStatus.setTextColor(
                                resources.getColor(
                                    remoteStateQuery.state.colorId,
                                    null
                                )
                            )
                            upload.uploadState = remoteStateQuery.state
                            upload.rejectReason = remoteStateQuery.rejectedReason
                            upload.crc32 = remoteStateQuery.crc32
                            upload.remoteId = remoteStateQuery.id
                            dbAdapter.updateUpload(upload)
                        }
                    } else if (response.code() == 401) {
                        onUnauthorized()
                    } else {
                        Log.w(TAG, "Upload states not processable")
                    }
                }

                override fun onFailure(call: Call<List<InboxStateQuery>?>, t: Throwable) {
                    Log.e(TAG, "Error retrieving upload state", t)
                }
            })
    }
}
