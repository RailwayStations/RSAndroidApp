package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.app.TaskStackBuilder
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.app.NavUtils
import androidx.core.content.FileProvider
import com.google.gson.Gson
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityUploadBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs.confirmOk
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country.Companion.getCountryByCode
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.KeyValueSpinnerItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.CheckedOutputStream

class UploadActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {

    private lateinit var baseApplication: BaseApplication
    private lateinit var rsapiClient: RSAPIClient
    private lateinit var binding: ActivityUploadBinding
    private lateinit var countries: List<Country>
    private var upload: Upload? = null
    private var station: Station? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var bahnhofId: String? = null
    private var crc32: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        baseApplication = application as BaseApplication
        rsapiClient = baseApplication.rsapiClient
        countries = ArrayList(baseApplication.dbAdapter.allCountries)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (!baseApplication.isLoggedIn) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MyDataActivity::class.java))
            finish()
            return
        }
        if (!baseApplication.profile.isAllowedToUploadPhoto()) {
            confirmOk(
                this,
                R.string.no_photo_upload_allowed
            ) { _: DialogInterface?, _: Int ->
                startActivity(Intent(this, MyDataActivity::class.java))
                finish()
            }
            return
        }
        binding.upload.buttonTakePicture.setOnClickListener { takePicture() }
        binding.upload.buttonSelectPicture.setOnClickListener { selectPicture() }
        binding.upload.buttonUpload.setOnClickListener { upload() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            upload = intent.getSerializableExtra(EXTRA_UPLOAD, Upload::class.java)
            station = intent.getSerializableExtra(EXTRA_STATION, Station::class.java)
            latitude = intent.getSerializableExtra(EXTRA_LATITUDE, Double::class.java)
            longitude = intent.getSerializableExtra(EXTRA_LONGITUDE, Double::class.java)
        } else {
            @Suppress("DEPRECATION")
            upload = intent.getSerializableExtra(EXTRA_UPLOAD) as Upload?
            @Suppress("DEPRECATION")
            station = intent.getSerializableExtra(EXTRA_STATION) as Station?
            @Suppress("DEPRECATION")
            latitude = intent.getSerializableExtra(EXTRA_LATITUDE) as Double?
            @Suppress("DEPRECATION")
            longitude = intent.getSerializableExtra(EXTRA_LONGITUDE) as Double?
        }
        if (station == null && upload != null && upload!!.isUploadForExistingStation) {
            station = baseApplication.dbAdapter.getStationForUpload(upload!!)
        }
        if (latitude == null && longitude == null && upload != null && upload!!.isUploadForMissingStation) {
            latitude = upload!!.lat
            longitude = upload!!.lon
        }
        if (station == null && (latitude == null || longitude == null)) {
            Log.w(
                TAG,
                "EXTRA_STATION and EXTRA_LATITUDE or EXTRA_LONGITUDE in intent data missing"
            )
            Toast.makeText(this, R.string.station_or_coords_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (station != null) {
            bahnhofId = station!!.id
            binding.upload.etStationTitle.setText(station!!.title)
            binding.upload.etStationTitle.inputType = EditorInfo.TYPE_NULL
            binding.upload.etStationTitle.isSingleLine = false
            if (upload == null) {
                upload =
                    baseApplication.dbAdapter.getPendingUploadsForStation(station!!)
                        .firstOrNull(Upload::isPendingPhotoUpload)
            }
            setLocalBitmap(upload)
            binding.upload.spActive.visibility = View.GONE
            binding.upload.spCountries.visibility = View.GONE
            val country = station!!.country
            updateOverrideLicense(country)
        } else {
            if (upload == null) {
                upload = baseApplication.dbAdapter
                    .getPendingUploadForCoordinates(latitude!!, longitude!!)
            }
            binding.upload.etStationTitle.inputType = EditorInfo.TYPE_CLASS_TEXT
            binding.upload.spActive.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(
                    R.array.active_flag_options
                )
            )
            if (upload != null) {
                binding.upload.etStationTitle.setText(upload!!.title)
                setLocalBitmap(upload)
                if (upload!!.active == null) {
                    binding.upload.spActive.setSelection(0)
                } else if (upload!!.active!!) {
                    binding.upload.spActive.setSelection(1)
                } else {
                    binding.upload.spActive.setSelection(2)
                }
            } else {
                binding.upload.spActive.setSelection(0)
            }
            val items = arrayOfNulls<KeyValueSpinnerItem>(
                countries.size + 1
            )
            items[0] = KeyValueSpinnerItem(getString(R.string.chooseCountry), "")
            var selected = 0
            for (i in countries.indices) {
                val country = countries[i]
                items[i + 1] = KeyValueSpinnerItem(country.name, country.code)
                if (upload != null && country.code == upload!!.country) {
                    selected = i + 1
                }
            }
            val countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.upload.spCountries.adapter = countryAdapter
            binding.upload.spCountries.setSelection(selected)
            binding.upload.spCountries.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long
                    ) {
                        val selectedCountry =
                            parent.getItemAtPosition(position) as KeyValueSpinnerItem
                        updateOverrideLicense(selectedCountry.value)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        updateOverrideLicense(null)
                    }
                }
        }
        if (upload != null) {
            binding.upload.etComment.setText(upload!!.comment)
        }
        binding.upload.txtPanorama.text =
            Html.fromHtml(getString(R.string.panorama_info), Html.FROM_HTML_MODE_COMPACT)
        binding.upload.txtPanorama.movementMethod = LinkMovementMethod.getInstance()
        binding.upload.txtPanorama.setLinkTextColor(Color.parseColor("#c71c4d"))
    }

    private fun updateOverrideLicense(country: String?) {
        val overrideLicense =
            getCountryByCode(countries, country)?.overrideLicense
        if (overrideLicense != null) {
            binding.upload.cbSpecialLicense.text =
                getString(R.string.special_license, overrideLicense)
        }
        binding.upload.cbSpecialLicense.visibility =
            if (overrideLicense == null) View.GONE else View.VISIBLE
    }

    private val imageCaptureResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            try {
                assertCurrentPhotoUploadExists()
                val cameraTempFile = cameraTempFile
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true // just query the image size in the first step
                BitmapFactory.decodeFile(cameraTempFile.path, options)
                val sampling = options.outWidth / Constants.STORED_PHOTO_WIDTH
                if (sampling > 1) {
                    options.inSampleSize = sampling
                }
                options.inJustDecodeBounds = false
                storeBitmapToLocalFile(
                    getStoredMediaFile(upload), BitmapFactory.decodeFile(
                        cameraTempFile.path, options
                    )
                )
                FileUtils.deleteQuietly(cameraTempFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing photo", e)
                Toast.makeText(
                    applicationContext,
                    getString(R.string.error_processing_photo) + e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun takePicture() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return
        }
        assertCurrentPhotoUploadExists()
        val photoURI = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            cameraTempFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, resources.getString(R.string.app_name))
        intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, binding.upload.etStationTitle.text)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            imageCaptureResultLauncher.launch(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_image_capture_app_found, Toast.LENGTH_LONG).show()
        }
    }

    private val selectPictureResultLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            contentResolver.openFileDescriptor(uri!!, "r").use { parcelFileDescriptor ->
                if (parcelFileDescriptor == null) {
                    return@registerForActivityResult
                }
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                assertCurrentPhotoUploadExists()
                val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                val sampling = bitmap.width / Constants.STORED_PHOTO_WIDTH
                var scaledScreen = bitmap
                if (sampling > 1) {
                    scaledScreen = Bitmap.createScaledBitmap(
                        bitmap,
                        bitmap.width / sampling,
                        bitmap.height / sampling,
                        false
                    )
                }
                storeBitmapToLocalFile(getStoredMediaFile(upload), scaledScreen)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing photo", e)
            Toast.makeText(
                applicationContext,
                getString(R.string.error_processing_photo) + e.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun selectPicture() {
        selectPictureResultLauncher.launch("image/*")
    }

    private fun assertCurrentPhotoUploadExists() {
        if (upload == null || upload!!.isProblemReport || upload!!.isUploaded) {
            val newUpload = Upload(
                null,
                if (station != null) station!!.country else null,
                if (station != null) station!!.id else null,
                null,
                null,
                latitude,
                longitude
            )
            upload = baseApplication.dbAdapter.insertUpload(newUpload)
        }
    }

    @Throws(IOException::class)
    private fun storeBitmapToLocalFile(file: File?, bitmap: Bitmap?) {
        if (bitmap == null) {
            throw RuntimeException(getString(R.string.error_scaling_photo))
        }
        Log.i(
            TAG,
            "Save photo with width=" + bitmap.width + " and height=" + bitmap.height + " to: " + file
        )
        CheckedOutputStream(FileOutputStream(file), CRC32()).use { cos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.STORED_PHOTO_QUALITY, cos)
            crc32 = cos.checksum.value
            setLocalBitmap(upload)
        }
    }

    /**
     * Get the file path for storing this stations foto
     *
     * @return the File
     */
    private fun getStoredMediaFile(upload: Upload?): File? {
        return if (upload == null) {
            null
        } else FileUtils.getStoredMediaFile(this, upload.id)
    }

    private val cameraTempFile: File
        /**
         * Get the file path for the Camera app to store the unprocessed photo to.
         */
        get() = FileUtils.getImageCacheFile(this, upload!!.id.toString())

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.upload, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.share_photo) {
            val shareIntent = createPhotoSendIntent()
            if (shareIntent != null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, binding.upload.etStationTitle.text)
                shareIntent.type = "image/jpeg"
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_photo)))
            }
        } else if (itemId == android.R.id.home) {
            navigateUp()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun navigateUp() {
        val callingActivity =
            callingActivity // if MapsActivity was calling, then we don't want to rebuild the Backstack
        val upIntent = NavUtils.getParentActivityIntent(this)
        if (callingActivity == null && upIntent != null) {
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot) {
                Log.v(TAG, "Recreate back stack")
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent)
                    .startActivities()
            }
        }
        finish()
    }

    fun upload() {
        if (TextUtils.isEmpty(binding.upload.etStationTitle.text)) {
            Toast.makeText(this, R.string.station_title_needed, Toast.LENGTH_LONG).show()
            return
        }
        assertCurrentPhotoUploadExists()
        val mediaFile = getStoredMediaFile(upload)!!
        if (!mediaFile.exists()) {
            if (station != null) {
                Toast.makeText(this, R.string.please_take_photo, Toast.LENGTH_LONG).show()
                return
            }
        }
        if (binding.upload.cbSpecialLicense.text.isNotEmpty() && !binding.upload.cbSpecialLicense.isChecked) {
            Toast.makeText(this, R.string.special_license_confirm, Toast.LENGTH_LONG).show()
            return
        }
        if (crc32 != null && upload != null && crc32 == upload!!.crc32 && !binding.upload.cbChecksum.isChecked) {
            Toast.makeText(this, R.string.photo_checksum, Toast.LENGTH_LONG).show()
            return
        }
        if (station == null) {
            if (binding.upload.spActive.selectedItemPosition == 0) {
                Toast.makeText(this, R.string.active_flag_choose, Toast.LENGTH_LONG).show()
                return
            }
            val selectedCountry = binding.upload.spCountries.selectedItem as KeyValueSpinnerItem
            upload!!.country = selectedCountry.value
        }
        SimpleDialogs.confirmOkCancel(
            this,
            if (station != null) R.string.photo_upload else R.string.report_missing_station
        ) { _: DialogInterface?, _: Int ->
            binding.upload.progressBar.visibility = View.VISIBLE
            var stationTitle: String? = binding.upload.etStationTitle.text.toString()
            var comment: String? = binding.upload.etComment.text.toString()
            upload!!.title = stationTitle
            upload!!.comment = comment
            try {
                stationTitle = URLEncoder.encode(
                    binding.upload.etStationTitle.text.toString(),
                    StandardCharsets.UTF_8.toString()
                )
                comment = URLEncoder.encode(comment, StandardCharsets.UTF_8.toString())
            } catch (e: UnsupportedEncodingException) {
                Log.e(TAG, "Error encoding station title or comment", e)
            }
            upload!!.active = binding.upload.spActive.selectedItemPosition == 1
            baseApplication.dbAdapter.updateUpload(upload)
            val file: RequestBody = if (mediaFile.exists()) mediaFile.asRequestBody(
                URLConnection.guessContentTypeFromName(
                    mediaFile.name
                ).toMediaTypeOrNull()
            ) else byteArrayOf().toRequestBody(
                "application/octet-stream".toMediaTypeOrNull(),
            )
            rsapiClient.photoUpload(
                bahnhofId, if (station != null) station!!.country else upload!!.country,
                stationTitle, latitude, longitude, comment, upload!!.active, file
            ).enqueue(object : Callback<InboxResponse?> {
                override fun onResponse(
                    call: Call<InboxResponse?>,
                    response: Response<InboxResponse?>
                ) {
                    binding.upload.progressBar.visibility = View.GONE
                    val inboxResponse: InboxResponse? = if (response.isSuccessful) {
                        response.body()
                    } else if (response.code() == 401) {
                        onUnauthorized()
                        return
                    } else {
                        assert(response.errorBody() != null)
                        val gson = Gson()
                        gson.fromJson(
                            response.errorBody()!!.charStream(),
                            InboxResponse::class.java
                        )
                    }
                    assert(inboxResponse != null)
                    upload!!.remoteId = inboxResponse!!.id
                    upload!!.inboxUrl = inboxResponse.inboxUrl
                    upload!!.uploadState = inboxResponse.state.uploadState
                    upload!!.crc32 = inboxResponse.crc32
                    baseApplication.dbAdapter.updateUpload(upload)
                    if (inboxResponse.state === InboxResponse.InboxResponseState.ERROR) {
                        confirmOk(
                            this@UploadActivity,
                            getString(
                                InboxResponse.InboxResponseState.ERROR.messageId,
                                inboxResponse.message
                            )
                        )
                    } else {
                        confirmOk(this@UploadActivity, inboxResponse.state.messageId)
                    }
                }

                override fun onFailure(call: Call<InboxResponse?>, t: Throwable) {
                    Log.e(TAG, "Error uploading photo", t)
                    binding.upload.progressBar.visibility = View.GONE
                    confirmOk(
                        this@UploadActivity,
                        getString(InboxResponse.InboxResponseState.ERROR.messageId, t.message)
                    )
                    fetchUploadStatus(upload) // try to get the upload state again
                }
            })
        }
    }

    private fun createPhotoSendIntent(): Intent? {
        val file = getStoredMediaFile(upload)
        if (file != null && file.canRead()) {
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.putExtra(
                Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    this@UploadActivity,
                    BuildConfig.APPLICATION_ID + ".fileprovider", file
                )
            )
            return sendIntent
        }
        return null
    }

    /**
     * Fetch bitmap from device local location, if it exists, and set the photo view.
     */
    private fun setLocalBitmap(upload: Upload?) {
        val localPhoto = checkForLocalPhoto(upload)
        if (localPhoto != null) {
            binding.upload.imageview.setImageBitmap(localPhoto)
            fetchUploadStatus(upload)
        }
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
                            binding.upload.uploadStatus.text =
                                getString(
                                    R.string.upload_state,
                                    getString(remoteStateQuery.state.textId)
                                )
                            binding.upload.uploadStatus.setTextColor(
                                resources.getColor(
                                    remoteStateQuery.state.colorId,
                                    null
                                )
                            )
                            binding.upload.uploadStatus.visibility = View.VISIBLE
                            upload.uploadState = remoteStateQuery.state
                            upload.rejectReason = remoteStateQuery.rejectedReason
                            upload.crc32 = remoteStateQuery.crc32
                            upload.remoteId = remoteStateQuery.id
                            baseApplication.dbAdapter.updateUpload(upload)
                            updateCrc32Checkbox()
                        }
                    } else if (response.code() == 401) {
                        onUnauthorized()
                    } else {
                        Log.w(TAG, "Upload states not processable")
                    }
                }

                override fun onFailure(call: Call<List<InboxStateQuery>>, t: Throwable) {
                    Log.e(TAG, "Error retrieving upload state", t)
                }
            })
    }

    private fun onUnauthorized() {
        baseApplication.accessToken = null
        rsapiClient.clearToken()
        Toast.makeText(this, R.string.authorization_failed, Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MyDataActivity::class.java))
        finish()
    }

    private fun updateCrc32Checkbox() {
        if (crc32 != null && upload != null) {
            val sameChecksum = crc32 == upload!!.crc32
            binding.upload.cbChecksum.visibility = if (sameChecksum) View.VISIBLE else View.GONE
        }
    }

    /**
     * Check if there's a local photo file for this station.
     */
    private fun checkForLocalPhoto(upload: Upload?): Bitmap? {
        // show the image
        val localFile = getStoredMediaFile(upload)
        Log.d(TAG, "File: $localFile")
        crc32 = null
        if (localFile != null && localFile.canRead()) {
            Log.d(TAG, "FileGetPath: " + localFile.path)
            try {
                CheckedInputStream(FileInputStream(localFile), CRC32()).use { cis ->
                    val scaledScreen = BitmapFactory.decodeStream(cis)
                    crc32 = cis.checksum.value
                    Log.d(
                        TAG,
                        "img width " + scaledScreen.width + ", height " + scaledScreen.height + ", crc32 " + crc32
                    )
                    updateCrc32Checkbox()
                    return scaledScreen
                }
            } catch (e: Exception) {
                Log.e(TAG, String.format("Error reading media file for station %s", bahnhofId), e)
            }
        }
        return null
    }

    companion object {
        private val TAG = UploadActivity::class.java.simpleName

        // Names of Extras that this class reacts to
        const val EXTRA_UPLOAD = "EXTRA_UPLOAD"
        const val EXTRA_STATION = "EXTRA_STATION"
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
    }
}