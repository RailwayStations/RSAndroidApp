package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.app.TaskStackBuilder
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.gson.Gson
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ReportProblemBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemReport
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import org.apache.commons.lang3.StringUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Objects

class ProblemReportActivity : AppCompatActivity() {
    private var baseApplication: BaseApplication? = null
    private var rsapiClient: RSAPIClient? = null
    private var binding: ReportProblemBinding? = null
    private var upload: Upload? = null
    private var station: Station? = null
    private var photoId: Long? = null
    private val problemTypes = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ReportProblemBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)
        baseApplication = application as BaseApplication
        rsapiClient = baseApplication.getRsapiClient()
        Objects.requireNonNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        problemTypes.add(getString(R.string.problem_please_specify))
        for (type in ProblemType.values()) {
            problemTypes.add(getString(type.messageId))
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, problemTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding!!.problemType.adapter = adapter
        binding!!.problemType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    val type = ProblemType.values()[position - 1]
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
        if (!baseApplication!!.isLoggedIn) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show()
            startActivity(Intent(this@ProblemReportActivity, MyDataActivity::class.java))
            finish()
            return
        }
        if (!baseApplication.getProfile().emailVerified) {
            SimpleDialogs.confirmOk(
                this,
                R.string.email_unverified_for_problem_report
            ) { dialog: DialogInterface?, view: Int ->
                startActivity(Intent(this@ProblemReportActivity, MyDataActivity::class.java))
                finish()
            }
            return
        }
        onNewIntent(intent)
    }

    private fun setCoordsVisible(visible: Boolean) {
        binding!!.tvNewCoords.visibility = if (visible) View.VISIBLE else View.GONE
        binding!!.etNewLatitude.visibility = if (visible) View.VISIBLE else View.GONE
        binding!!.etNewLongitude.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setTitleVisible(visible: Boolean) {
        binding!!.tvNewTitle.visibility = if (visible) View.VISIBLE else View.GONE
        binding!!.etNewTitle.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null) {
            upload = intent.getSerializableExtra(EXTRA_UPLOAD) as Upload?
            station = intent.getSerializableExtra(EXTRA_STATION) as Station?
            photoId = intent.getSerializableExtra(EXTRA_PHOTO_ID) as Long?
            if (upload != null && upload!!.isProblemReport) {
                binding!!.etProblemComment.setText(upload!!.comment)
                binding!!.etNewLatitude.setText(if (upload!!.lat != null) upload!!.lat.toString() else "")
                binding!!.etNewLongitude.setText(if (upload!!.lon != null) upload!!.lon.toString() else "")
                val selected = upload!!.problemType!!.ordinal + 1
                binding!!.problemType.setSelection(selected)
                if (station == null) {
                    station = baseApplication.getDbAdapter().getStationForUpload(upload)
                }
                fetchUploadStatus(upload)
            }
            if (station != null) {
                binding!!.tvStationTitle.text = station!!.title
                binding!!.etNewTitle.setText(station!!.title)
                binding!!.etNewLatitude.setText(station!!.lat.toString())
                binding!!.etNewLongitude.setText(station!!.lon.toString())
            }
        }
    }

    fun reportProblem(view: View?) {
        val selectedType = binding!!.problemType.selectedItemPosition
        if (selectedType == 0) {
            Toast.makeText(
                applicationContext,
                getString(R.string.problem_please_specify),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val type = ProblemType.values()[selectedType - 1]
        val comment = binding!!.etProblemComment.text.toString()
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
        if (binding!!.etNewLatitude.visibility == View.VISIBLE) {
            lat = parseDouble(binding!!.etNewLatitude)
        }
        if (binding!!.etNewLongitude.visibility == View.VISIBLE) {
            lon = parseDouble(binding!!.etNewLongitude)
        }
        if (type === ProblemType.WRONG_LOCATION && (lat == null || lon == null)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.problem_wrong_lat_lon),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val title = binding!!.etNewTitle.text.toString()
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
        upload = baseApplication.getDbAdapter().insertUpload(upload)
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
        ) { dialog: DialogInterface?, which: Int ->
            rsapiClient!!.reportProblem(problemReport)!!
                .enqueue(object : Callback<InboxResponse?> {
                    override fun onResponse(
                        call: Call<InboxResponse?>,
                        response: Response<InboxResponse?>
                    ) {
                        val inboxResponse: InboxResponse?
                        inboxResponse = if (response.isSuccessful) {
                            response.body()
                        } else if (response.code() == 401) {
                            onUnauthorized()
                            return@confirmOkCancel
                        } else {
                            Gson().fromJson<InboxResponse>(
                                response.errorBody()!!.charStream(),
                                InboxResponse::class.java
                            )
                        }
                        upload!!.remoteId = inboxResponse!!.id
                        upload!!.uploadState = inboxResponse.state.uploadState
                        baseApplication.getDbAdapter().updateUpload(upload)
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
        baseApplication.setAccessToken(null)
        rsapiClient!!.clearToken()
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
        if (upload == null || upload.remoteId == null) {
            return
        }
        val stateQuery = InboxStateQuery(
            upload.remoteId,
            upload.country,
            upload.stationId
        )
        rsapiClient!!.queryUploadState(java.util.List.of(stateQuery))!!
            .enqueue(object : Callback<List<InboxStateQuery>?> {
                override fun onResponse(
                    call: Call<List<InboxStateQuery>?>,
                    response: Response<List<InboxStateQuery>?>
                ) {
                    if (response.isSuccessful) {
                        val stateQueries = response.body()
                        if (stateQueries != null && !stateQueries.isEmpty()) {
                            val stateQuery = stateQueries[0]
                            binding!!.uploadStatus.text =
                                getString(R.string.upload_state, getString(stateQuery.state.textId))
                            binding!!.uploadStatus.setTextColor(
                                resources.getColor(
                                    stateQuery.state.colorId,
                                    null
                                )
                            )
                            upload.uploadState = stateQuery.state
                            upload.rejectReason = stateQuery.rejectedReason
                            upload.crc32 = stateQuery.crc32
                            upload.remoteId = stateQuery.id
                            baseApplication.getDbAdapter().updateUpload(upload)
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

    override fun onBackPressed() {
        navigateUp()
    }

    fun navigateUp() {
        val callingActivity =
            callingActivity // if MapsActivity was calling, then we don't want to rebuild the Backstack
        if (callingActivity == null) {
            val upIntent = NavUtils.getParentActivityIntent(this)!!
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot) {
                Log.v(TAG, "Recreate back stack")
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent)
                    .startActivities()
            }
        }
        finish()
    }

    companion object {
        private val TAG = ProblemReportActivity::class.java.simpleName
        const val EXTRA_UPLOAD = "EXTRA_UPLOAD"
        const val EXTRA_STATION = "EXTRA_STATION"
        const val EXTRA_PHOTO_ID = "EXTRA_PHOTO_ID"
    }
}