package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.app.TaskStackBuilder
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher.addCallback
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityDetailsBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.StationInfoBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country.Companion.getCountryByCode
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PageablePhoto
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Photo
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStation
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapAvailableHandler
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.NavItem
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.Locale
import java.util.Objects
import java.util.function.Consumer

class DetailsActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private var baseApplication: BaseApplication? = null
    private var rsapiClient: RSAPIClient? = null
    private var binding: ActivityDetailsBinding? = null
    private var station: Station? = null
    private var countries: Set<Country?>? = null
    private var nickname: String? = null
    private var photoPagerAdapter: PhotoPagerAdapter? = null
    private val photoBitmaps: MutableMap<String?, Bitmap?> = HashMap()
    private var selectedPhoto: PageablePhoto? = null
    private val carouselPageIndicators: MutableList<ImageView>? = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)
        baseApplication = application as BaseApplication
        rsapiClient = baseApplication.getRsapiClient()
        countries = baseApplication.getDbAdapter()
            .fetchCountriesWithProviderApps(baseApplication.getCountryCodes())
        Objects.requireNonNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        photoPagerAdapter = PhotoPagerAdapter(this)
        binding!!.details.viewPager.adapter = photoPagerAdapter
        binding!!.details.viewPager.setCurrentItem(0, false)
        binding!!.details.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageablePhoto = photoPagerAdapter!!.getPageablePhotoAtPosition(position)
                onPageablePhotoSelected(pageablePhoto, position)
            }
        })

        // switch off image and license view until we actually have a foto
        binding!!.details.licenseTag.visibility = View.INVISIBLE
        binding!!.details.licenseTag.movementMethod = LinkMovementMethod.getInstance()
        getOnBackPressedDispatcher().addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })
        readPreferences()
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null) {
            station = intent.getSerializableExtra(EXTRA_STATION) as Station?
            if (station == null) {
                Log.w(TAG, "EXTRA_STATION in intent data missing")
                Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show()
                finish()
                return
            }
            binding!!.details.marker.setImageDrawable(ContextCompat.getDrawable(this, markerRes))
            binding!!.details.tvStationTitle.text = station!!.title
            binding!!.details.tvStationTitle.isSingleLine = false
            if (station!!.hasPhoto()) {
                if (ConnectionUtil.checkInternetConnection(this)) {
                    photoBitmaps[station!!.photoUrl] = null
                    BitmapCache.Companion.getInstance()
                        .getPhoto(BitmapAvailableHandler { bitmap: Bitmap? ->
                            if (bitmap != null) {
                                val pageablePhoto = PageablePhoto(station!!, bitmap)
                                runOnUiThread {
                                    addIndicator()
                                    val position =
                                        photoPagerAdapter!!.addPageablePhoto(pageablePhoto)
                                    if (position == 0) {
                                        onPageablePhotoSelected(pageablePhoto, position)
                                    }
                                }
                            }
                        }, station!!.photoUrl)
                }
            }
            loadAdditionalPhotos(station!!)
            baseApplication.getDbAdapter()
                .getPendingUploadsForStation(station)
                .forEach(Consumer { upload: Upload? -> addUploadPhoto(upload) })
        }
    }

    private fun addUploadPhoto(upload: Upload?) {
        if (!upload!!.isPendingPhotoUpload) {
            return
        }
        val profile = baseApplication.getProfile()
        val file = FileUtils.getStoredMediaFile(this, upload.id)
        if (file != null && file.canRead()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                val pageablePhoto = PageablePhoto(
                    upload.id!!,
                    file.toURI().toString(),
                    getString(R.string.new_local_photo),
                    "",
                    if (profile!!.license != null) profile!!.license!!.longName else "",
                    "",
                    bitmap
                )
                runOnUiThread {
                    addIndicator()
                    val position = photoPagerAdapter!!.addPageablePhoto(pageablePhoto)
                    if (position == 0) {
                        onPageablePhotoSelected(pageablePhoto, position)
                    }
                }
            }
        }
    }

    private fun loadAdditionalPhotos(station: Station) {
        rsapiClient!!.getPhotoStationById(station.country, station.id)!!
            .enqueue(object : Callback<PhotoStations?> {
                override fun onResponse(
                    call: Call<PhotoStations?>,
                    response: Response<PhotoStations?>
                ) {
                    if (response.isSuccessful) {
                        val photoStations = response.body() ?: return
                        photoStations.stations.stream()
                            .flatMap<Photo> { (_, _, _, _, _, _, _, photos): PhotoStation -> photos!!.stream() }
                            .forEach { photo: Photo ->
                                val url = photoStations.photoBaseUrl + photo.path
                                if (!photoBitmaps.containsKey(url)) {
                                    photoBitmaps[url] = null
                                    addIndicator()
                                    BitmapCache.Companion.getInstance()
                                        .getPhoto(BitmapAvailableHandler { bitmap: Bitmap ->
                                            runOnUiThread {
                                                addAdditionalPhotoToPagerAdapter(
                                                    photo,
                                                    url,
                                                    photoStations,
                                                    bitmap
                                                )
                                            }
                                        }, url)
                                }
                            }
                    }
                }

                override fun onFailure(call: Call<PhotoStations?>, t: Throwable) {
                    Log.e(TAG, "Failed to load additional photos", t)
                }
            })
    }

    private fun addAdditionalPhotoToPagerAdapter(
        photo: Photo,
        url: String,
        photoStations: PhotoStations,
        bitmap: Bitmap
    ) {
        photoPagerAdapter!!.addPageablePhoto(
            PageablePhoto(
                photo.id,
                url,
                photo.photographer,
                photoStations.getPhotographerUrl(photo.photographer),
                photoStations.getLicenseName(photo.license),
                photoStations.getLicenseUrl(photo.license),
                bitmap
            )
        )
    }

    private fun addIndicator() {
        val indicator = ImageView(this@DetailsActivity)
        indicator.setImageResource(R.drawable.selector_carousel_page_indicator)
        indicator.setPadding(0, 0, 5, 0) // left, top, right, bottom
        binding!!.details.llPageIndicatorContainer.addView(indicator)
        carouselPageIndicators!!.add(indicator)
    }

    private val markerRes: Int
        private get() {
            if (station == null) {
                return R.drawable.marker_missing
            }
            return if (station!!.hasPhoto()) {
                if (isOwner) {
                    if (station!!.active) R.drawable.marker_violet else R.drawable.marker_violet_inactive
                } else {
                    if (station!!.active) R.drawable.marker_green else R.drawable.marker_green_inactive
                }
            } else {
                if (station!!.active) R.drawable.marker_red else R.drawable.marker_red_inactive
            }
        }

    override fun onResume() {
        super.onResume()
        readPreferences()
    }

    private fun readPreferences() {
        nickname = baseApplication.getNickname()
    }

    private val isOwner: Boolean
        private get() = station != null && TextUtils.equals(nickname, station!!.photographer)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    var activityForResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? -> recreate() }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.add_photo) {
            val intent = Intent(this@DetailsActivity, UploadActivity::class.java)
            intent.putExtra(UploadActivity.Companion.EXTRA_STATION, station)
            activityForResultLauncher.launch(intent)
        } else if (itemId == R.id.report_problem) {
            val intent = Intent(this@DetailsActivity, ProblemReportActivity::class.java)
            intent.putExtra(ProblemReportActivity.Companion.EXTRA_STATION, station)
            intent.putExtra(
                ProblemReportActivity.Companion.EXTRA_PHOTO_ID,
                if (selectedPhoto != null) selectedPhoto!!.id else null
            )
            startActivity(intent)
        } else if (itemId == R.id.nav_to_station) {
            startNavigation(this@DetailsActivity)
        } else if (itemId == R.id.timetable) {
            getCountryByCode(countries, station!!.country).map<Any?> { country: Country ->
                val timetableIntent = Timetable().createTimetableIntent(country, station)
                timetableIntent?.let { startActivity(it) }
                null
            }
        } else if (itemId == R.id.share_link) {
            val stationUri = Uri.parse(
                String.format(
                    "https://map.railway-stations.org/station.php?countryCode=%s&stationId=%s",
                    station!!.country,
                    station!!.id
                )
            )
            startActivity(Intent(Intent.ACTION_VIEW, stationUri))
        } else if (itemId == R.id.share_photo) {
            getCountryByCode(countries, station!!.country).map<Any?> { country: Country? ->
                val shareIntent = createPhotoSendIntent() ?: return@map null
                shareIntent.putExtra(Intent.EXTRA_TEXT, binding!!.details.tvStationTitle.text)
                shareIntent.type = "image/jpeg"
                startActivity(Intent.createChooser(shareIntent, "send"))
                null
            }
        } else if (itemId == R.id.station_info) {
            showStationInfo(null)
        } else if (itemId == R.id.provider_android_app) {
            getCountryByCode(countries, station!!.country).map<Any?> { country: Country ->
                val providerApps = country.compatibleProviderApps
                if (providerApps.size == 1) {
                    openAppOrPlayStore(providerApps[0], this)
                } else if (providerApps.size > 1) {
                    val appNames = providerApps.stream()
                        .map<String>(ProviderApp::name)
                        .toArray<CharSequence> { _Dummy_.__Array__() }
                    SimpleDialogs.simpleSelect(
                        this,
                        resources.getString(R.string.choose_provider_app),
                        appNames
                    ) { dialog: DialogInterface?, which: Int ->
                        if (which >= 0 && providerApps.size > which) {
                            openAppOrPlayStore(providerApps[which], this@DetailsActivity)
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.provider_app_missing, Toast.LENGTH_LONG).show()
                }
                null
            }
        } else if (itemId == android.R.id.home) {
            navigateUp()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Tries to open the provider app if installed. If it is not installed or cannot be opened Google Play Store will be opened instead.
     *
     * @param context activity context
     */
    fun openAppOrPlayStore(providerApp: ProviderApp, context: Context) {
        // Try to open App
        val success = openApp(providerApp, context)
        // Could not open App, open play store instead
        if (!success) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(providerApp.url)
            context.startActivity(intent)
        }
    }

    /**
     * Open another app.
     *
     * @param context activity context
     * @return true if likely successful, false if unsuccessful
     * @see https://stackoverflow.com/a/7596063/714965
     */
    private fun openApp(providerApp: ProviderApp, context: Context): Boolean {
        if (!providerApp.isAndroid) {
            return false
        }
        val manager = context.packageManager
        return try {
            val packageName = Uri.parse(providerApp.url).getQueryParameter("id")!!
            val intent = manager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
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

    fun showStationInfo(view: View?) {
        val stationInfoBinding = StationInfoBinding.inflate(
            layoutInflater
        )
        stationInfoBinding.id.text = station!!.id
        stationInfoBinding.coordinates.text = String.format(
            Locale.US,
            resources.getString(R.string.coordinates),
            station!!.lat,
            station!!.lon
        )
        stationInfoBinding.active.setText(if (station != null && station!!.active) R.string.active else R.string.inactive)
        stationInfoBinding.owner.text =
            if (station != null && station!!.photographer != null) station!!.photographer else ""
        if (station!!.outdated) {
            stationInfoBinding.outdatedLabel.visibility = View.VISIBLE
        }
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
            .setTitle(binding!!.details.tvStationTitle.text)
            .setView(stationInfoBinding.root)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }

    private fun createPhotoSendIntent(): Intent? {
        if (selectedPhoto != null) {
            val sendIntent = Intent(Intent.ACTION_SEND)
            val newFile = FileUtils.getImageCacheFile(
                applicationContext, System.currentTimeMillis().toString()
            )
            try {
                Log.i(TAG, "Save photo to: $newFile")
                selectedPhoto!!.bitmap!!.compress(
                    Bitmap.CompressFormat.JPEG,
                    Constants.STORED_PHOTO_QUALITY,
                    FileOutputStream(newFile)
                )
                sendIntent.putExtra(
                    Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                        this@DetailsActivity,
                        BuildConfig.APPLICATION_ID + ".fileprovider", newFile!!
                    )
                )
                return sendIntent
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Error saving cached bitmap", e)
            }
        }
        return null
    }

    private fun startNavigation(context: Context) {
        val adapter: ArrayAdapter<NavItem> = object : ArrayAdapter<NavItem?>(
            this, android.R.layout.select_dialog_item,
            android.R.id.text1, NavItem.values()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)!!
                val view = super.getView(position, convertView, parent)
                val tv = view.findViewById<TextView>(android.R.id.text1)

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(item.iconRes, 0, 0, 0)
                tv.text = getString(item.textRes)

                //Add margin between image and text (support various screen densities)
                val dp5 = (20 * resources.displayMetrics.density + 0.5f).toInt()
                val dp7 = (20 * resources.displayMetrics.density).toInt()
                tv.compoundDrawablePadding = dp5
                tv.setPadding(dp7, 0, 0, 0)
                return view
            }
        }
        AlertDialog.Builder(this)
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.navMethod)
            .setAdapter(adapter) { dialog: DialogInterface?, position: Int ->
                val item = adapter.getItem(position)!!
                val lat = station!!.lat
                val lon = station!!.lon
                val intent = item.createIntent(
                    this@DetailsActivity,
                    lat,
                    lon,
                    binding!!.details.tvStationTitle.text.toString(),
                    markerRes
                )
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.activitynotfound, Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    fun onPageablePhotoSelected(pageablePhoto: PageablePhoto?, position: Int) {
        selectedPhoto = pageablePhoto
        binding!!.details.licenseTag.visibility = View.INVISIBLE
        if (pageablePhoto == null) {
            return
        }

        // Lizenzinfo aufbauen und einblenden
        binding!!.details.licenseTag.visibility = View.VISIBLE
        val photographerUrlAvailable =
            pageablePhoto.photographerUrl != null && !pageablePhoto.photographerUrl!!.isEmpty()
        val licenseUrlAvailable =
            pageablePhoto.licenseUrl != null && !pageablePhoto.licenseUrl!!.isEmpty()
        val photographerText: String?
        photographerText = if (photographerUrlAvailable) {
            String.format(
                LINK_FORMAT,
                pageablePhoto.photographerUrl,
                pageablePhoto.photographer
            )
        } else {
            pageablePhoto.photographer
        }
        val licenseText: String?
        licenseText = if (licenseUrlAvailable) {
            String.format(
                LINK_FORMAT,
                pageablePhoto.licenseUrl,
                pageablePhoto.license
            )
        } else {
            pageablePhoto.license
        }
        binding!!.details.licenseTag.text = Html.fromHtml(
            String.format(
                getText(R.string.license_tag).toString(),
                photographerText,
                licenseText
            ), Html.FROM_HTML_MODE_LEGACY
        )
        if (carouselPageIndicators != null) {
            for (i in carouselPageIndicators.indices) {
                if (i == position) {
                    carouselPageIndicators[position].isSelected = true
                } else {
                    carouselPageIndicators[i].isSelected = false
                }
            }
        }
    }

    companion object {
        private val TAG = DetailsActivity::class.java.simpleName

        // Names of Extras that this class reacts to
        const val EXTRA_STATION = "EXTRA_STATION"
        private const val LINK_FORMAT = "<b><a href=\"%s\">%s</a></b>"
    }
}