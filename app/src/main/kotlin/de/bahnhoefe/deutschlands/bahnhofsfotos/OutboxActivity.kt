package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityOutboxBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.OutboxAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

private val TAG = OutboxActivity::class.java.simpleName

@AndroidEntryPoint
class OutboxActivity : AppCompatActivity() {
    private lateinit var adapter: OutboxAdapter

    @Inject
    lateinit var dbAdapter: DbAdapter

    @Inject
    lateinit var rsapiClient: RSAPIClient

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityOutboxBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)

        if (!rsapiClient.isLoggedIn) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show()
            startActivity(Intent(this@OutboxActivity, MyDataActivity::class.java))
            finish()
            return
        }

        adapter = OutboxAdapter(this@OutboxActivity, dbAdapter.outbox)
        binding.lstUploads.adapter = adapter

        binding.lstUploads.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
                val upload = dbAdapter.getUploadById(id)
                val intent: Intent
                if (upload!!.isProblemReport) {
                    intent = Intent(this@OutboxActivity, ProblemReportActivity::class.java)
                    intent.putExtra(EXTRA_PROBLEM_UPLOAD, upload)
                } else {
                    intent = Intent(this@OutboxActivity, UploadActivity::class.java)
                    intent.putExtra(EXTRA_UPLOAD_UPLOAD, upload)
                }
                startActivity(intent)
            }
        binding.lstUploads.onItemLongClickListener =
            OnItemLongClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
                dbAdapter.getUploadById(id)?.let { deleteUploadEntry(it) }
                true
            }

        val query = dbAdapter.getPendingUploads(true)
            .map { upload: Upload? ->
                InboxStateQuery(
                    upload!!.remoteId
                )
            }

        rsapiClient.queryUploadState(query)
            .enqueue(object : Callback<List<InboxStateQuery>> {
                override fun onResponse(
                    call: Call<List<InboxStateQuery>>,
                    response: Response<List<InboxStateQuery>>
                ) {
                    if (response.isSuccessful) {
                        val stateQueries = response.body()
                        if (stateQueries != null) {
                            dbAdapter.updateUploadStates(stateQueries)
                            adapter.changeCursor(dbAdapter.outbox)
                        }
                    } else if (response.code() == 401) {
                        rsapiClient.clearToken()
                        Toast.makeText(
                            this@OutboxActivity,
                            R.string.authorization_failed,
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this@OutboxActivity, MyDataActivity::class.java))
                        finish()
                    } else {
                        Log.w(TAG, "Upload states not processable")
                    }
                }

                override fun onFailure(call: Call<List<InboxStateQuery>>, t: Throwable) {
                    Log.e(TAG, "Error retrieving upload state", t)
                    Toast.makeText(
                        this@OutboxActivity,
                        R.string.error_retrieving_upload_state,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun deleteUploadEntry(upload: Upload) {
        val remoteDelete = (upload.remoteId != null && upload.uploadState.isPending)
        val message =
            if (remoteDelete) {
                getString(R.string.delete_outbox_entry_remote, upload.remoteId)
            } else {
                getString(R.string.delete_outbox_entry_local, upload.id)
            }

        SimpleDialogs.confirmOkCancel(this@OutboxActivity, message) { _: DialogInterface?, _: Int ->
            if (remoteDelete) {
                rsapiClient.deleteInboxEntry(upload.remoteId!!).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            deleteUploadEntryLocally(upload.id!!)
                        } else if (response.code() == 401) {
                            rsapiClient.clearToken()
                            Toast.makeText(
                                this@OutboxActivity,
                                R.string.authorization_failed,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@OutboxActivity,
                                getString(
                                    R.string.error_deleting_inbox_entry,
                                    "${response.code()}: ${response.message()}"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e(TAG, "Error deleting remote inbox entry", t)
                        Toast.makeText(
                            this@OutboxActivity,
                            getString(R.string.error_deleting_inbox_entry, t.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })
            } else {
                deleteUploadEntryLocally(upload.id!!)
            }
        }
    }

    private fun deleteUploadEntryLocally(id: Long) {
        dbAdapter.deleteUpload(id)
        FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, id))
        adapter.changeCursor(dbAdapter.outbox)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.outbox, menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_delete_processed_uploads) {
            deleteCompletedUploads()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteCompletedUploads() {
        val uploads = dbAdapter.completedUploads
        if (uploads.isEmpty()) {
            return
        }
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.confirm_delete_processed_uploads)
            .setPositiveButton(R.string.button_ok_text) { _: DialogInterface?, _: Int ->
                for (upload in uploads) {
                    dbAdapter.deleteUpload(upload.id!!)
                    FileUtils.deleteQuietly(FileUtils.getStoredMediaFile(this, upload.id))
                }
                adapter.changeCursor(dbAdapter.outbox)
            }
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        adapter.changeCursor(dbAdapter.outbox)
    }

}