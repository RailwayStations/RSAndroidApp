package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityShowErrorBinding
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val EXTRA_ERROR_TEXT = "error"

class ShowErrorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowErrorBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowErrorBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        binding.textViewError.text = intent.getStringExtra(EXTRA_ERROR_TEXT)
        setSupportActionBar(binding.mapsToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.title = createErrorTitle()
        }
    }

    private fun createErrorTitle(): String {
        return String.format(getString(R.string.error_crash_title), getString(R.string.app_name))
    }

    private fun reportBug() {
        val uriUrl: Uri = try {
            Uri.parse(
                String.format(
                    getString(R.string.report_issue_link),
                    URLEncoder.encode(
                        binding.textViewError.text.toString(),
                        StandardCharsets.UTF_8.toString()
                    )
                )
            )
        } catch (ignored: UnsupportedEncodingException) {
            // can't happen as UTF-8 is always available
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_error, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.error_share) {
            onClickedShare()
            return true
        } else if (item.itemId == R.id.error_report) {
            reportBug()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, createErrorTitle())
        intent.putExtra(Intent.EXTRA_TEXT, binding.textViewError.text)
        intent.type = "text/plain"
        startActivity(intent)
    }

}