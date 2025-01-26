package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.MapsActivity.BahnhofGeoItem
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMapsBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.MapInfoFragment
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.StationFilterBar
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.ClusterManager
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.DbsTileSource
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.GeoItem
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.MarkerBitmap
import de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge.TapHandler
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter
import org.mapsforge.core.graphics.Align
import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.FontFamily
import org.mapsforge.core.graphics.FontStyle
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.input.MapZoomControls
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.IMapViewPosition
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.mapsforge.map.rendertheme.StreamRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.inject.Inject

private val TAG = MapsActivity::class.java.simpleName

const val EXTRAS_MAPS_LATITUDE = "EXTRAS_MAPS_LATITUDE"
const val EXTRAS_MAPS_LONGITUDE = "EXTRAS_MAPS_LONGITUDE"
const val EXTRAS_MAPS_MARKER = "EXTRAS_MAPS_MARKER"

private const val MIN_DISTANCE_METERS_CHANGE_FOR_UPDATES: Long = 1
private const val MIN_TIME_MILLIS_BETWEEN_UPDATES: Long = 500 // half a minute
private const val REQUEST_FINE_LOCATION = 1
private const val USER_AGENT = "railway-stations.org-android"

private val NAME_PATTERN: Pattern = Pattern.compile("[+\\s]*\\((.*)\\)[+\\s]*$")
private val POSITION_PATTERN: Pattern = Pattern.compile(
    "([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)"
)
private val QUERY_POSITION_PATTERN: Pattern =
    Pattern.compile("q=([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)")

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), LocationListener, TapHandler<BahnhofGeoItem>,
    StationFilterBar.OnChangeListener {

    @Inject
    lateinit var dbAdapter: DbAdapter

    @Inject
    lateinit var preferencesService: PreferencesService

    @Inject
    lateinit var rsapiClient: RSAPIClient

    private lateinit var binding: ActivityMapsBinding

    private val onlineTileSources = mutableMapOf<String, OnlineTileSource>()
    private var layer: Layer? = null
    private var clusterer: ClusterManager<BahnhofGeoItem>? = null
    private val tileCaches = mutableListOf<TileCache>()
    private var myPos: LatLong? = null
    private var myLocSwitch: CheckBox? = null
    private var nickname: String? = null
    private var locationManager: LocationManager? = null
    private var askedForPermission = false
    private var missingMarker: Marker? = null
    private var extraMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidGraphicFactory.createInstance(application)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("#c71c4d")
        setSupportActionBar(binding.mapsToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        nickname = preferencesService.nickname
        onNewIntent(intent)
        addDBSTileSource(R.string.dbs_osm_basic, "/styles/dbs-osm-basic/")
        addDBSTileSource(R.string.dbs_osm_railway, "/styles/dbs-osm-railway/")
        createMapViews()
        createTileCaches()
        checkPermissionsAndCreateLayersAndControls()
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        super.onNewIntent(intent)

        extraMarker = if ("geo" == intent.scheme && intent.data != null) {
            fromGeoUri(intent.data.toString())
        } else {
            val latitude = intent.getDoubleExtra(EXTRAS_MAPS_LATITUDE, 0.0)
            val longitude = intent.getDoubleExtra(EXTRAS_MAPS_LONGITUDE, 0.0)
            val markerRes = intent.getIntExtra(EXTRAS_MAPS_MARKER, -1)
            if (markerRes != -1) {
                createBitmapMarker(LatLong(latitude, longitude), markerRes)
            } else {
                null
            }
        }

        extraMarker?.let {
            setMyLocSwitch(false)
            myPos = it.latLong
        }
    }

    private fun fromGeoUri(uri: String): Marker? {
        var schemeSpecific = uri.substring(uri.indexOf(":") + 1)

        val nameMatcher = NAME_PATTERN.matcher(schemeSpecific)
        if (nameMatcher.find()) {
            URLDecoder.decode(nameMatcher.group(1), "UTF-8")?.let {
                schemeSpecific = schemeSpecific.substring(0, nameMatcher.start())
            }
        }

        var positionPart = schemeSpecific
        var queryPart = ""
        val queryStartIndex = schemeSpecific.indexOf('?')
        if (queryStartIndex != -1) {
            positionPart = schemeSpecific.substring(0, queryStartIndex)
            queryPart = schemeSpecific.substring(queryStartIndex + 1)
        }

        val positionMatcher = POSITION_PATTERN.matcher(positionPart)
        var lat: Double? = null
        var lon: Double? = null
        if (positionMatcher.find()) {
            lat = positionMatcher.group(1)?.toDouble()
            lon = positionMatcher.group(2)?.toDouble()
        }

        val queryPositionMatcher = QUERY_POSITION_PATTERN.matcher(queryPart)
        if (queryPositionMatcher.find()) {
            lat = queryPositionMatcher.group(1)?.toDouble()
            lon = queryPositionMatcher.group(2)?.toDouble()
        }
        if (lat == null || lon == null) {
            return null
        }
        return createBitmapMarker(LatLong(lat, lon), R.drawable.marker_orange)
    }

    private fun addDBSTileSource(nameResId: Int, baseUrl: String) {
        val dbsBasic = DbsTileSource(getString(nameResId), baseUrl)
        onlineTileSources[dbsBasic.name] = dbsBasic
    }

    private fun createTileCaches() {
        tileCaches.add(
            AndroidUtil.createTileCache(
                this, this.javaClass.simpleName,
                binding.map.mapView.model.displayModel.tileSize, 1.0f,
                binding.map.mapView.model.frameBufferModel.overdrawFactor, true
            )
        )
    }

    /**
     * Hook to check for Android Runtime Permissions.
     */
    private fun checkPermissionsAndCreateLayersAndControls() {
        createLayers()
        createControls()
    }

    /**
     * Hook to create controls, such as scale bars.
     * You can add more controls.
     */
    private fun createControls() {
        initializePosition(binding.map.mapView.model.mapViewPosition)
    }

    /**
     * initializes the map view position.
     *
     * @param mvp the map view position to be set
     */
    private fun initializePosition(mvp: IMapViewPosition) {
        if (myPos != null) {
            mvp.mapPosition = MapPosition(myPos, preferencesService.zoomLevelDefault)
        } else {
            mvp.mapPosition = preferencesService.lastMapPosition
        }
        mvp.zoomLevelMax = zoomLevelMax
        mvp.zoomLevelMin = zoomLevelMin
    }

    /**
     * Template method to create the map views.
     */
    private fun createMapViews() {
        binding.map.mapView.isClickable = true
        binding.map.mapView.setOnMapDragListener { myLocSwitch?.isChecked = false }
        binding.map.mapView.mapScaleBar.isVisible = true
        binding.map.mapView.setBuiltInZoomControls(true)
        binding.map.mapView.mapZoomControls.isAutoHide = true
        binding.map.mapView.mapZoomControls.zoomLevelMin = zoomLevelMin
        binding.map.mapView.mapZoomControls.zoomLevelMax = zoomLevelMax
        binding.map.mapView.mapZoomControls.setZoomControlsOrientation(MapZoomControls.Orientation.VERTICAL_IN_OUT)
        binding.map.mapView.mapZoomControls.setZoomInResource(R.drawable.zoom_control_in)
        binding.map.mapView.mapZoomControls.setZoomOutResource(R.drawable.zoom_control_out)
        binding.map.mapView.mapZoomControls.setMarginHorizontal(
            resources.getDimensionPixelOffset(R.dimen.controls_margin)
        )
        binding.map.mapView.mapZoomControls.setMarginVertical(resources.getDimensionPixelOffset(R.dimen.controls_margin))
    }

    private val zoomLevelMax: Byte
        get() = binding.map.mapView.model.mapViewPosition.zoomLevelMax
    private val zoomLevelMin: Byte
        get() = binding.map.mapView.model.mapViewPosition.zoomLevelMin

    /**
     * Hook to purge tile caches.
     * By default we purge every tile cache that has been added to the tileCaches list.
     */
    private fun purgeTileCaches() {
        for (tileCache in tileCaches) {
            tileCache.purge()
        }
        tileCaches.clear()
    }

    private val renderTheme: XmlRenderTheme
        get() {
            preferencesService.mapThemeUri?.let {
                try {
                    val renderThemeFile = DocumentFile.fromSingleUri(application, it)
                    return StreamRenderTheme(
                        "/assets/", contentResolver.openInputStream(
                            renderThemeFile!!.uri
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading theme $it", e)
                    return InternalRenderTheme.DEFAULT
                }
            }
            return InternalRenderTheme.DEFAULT
        }
    private val mapFile: MapDataStore?
        get() {
            preferencesService.map.toUri()?.let {
                if (!DocumentFile.isDocumentUri(this, it)) {
                    return null
                }
                try {
                    val inputStream = contentResolver.openInputStream(it) as FileInputStream?
                    return MapFile(inputStream, 0, null)
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "Can't open mapFile", e)
                }
                null
            }
            return null
        }

    private fun createLayers() {
        if (mapFile != null) {
            val rendererLayer: TileRendererLayer = object : TileRendererLayer(
                tileCaches[0],
                mapFile,
                binding.map.mapView.model.mapViewPosition,
                false,
                true,
                false,
                AndroidGraphicFactory.INSTANCE
            ) {
                override fun onLongPress(
                    tapLatLong: LatLong,
                    thisXY: Point?,
                    tapXY: Point?
                ): Boolean {
                    this@MapsActivity.onLongPress(tapLatLong)
                    return true
                }
            }
            rendererLayer.setXmlRenderTheme(renderTheme)
            layer = rendererLayer
            binding.map.mapView.layerManager.layers.add(layer)
        } else {
            var tileSource: AbstractTileSource? = onlineTileSources[preferencesService.map]
            if (tileSource == null) {
                tileSource = OpenStreetMapMapnik.INSTANCE
            }
            tileSource!!.userAgent = USER_AGENT
            layer = object : TileDownloadLayer(
                tileCaches[0],
                binding.map.mapView.model.mapViewPosition, tileSource,
                AndroidGraphicFactory.INSTANCE
            ) {
                override fun onLongPress(
                    tapLatLong: LatLong, thisXY: Point?,
                    tapXY: Point?
                ): Boolean {
                    this@MapsActivity.onLongPress(tapLatLong)
                    return true
                }
            }
            binding.map.mapView.layerManager.layers.add(layer)
            binding.map.mapView.setZoomLevelMin(tileSource.zoomLevelMin)
            binding.map.mapView.setZoomLevelMax(tileSource.zoomLevelMax)
        }
    }

    private fun createBitmapMarker(latLong: LatLong, markerRes: Int): Marker {
        val drawable = ContextCompat.getDrawable(this, markerRes)!!
        val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
        return Marker(latLong, bitmap, 0, -(bitmap.height / 2))
    }

    private fun onLongPress(tapLatLong: LatLong) {
        if (missingMarker == null) {
            val drawable = ContextCompat.getDrawable(this, R.drawable.marker_missing)!!
            val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
            missingMarker =
                object : Marker(tapLatLong, bitmap, -(bitmap.width / 2), -bitmap.height) {
                    override fun onTap(tapLatLong: LatLong, layerXY: Point, tapXY: Point): Boolean {
                        return showMissingStationDialog()
                    }
                }
            binding.map.mapView.layerManager.layers.add(missingMarker)
        } else {
            missingMarker!!.latLong = tapLatLong
            missingMarker!!.requestRedraw()
        }

        vibrationFeedbackForLongClick()
    }

    private fun Marker.showMissingStationDialog(): Boolean {
        SimpleDialogs.confirmOkCancel(
            this@MapsActivity,
            R.string.add_missing_station,
            { _: DialogInterface?, _: Int ->
                val intent = Intent(this@MapsActivity, UploadActivity::class.java)
                intent.putExtra(
                    EXTRA_UPLOAD_LATITUDE,
                    latLong.latitude
                )
                intent.putExtra(
                    EXTRA_UPLOAD_LONGITUDE,
                    latLong.longitude
                )
                binding.map.mapView.layerManager.layers.remove(missingMarker)
                missingMarker = null
                startActivity(intent)
            })
        { _: DialogInterface?, _: Int ->
            binding.map.mapView.layerManager.layers.remove(missingMarker)
            missingMarker = null
        }

        return false
    }

    private fun vibrationFeedbackForLongClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrationEffect = VibrationEffect.createOneShot(
                150,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibratorManager.vibrate(CombinedVibration.createParallel(vibrationEffect))
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createOneShot(
                    150,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.maps, menu)
        val item = menu.findItem(R.id.menu_toggle_mypos)
        myLocSwitch = CheckBox(this)
        myLocSwitch?.setButtonDrawable(R.drawable.ic_gps_fix_selector)
        myLocSwitch?.isChecked = preferencesService.isLocationUpdates
        item.actionView = myLocSwitch
        myLocSwitch?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferencesService.isLocationUpdates = isChecked
            if (isChecked) {
                askedForPermission = false
                registerLocationManager()
            } else {
                unregisterLocationManager()
            }
        }
        val map = preferencesService.map
        val osmMapnick = menu.findItem(R.id.osm_mapnik)
        osmMapnick.isChecked = map == null
        osmMapnick.setOnMenuItemClickListener(
            MapMenuListener(
                mapsActivity = this,
                preferencesService = preferencesService,
                map = null
            )
        )
        val mapSubmenu = menu.findItem(R.id.maps_submenu).subMenu!!
        for (tileSource in onlineTileSources.values) {
            val mapItem = mapSubmenu.add(R.id.maps_group, Menu.NONE, Menu.NONE, tileSource.name)
            mapItem.isChecked = tileSource.name == map
            mapItem.setOnMenuItemClickListener(
                MapMenuListener(
                    mapsActivity = this,
                    preferencesService = preferencesService,
                    map = tileSource.name
                )
            )
        }
        preferencesService.mapDirectoryUri?.let { mapDirectoryUri ->
            getDocumentFileFromTreeUri(mapDirectoryUri)?.let { documentFile ->
                documentFile.listFiles()
                    .filter { it.isFile && it.name!!.endsWith(".map") }
                    .forEach { file ->
                        mapSubmenu.add(R.id.maps_group, Menu.NONE, Menu.NONE, file.name).apply {
                            isChecked = map.toUri()?.let { file.uri == it } == true
                            setOnMenuItemClickListener(
                                MapMenuListener(
                                    mapsActivity = this@MapsActivity,
                                    preferencesService = preferencesService,
                                    map = file.uri.toString()
                                )
                            )
                        }
                    }
            }
        }

        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true)
        mapSubmenu.add(R.string.map_folder).apply {
            setOnMenuItemClickListener {
                openMapDirectoryChooser()
                false
            }
        }
        val mapTheme = preferencesService.mapThemeUri
        menu.findItem(R.id.default_theme).apply {
            isChecked = mapTheme == null
            setOnMenuItemClickListener(
                MapThemeMenuListener(
                    mapsActivity = this@MapsActivity,
                    preferencesService = preferencesService,
                    mapThemeUri = null
                )
            )
        }
        val themeSubmenu = menu.findItem(R.id.themes_submenu).subMenu!!
        preferencesService.mapThemeDirectoryUri?.let {
            getDocumentFileFromTreeUri(it)?.let { documentsTree ->
                for (file in documentsTree.listFiles()) {
                    if (file.isFile && file.name!!.endsWith(".xml")) {
                        themeSubmenu.add(R.id.themes_group, Menu.NONE, Menu.NONE, file.name).apply {
                            isChecked = mapTheme?.let { uri: Uri -> file.uri == uri }
                                ?: false
                            setOnMenuItemClickListener(
                                MapThemeMenuListener(
                                    this@MapsActivity,
                                    preferencesService,
                                    file.uri
                                )
                            )
                        }
                    } else if (file.isDirectory) {
                        file.findFile(file.name + ".xml")?.let { childFile ->
                            themeSubmenu.add(R.id.themes_group, Menu.NONE, Menu.NONE, file.name)
                                .apply {
                                    isChecked =
                                        mapTheme?.let { uri: Uri -> childFile.uri == uri }
                                            ?: false
                                    setOnMenuItemClickListener(
                                        MapThemeMenuListener(
                                            this@MapsActivity,
                                            preferencesService,
                                            childFile.uri
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true)
        themeSubmenu.add(R.string.theme_folder).apply {
            setOnMenuItemClickListener {
                openThemeDirectoryChooser()
                false
            }
        }
        return true
    }

    private fun getDocumentFileFromTreeUri(uri: Uri) =
        try {
            DocumentFile.fromTreeUri(application, uri)
        } catch (_: Exception) {
            Log.w(TAG, "Error getting DocumentFile from Uri: $uri")
            null
        }

    private var themeDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data!!.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                preferencesService.setMapThemeDirectoryUri(uri)
                recreate()
            }
        }
    }

    private var mapDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data!!.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                preferencesService.setMapDirectoryUri(uri)
                recreate()
            }
        }
    }

    private fun openDirectory(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        launcher.launch(intent)
    }

    private fun openMapDirectoryChooser() {
        openDirectory(mapDirectoryLauncher)
    }

    private fun openThemeDirectoryChooser() {
        openDirectory(themeDirectoryLauncher)
    }

    /**
     * Android Activity life cycle method.
     */
    override fun onDestroy() {
        binding.map.mapView.destroyAll()
        AndroidGraphicFactory.clearResourceMemoryCache()
        purgeTileCaches()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.map_info) {
            MapInfoFragment().show(supportFragmentManager, "Map Info Dialog")
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    private fun reloadMap() {
        destroyClusterManager()
        LoadMapMarkerTask(this).start()
    }

    private fun runUpdateCountriesAndStations() {
        binding.map.progressBar.visibility = View.VISIBLE
        rsapiClient.runUpdateCountriesAndStations(
            this,
            dbAdapter,
        ) { reloadMap() }
    }

    private fun onStationsLoaded(stationList: List<Station>, uploadList: List<Upload>) {
        try {
            createClusterManager()
            addMarkers(stationList, uploadList)
            extraMarker?.let { marker ->
                if (stationList.none { it.lat == marker.latLong.latitude && it.lon == marker.latLong.longitude }) {
                    binding.map.mapView.layerManager.layers.add(marker)
                }
            }
            binding.map.progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                resources.getQuantityString(
                    R.plurals.stations_loaded,
                    stationList.size,
                    stationList.size
                ),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading markers", e)
        }
    }

    override fun onTap(item: BahnhofGeoItem) {
        val id = item.station.id
        try {
            val station = dbAdapter.getStationByKey(item.station.country, id)
            val intent = Intent(this@MapsActivity, DetailsActivity::class.java)
                .putExtra(EXTRA_DETAILS_STATION, station)
            startActivity(intent)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not fetch station id $id that we put onto the map", e)
        }
    }

    private fun setMyLocSwitch(checked: Boolean) {
        myLocSwitch?.isChecked = checked
        preferencesService.isLocationUpdates = checked
    }

    override fun stationFilterChanged(stationFilter: StationFilter) {
        reloadMap()
    }

    override fun sortOrderChanged(sortByDistance: Boolean) {
        // unused
    }

    private class LoadMapMarkerTask(activity: MapsActivity) : Thread() {
        private val activityRef = WeakReference(activity)

        override fun run() {
            activityRef.get()?.let {
                val stationList = it.readStations()
                val uploadList = it.readPendingUploads()
                it.runOnUiThread { it.onStationsLoaded(stationList, uploadList) }
            }
        }
    }

    private fun readStations() =
        dbAdapter.getAllStations(
            preferencesService.stationFilter,
            preferencesService.countryCodes
        )

    private fun readPendingUploads() =
        dbAdapter.getPendingUploads(false)

    private fun createMarkerBitmaps() =
        listOf(
            createSmallSingleIconMarker(),
            createSmallClusterIconMarker(),
            createLargeClusterIconMarker()
        )

    /**
     * large cluster icon. 100 will be ignored.
     */
    private fun createLargeClusterIconMarker(): MarkerBitmap {
        val bitmapBalloonMN = loadBitmap(R.drawable.balloon_m_n)
        val paint = AndroidGraphicFactory.INSTANCE.createPaint()
        paint.setStyle(Style.FILL)
        paint.setTextAlign(Align.CENTER)
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD)
        paint.color = Color.BLACK
        return MarkerBitmap(
            this.applicationContext, bitmapBalloonMN,
            Point(0.0, 0.0), 11f, 100, paint
        )
    }

    /**
     * small cluster icon. for 10 or less items.
     */
    private fun createSmallClusterIconMarker(): MarkerBitmap {
        val bitmapBalloonSN = loadBitmap(R.drawable.balloon_s_n)
        val paint = AndroidGraphicFactory.INSTANCE.createPaint()
        paint.setStyle(Style.FILL)
        paint.setTextAlign(Align.CENTER)
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD)
        paint.color = Color.BLACK
        return MarkerBitmap(
            this.applicationContext, bitmapBalloonSN,
            Point(0.0, 0.0), 9f, 10, paint
        )
    }

    private fun createSmallSingleIconMarker(): MarkerBitmap {
        val bitmapWithPhoto = loadBitmap(R.drawable.marker_green)
        val markerWithoutPhoto = loadBitmap(R.drawable.marker_red)
        val markerOwnPhoto = loadBitmap(R.drawable.marker_violet)
        val markerPendingUpload = loadBitmap(R.drawable.marker_yellow)
        val markerWithPhotoInactive = loadBitmap(R.drawable.marker_green_inactive)
        val markerWithoutPhotoInactive = loadBitmap(R.drawable.marker_red_inactive)
        val markerOwnPhotoInactive = loadBitmap(R.drawable.marker_violet_inactive)
        val paint = AndroidGraphicFactory.INSTANCE.createPaint()
        paint.setStyle(Style.FILL)
        paint.setTextAlign(Align.CENTER)
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD)
        paint.color = Color.RED
        return MarkerBitmap(
            this.applicationContext,
            markerWithoutPhoto,
            bitmapWithPhoto,
            markerOwnPhoto,
            markerWithoutPhotoInactive,
            markerWithPhotoInactive,
            markerOwnPhotoInactive,
            markerPendingUpload,
            Point(0.0, -(markerWithoutPhoto.height / 2.0)),
            10f,
            1,
            paint
        )
    }

    private fun loadBitmap(resourceId: Int): Bitmap {
        val bitmap = AndroidGraphicFactory.convertToBitmap(
            ResourcesCompat.getDrawable(
                resources,
                resourceId,
                null
            )
        )
        bitmap.incrementRefCount()
        return bitmap
    }

    private fun addMarkers(stationMarker: List<Station>, uploadList: List<Upload>) {
        var minLat = 0.0
        var maxLat = 0.0
        var minLon = 0.0
        var maxLon = 0.0
        for (station in stationMarker) {
            val isPendingUpload = isPendingUpload(station, uploadList)
            val geoItem = BahnhofGeoItem(station, isPendingUpload)
            val stationPos = geoItem.latLong
            if (minLat == 0.0) {
                minLat = stationPos.latitude
                maxLat = stationPos.latitude
                minLon = stationPos.longitude
                maxLon = stationPos.longitude
            } else {
                minLat = minLat.coerceAtMost(stationPos.latitude)
                maxLat = maxLat.coerceAtLeast(stationPos.latitude)
                minLon = minLon.coerceAtMost(stationPos.longitude)
                maxLon = maxLon.coerceAtLeast(stationPos.longitude)
            }
            clusterer!!.addItem(geoItem)
        }
        clusterer!!.redraw()
        if (myPos == null || (myPos?.latitude == 0.0 && myPos?.longitude == 0.0)) {
            myPos = LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2)
        }
        updatePosition()
    }

    private fun isPendingUpload(station: Station, uploadList: List<Upload>) =
        uploadList.any { it.isPendingPhotoUpload && station.id == it.stationId && station.country == it.country }

    public override fun onResume() {
        super.onResume()
        (layer as? TileDownloadLayer)?.onResume()
        if (preferencesService.lastUpdate == 0L) {
            runUpdateCountriesAndStations()
        } else {
            reloadMap()
        }
        if (preferencesService.isLocationUpdates) {
            registerLocationManager()
        }
        binding.map.stationFilterBar.init(preferencesService, dbAdapter, this)
        binding.map.stationFilterBar.setSortOrderEnabled(false)
    }

    private fun createClusterManager() {
        clusterer = ClusterManager(
            binding.map.mapView,
            createMarkerBitmaps(), 9.toByte(),
            this
        )
        // this uses the framebuffer position, the mapview position can be out of sync with
        // what the user sees on the screen if an animation is in progress
        binding.map.mapView.model.frameBufferModel.addObserver(clusterer)
    }

    override fun onPause() {
        (layer as? TileDownloadLayer)?.onPause()
        unregisterLocationManager()
        val mapPosition = binding.map.mapView.model.mapViewPosition.mapPosition
        preferencesService.lastMapPosition = mapPosition
        destroyClusterManager()
        super.onPause()
    }

    private fun destroyClusterManager() {
        if (clusterer != null) {
            clusterer!!.destroyGeoClusterer()
            binding.map.mapView.model.frameBufferModel.removeObserver(clusterer)
            clusterer = null
        }
    }

    override fun onLocationChanged(location: Location) {
        myPos = LatLong(location.latitude, location.longitude)
        updatePosition()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FINE_LOCATION) {
            Log.i(TAG, "Received response for location permission request.")

            // Check if the required permission has been granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerLocationManager()
            } else {
                Toast.makeText(
                    this@MapsActivity,
                    R.string.grant_location_permission,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun registerLocationManager() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (!askedForPermission) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FINE_LOCATION
                    )
                    askedForPermission = true
                }
                setMyLocSwitch(false)
                return
            }
            locationManager =
                applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

            // getting GPS status
            val isGPSEnabled = locationManager!!
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MILLIS_BETWEEN_UPDATES,
                    MIN_DISTANCE_METERS_CHANGE_FOR_UPDATES.toFloat(), this
                )
                Log.d(TAG, "GPS Enabled")
                if (locationManager != null) {
                    val loc = locationManager!!
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    myPos = LatLong(loc!!.latitude, loc.longitude)
                }
            } else {
                // getting network status
                val isNetworkEnabled = locationManager!!
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_MILLIS_BETWEEN_UPDATES,
                        MIN_DISTANCE_METERS_CHANGE_FOR_UPDATES.toFloat(), this
                    )
                    Log.d(TAG, "Network Location enabled")
                    if (locationManager != null) {
                        val loc = locationManager!!
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        myPos = LatLong(loc!!.latitude, loc.longitude)
                    }
                }
            }
            setMyLocSwitch(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering LocationManager", e)
            locationManager = null
            myPos = null
            setMyLocSwitch(false)
            return
        }
        updatePosition()
    }

    private fun unregisterLocationManager() {
        if (locationManager != null) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager!!.removeUpdates(this)
            }
            locationManager = null
        }
        Log.i(TAG, "LocationManager unregistered")
    }

    private fun updatePosition() {
        if (isUpdateMyLocationActive()) {
            binding.map.mapView.setCenter(myPos)
            binding.map.mapView.repaint()
        }
    }

    private fun isUpdateMyLocationActive() = myLocSwitch?.isChecked == true

    inner class BahnhofGeoItem(
        var station: Station,
        override val isPendingUpload: Boolean
    ) : GeoItem {
        override val latLong: LatLong = LatLong(station.lat, station.lon)

        override val title: String
            get() = station.title

        override fun hasPhoto(): Boolean {
            return station.hasPhoto()
        }

        override fun ownPhoto(): Boolean {
            return hasPhoto() && station.photographer == nickname
        }

        override fun stationActive(): Boolean {
            return station.active
        }
    }

    private class MapMenuListener(
        mapsActivity: MapsActivity,
        private val preferencesService: PreferencesService,
        private val map: String?
    ) : MenuItem.OnMenuItemClickListener {
        private val mapsActivityRef = WeakReference(mapsActivity)

        override fun onMenuItemClick(item: MenuItem): Boolean {
            item.isChecked = true
            if (item.itemId == R.id.osm_mapnik) { // default Mapnik online tiles
                preferencesService.map = null
            } else {
                preferencesService.map = map
            }
            val mapsActivity = mapsActivityRef.get()
            mapsActivity?.recreate()
            return false
        }
    }

    private class MapThemeMenuListener(
        mapsActivity: MapsActivity,
        private val preferencesService: PreferencesService,
        private val mapThemeUri: Uri?
    ) : MenuItem.OnMenuItemClickListener {
        private val mapsActivityRef = WeakReference(mapsActivity)

        override fun onMenuItemClick(item: MenuItem): Boolean {
            item.isChecked = true
            if (item.itemId == R.id.default_theme) { // default theme
                preferencesService.setMapThemeUri(null)
            } else {
                preferencesService.setMapThemeUri(mapThemeUri)
            }
            mapsActivityRef.get()?.recreate()
            return false
        }
    }

}