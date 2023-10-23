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
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
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
import java.util.Optional
import java.util.function.Function

class MapsActivity : AppCompatActivity(), LocationListener, TapHandler<BahnhofGeoItem>,
    StationFilterBar.OnChangeListener {
    private val onlineTileSources: MutableMap<String?, OnlineTileSource> = HashMap()
    protected var layer: Layer? = null
    protected var clusterer: ClusterManager<BahnhofGeoItem>? = null
    protected val tileCaches: MutableList<TileCache> = ArrayList()
    private var myPos: LatLong? = null
    private var myLocSwitch: CheckBox? = null
    private var dbAdapter: DbAdapter? = null
    private var nickname: String? = null
    private var baseApplication: BaseApplication? = null
    private var locationManager: LocationManager? = null
    private var askedForPermission = false
    private var missingMarker: Marker? = null
    private var binding: ActivityMapsBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidGraphicFactory.createInstance(this.application)
        binding = ActivityMapsBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("#c71c4d")
        setSupportActionBar(binding!!.mapsToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        baseApplication = application as BaseApplication
        dbAdapter = baseApplication.getDbAdapter()
        nickname = baseApplication.getNickname()
        val intent = intent
        var extraMarker: Marker? = null
        if (intent != null) {
            val latitude = intent.getSerializableExtra(EXTRAS_LATITUDE) as Double?
            val longitude = intent.getSerializableExtra(EXTRAS_LONGITUDE) as Double?
            setMyLocSwitch(false)
            if (latitude != null && longitude != null) {
                myPos = LatLong(latitude, longitude)
            }
            val markerRes = intent.getSerializableExtra(EXTRAS_MARKER) as Int?
            if (markerRes != null) {
                extraMarker = createBitmapMarker(myPos, markerRes)
            }
        }
        addDBSTileSource(R.string.dbs_osm_basic, "/styles/dbs-osm-basic/")
        addDBSTileSource(R.string.dbs_osm_railway, "/styles/dbs-osm-railway/")
        createMapViews()
        createTileCaches()
        checkPermissionsAndCreateLayersAndControls()
        if (extraMarker != null) {
            binding!!.map.mapView.layerManager.layers.add(extraMarker)
        }
    }

    private fun addDBSTileSource(nameResId: Int, baseUrl: String) {
        val dbsBasic = DbsTileSource(getString(nameResId), baseUrl)
        onlineTileSources[dbsBasic.name] = dbsBasic
    }

    protected fun createTileCaches() {
        tileCaches.add(
            AndroidUtil.createTileCache(
                this, persistableId,
                binding!!.map.mapView.model.displayModel.tileSize, screenRatio,
                binding!!.map.mapView.model.frameBufferModel.overdrawFactor, true
            )
        )
    }

    protected val persistableId: String
        /**
         * The persistable ID is used to store settings information, like the center of the last view
         * and the zoomlevel. By default the simple name of the class is used. The value is not user
         * visibile.
         *
         * @return the id that is used to save this mapview.
         */
        protected get() = this.javaClass.simpleName
    protected val screenRatio: Float
        /**
         * Returns the relative size of a map view in relation to the screen size of the device. This
         * is used for cache size calculations.
         * By default this returns 1.0, for a full size map view.
         *
         * @return the screen ratio of the mapview
         */
        protected get() = 1.0f

    /**
     * Hook to check for Android Runtime Permissions.
     */
    protected fun checkPermissionsAndCreateLayersAndControls() {
        createLayers()
        createControls()
    }

    /**
     * Hook to create controls, such as scale bars.
     * You can add more controls.
     */
    protected fun createControls() {
        initializePosition(binding!!.map.mapView.model.mapViewPosition)
    }

    /**
     * initializes the map view position.
     *
     * @param mvp the map view position to be set
     */
    protected fun initializePosition(mvp: IMapViewPosition) {
        if (myPos != null) {
            mvp.mapPosition = MapPosition(myPos, baseApplication.getZoomLevelDefault())
        } else {
            mvp.mapPosition = baseApplication.getLastMapPosition()
        }
        mvp.zoomLevelMax = zoomLevelMax
        mvp.zoomLevelMin = zoomLevelMin
    }

    /**
     * Template method to create the map views.
     */
    protected fun createMapViews() {
        binding!!.map.mapView.isClickable = true
        binding!!.map.mapView.setOnMapDragListener { myLocSwitch!!.isChecked = false }
        binding!!.map.mapView.mapScaleBar.isVisible = true
        binding!!.map.mapView.setBuiltInZoomControls(true)
        binding!!.map.mapView.mapZoomControls.isAutoHide = true
        binding!!.map.mapView.mapZoomControls.zoomLevelMin = zoomLevelMin
        binding!!.map.mapView.mapZoomControls.zoomLevelMax = zoomLevelMax
        binding!!.map.mapView.mapZoomControls.setZoomControlsOrientation(MapZoomControls.Orientation.VERTICAL_IN_OUT)
        binding!!.map.mapView.mapZoomControls.setZoomInResource(R.drawable.zoom_control_in)
        binding!!.map.mapView.mapZoomControls.setZoomOutResource(R.drawable.zoom_control_out)
        binding!!.map.mapView.mapZoomControls.setMarginHorizontal(
            resources.getDimensionPixelOffset(
                R.dimen.controls_margin
            )
        )
        binding!!.map.mapView.mapZoomControls.setMarginVertical(resources.getDimensionPixelOffset(R.dimen.controls_margin))
    }

    protected val zoomLevelMax: Byte
        protected get() = binding!!.map.mapView.model.mapViewPosition.zoomLevelMax
    protected val zoomLevelMin: Byte
        protected get() = binding!!.map.mapView.model.mapViewPosition.zoomLevelMin

    /**
     * Hook to purge tile caches.
     * By default we purge every tile cache that has been added to the tileCaches list.
     */
    protected fun purgeTileCaches() {
        for (tileCache in tileCaches) {
            tileCache.purge()
        }
        tileCaches.clear()
    }

    protected val renderTheme: XmlRenderTheme
        protected get() {
            val mapTheme = baseApplication.getMapThemeUri()
            return mapTheme!!.map { m: Uri? ->
                try {
                    val renderThemeFile = DocumentFile.fromSingleUri(application, m!!)
                    return@map StreamRenderTheme(
                        "/assets/", contentResolver.openInputStream(
                            renderThemeFile!!.uri
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading theme $mapTheme", e)
                    return@map InternalRenderTheme.DEFAULT
                }
            }.orElse(InternalRenderTheme.DEFAULT)
        }
    protected val mapFile: Optional<MapDataStore?>
        protected get() {
            val mapUri: Optional<Uri> = BaseApplication.Companion.toUri(baseApplication.getMap())
            return mapUri.map { map: Uri? ->
                if (!DocumentFile.isDocumentUri(this, map)) {
                    return@map null
                }
                try {
                    val inputStream = contentResolver.openInputStream(map!!) as FileInputStream?
                    return@map MapFile(inputStream, 0, null)
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "Can't open mapFile", e)
                }
                null
            }
        }

    protected fun createLayers() {
        val mapFile = mapFile
        if (mapFile.isPresent) {
            val rendererLayer: TileRendererLayer = object : TileRendererLayer(
                tileCaches[0],
                mapFile.get(),
                binding!!.map.mapView.model.mapViewPosition,
                false,
                true,
                false,
                AndroidGraphicFactory.INSTANCE
            ) {
                override fun onLongPress(
                    tapLatLong: LatLong, thisXY: Point,
                    tapXY: Point
                ): Boolean {
                    this@MapsActivity.onLongPress(tapLatLong)
                    return true
                }
            }
            rendererLayer.setXmlRenderTheme(renderTheme)
            layer = rendererLayer
            binding!!.map.mapView.layerManager.layers.add(layer)
        } else {
            var tileSource: AbstractTileSource? = onlineTileSources[baseApplication.getMap()]
            if (tileSource == null) {
                tileSource = OpenStreetMapMapnik.INSTANCE
            }
            tileSource!!.userAgent = USER_AGENT
            layer = object : TileDownloadLayer(
                tileCaches[0],
                binding!!.map.mapView.model.mapViewPosition, tileSource,
                AndroidGraphicFactory.INSTANCE
            ) {
                override fun onLongPress(
                    tapLatLong: LatLong, thisXY: Point,
                    tapXY: Point
                ): Boolean {
                    this@MapsActivity.onLongPress(tapLatLong)
                    return true
                }
            }
            binding!!.map.mapView.layerManager.layers.add(layer)
            binding!!.map.mapView.setZoomLevelMin(tileSource.zoomLevelMin)
            binding!!.map.mapView.setZoomLevelMax(tileSource.zoomLevelMax)
        }
    }

    private fun createBitmapMarker(latLong: LatLong?, markerRes: Int): Marker {
        val drawable = ContextCompat.getDrawable(this, markerRes)!!
        val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
        return Marker(latLong, bitmap, -(bitmap.width / 2), -bitmap.height)
    }

    private fun onLongPress(tapLatLong: LatLong) {
        if (missingMarker == null) {
            // marker to show at the location
            val drawable = ContextCompat.getDrawable(this, R.drawable.marker_missing)!!
            val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
            missingMarker =
                object : Marker(tapLatLong, bitmap, -(bitmap.width / 2), -bitmap.height) {
                    override fun onTap(tapLatLong: LatLong, layerXY: Point, tapXY: Point): Boolean {
                        SimpleDialogs.confirmOkCancel(
                            this@MapsActivity,
                            R.string.add_missing_station
                        ) { dialogInterface: DialogInterface?, i: Int ->
                            val intent = Intent(this@MapsActivity, UploadActivity::class.java)
                            intent.putExtra(
                                UploadActivity.Companion.EXTRA_LATITUDE,
                                latLong.latitude
                            )
                            intent.putExtra(
                                UploadActivity.Companion.EXTRA_LONGITUDE,
                                latLong.longitude
                            )
                            startActivity(intent)
                        }
                        return false
                    }
                }
            binding!!.map.mapView.layerManager.layers.add(missingMarker)
        } else {
            missingMarker!!.latLong = tapLatLong
            missingMarker!!.requestRedraw()
        }

        // feedback for long click
        (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
            VibrationEffect.createOneShot(
                150,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.maps, menu)
        val item = menu.findItem(R.id.menu_toggle_mypos)
        myLocSwitch = CheckBox(this)
        myLocSwitch!!.setButtonDrawable(R.drawable.ic_gps_fix_selector)
        myLocSwitch!!.isChecked = baseApplication!!.isLocationUpdates
        item.actionView = myLocSwitch
        myLocSwitch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            baseApplication.setLocationUpdates(isChecked)
            if (isChecked) {
                askedForPermission = false
                registerLocationManager()
            } else {
                unregisterLocationManager()
            }
        }
        val map = baseApplication.getMap()
        val osmMapnick = menu.findItem(R.id.osm_mapnik)
        osmMapnick.isChecked = map == null
        osmMapnick.setOnMenuItemClickListener(MapMenuListener(this, baseApplication, null))
        val mapSubmenu = menu.findItem(R.id.maps_submenu).subMenu!!
        for (tileSource in onlineTileSources.values) {
            val mapItem = mapSubmenu.add(R.id.maps_group, Menu.NONE, Menu.NONE, tileSource.name)
            mapItem.isChecked = tileSource.name == map
            mapItem.setOnMenuItemClickListener(
                MapMenuListener(
                    this,
                    baseApplication,
                    tileSource.name
                )
            )
        }
        val mapDirectory = baseApplication.getMapDirectoryUri()
        if (mapDirectory!!.isPresent) {
            val documentsTree = getDocumentFileFromTreeUri(mapDirectory!!.get())
            if (documentsTree != null) {
                for (file in documentsTree.listFiles()) {
                    if (file.isFile && file.name!!.endsWith(".map")) {
                        val mapItem =
                            mapSubmenu.add(R.id.maps_group, Menu.NONE, Menu.NONE, file.name)
                        mapItem.isChecked = BaseApplication.Companion.toUri(map).map<Boolean>(
                            Function<Uri, Boolean> { uri: Uri -> file.uri == uri }).orElse(false)
                        mapItem.setOnMenuItemClickListener(
                            MapMenuListener(
                                this,
                                baseApplication,
                                file.uri.toString()
                            )
                        )
                    }
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true)
        val mapFolder = mapSubmenu.add(R.string.map_folder)
        mapFolder.setOnMenuItemClickListener { item1: MenuItem? ->
            openMapDirectoryChooser()
            false
        }
        val mapTheme = baseApplication.getMapThemeUri()
        val mapThemeDirectory = baseApplication.getMapThemeDirectoryUri()
        val defaultTheme = menu.findItem(R.id.default_theme)
        defaultTheme.isChecked = !mapTheme!!.isPresent
        defaultTheme.setOnMenuItemClickListener(MapThemeMenuListener(this, baseApplication, null))
        val themeSubmenu = menu.findItem(R.id.themes_submenu).subMenu!!
        if (mapThemeDirectory!!.isPresent) {
            val documentsTree = getDocumentFileFromTreeUri(mapThemeDirectory!!.get())
            if (documentsTree != null) {
                for (file in documentsTree.listFiles()) {
                    if (file.isFile && file.name!!.endsWith(".xml")) {
                        val themeName = file.name
                        val themeItem =
                            themeSubmenu.add(R.id.themes_group, Menu.NONE, Menu.NONE, themeName)
                        themeItem.isChecked = mapTheme!!.map { uri: Uri? -> file.uri == uri }
                            .orElse(false)
                        themeItem.setOnMenuItemClickListener(
                            MapThemeMenuListener(
                                this,
                                baseApplication,
                                file.uri
                            )
                        )
                    } else if (file.isDirectory) {
                        val childFile = file.findFile(file.name + ".xml")
                        if (childFile != null) {
                            val themeName = file.name
                            val themeItem =
                                themeSubmenu.add(R.id.themes_group, Menu.NONE, Menu.NONE, themeName)
                            themeItem.isChecked =
                                mapTheme!!.map { uri: Uri? -> childFile.uri == uri }
                                    .orElse(false)
                            themeItem.setOnMenuItemClickListener(
                                MapThemeMenuListener(
                                    this,
                                    baseApplication,
                                    childFile.uri
                                )
                            )
                        }
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true)
        val themeFolder = themeSubmenu.add(R.string.theme_folder)
        themeFolder.setOnMenuItemClickListener { item12: MenuItem? ->
            openThemeDirectoryChooser()
            false
        }
        return true
    }

    private fun getDocumentFileFromTreeUri(uri: Uri): DocumentFile? {
        try {
            return DocumentFile.fromTreeUri(application, uri)
        } catch (e: Exception) {
            Log.w(TAG, "Error getting DocumentFile from Uri: $uri")
        }
        return null
    }

    protected var themeDirectoryLauncher = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data!!.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                baseApplication!!.setMapThemeDirectoryUri(uri)
                recreate()
            }
        }
    }
    protected var mapDirectoryLauncher = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data!!.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                baseApplication!!.setMapDirectoryUri(uri)
                recreate()
            }
        }
    }

    fun openDirectory(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
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
        binding!!.map.mapView.destroyAll()
        AndroidGraphicFactory.clearResourceMemoryCache()
        purgeTileCaches()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.map_info) {
            MapInfoFragment().show(supportFragmentManager, "Map Info Dialog")
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun reloadMap() {
        destroyClusterManager()
        LoadMapMarkerTask(this).start()
    }

    private fun runUpdateCountriesAndStations() {
        binding!!.map.progressBar.visibility = View.VISIBLE
        baseApplication.getRsapiClient().runUpdateCountriesAndStations(
            this,
            baseApplication
        ) { success: Boolean -> reloadMap() }
    }

    private fun onStationsLoaded(stationList: List<Station?>?, uploadList: List<Upload?>?) {
        try {
            createClusterManager()
            addMarkers(stationList, uploadList)
            binding!!.map.progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                resources.getQuantityString(
                    R.plurals.stations_loaded,
                    stationList!!.size,
                    stationList.size
                ),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading markers", e)
        }
    }

    override fun onTap(marker: BahnhofGeoItem) {
        val intent = Intent(this@MapsActivity, DetailsActivity::class.java)
        val id = marker.station!!.id
        val country = marker.station!!.country
        try {
            val station = dbAdapter!!.getStationByKey(country, id)
            intent.putExtra(DetailsActivity.Companion.EXTRA_STATION, station)
            startActivity(intent)
        } catch (e: RuntimeException) {
            Log.wtf(
                TAG,
                String.format("Could not fetch station id %s that we put onto the map", id),
                e
            )
        }
    }

    fun setMyLocSwitch(checked: Boolean) {
        if (myLocSwitch != null) {
            myLocSwitch!!.isChecked = checked
        }
        baseApplication.setLocationUpdates(checked)
    }

    override fun stationFilterChanged(stationFilter: StationFilter?) {
        reloadMap()
    }

    override fun sortOrderChanged(sortByDistance: Boolean) {
        // unused
    }

    private class LoadMapMarkerTask(activity: MapsActivity) : Thread() {
        private val activityRef: WeakReference<MapsActivity>

        init {
            activityRef = WeakReference(activity)
        }

        override fun run() {
            val stationList = activityRef.get()!!.readStations()
            val uploadList = activityRef.get()!!.readPendingUploads()
            val mapsActivity = activityRef.get()
            mapsActivity?.runOnUiThread { mapsActivity.onStationsLoaded(stationList, uploadList) }
        }
    }

    private fun readStations(): List<Station?>? {
        try {
            return dbAdapter!!.getAllStations(
                baseApplication.getStationFilter(),
                baseApplication.getCountryCodes()
            )
        } catch (e: Exception) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden")
        }
        return null
    }

    private fun readPendingUploads(): List<Upload?>? {
        try {
            return dbAdapter!!.getPendingUploads(false)
        } catch (e: Exception) {
            Log.i(TAG, "Datenbank konnte nicht geöffnet werden")
        }
        return null
    }

    private fun createMarkerBitmaps(): List<MarkerBitmap?> {
        val markerBitmaps = ArrayList<MarkerBitmap?>()
        markerBitmaps.add(createSmallSingleIconMarker())
        markerBitmaps.add(createSmallClusterIconMarker())
        markerBitmaps.add(createLargeClusterIconMarker())
        return markerBitmaps
    }

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
            11f, paint
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
            9f, paint
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
            10f,
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

    private fun addMarkers(stationMarker: List<Station?>?, uploadList: List<Upload?>?) {
        var minLat = 0.0
        var maxLat = 0.0
        var minLon = 0.0
        var maxLon = 0.0
        for (station in stationMarker!!) {
            val isPendingUpload = isPendingUpload(station, uploadList)
            val geoItem = BahnhofGeoItem(station, isPendingUpload)
            val bahnhofPos = geoItem.getLatLong()
            if (minLat == 0.0) {
                minLat = bahnhofPos!!.latitude
                maxLat = bahnhofPos.latitude
                minLon = bahnhofPos.longitude
                maxLon = bahnhofPos.longitude
            } else {
                minLat = Math.min(minLat, bahnhofPos!!.latitude)
                maxLat = Math.max(maxLat, bahnhofPos.latitude)
                minLon = Math.min(minLon, bahnhofPos.longitude)
                maxLon = Math.max(maxLon, bahnhofPos.longitude)
            }
            clusterer!!.addItem(geoItem)
        }
        clusterer!!.redraw()
        if (myPos == null || myPos!!.latitude == 0.0 && myPos!!.longitude == 0.0) {
            myPos = LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2)
        }
        updatePosition()
    }

    private fun isPendingUpload(station: Station?, uploadList: List<Upload?>?): Boolean {
        for (upload in uploadList!!) {
            if (upload!!.isPendingPhotoUpload && station!!.id == upload.stationId && station.country == upload.country) {
                return true
            }
        }
        return false
    }

    public override fun onResume() {
        super.onResume()
        if (layer is TileDownloadLayer) {
            (layer as TileDownloadLayer?)!!.onResume()
        }
        if (baseApplication.getLastUpdate() == 0L) {
            runUpdateCountriesAndStations()
        } else {
            reloadMap()
        }
        if (baseApplication!!.isLocationUpdates) {
            registerLocationManager()
        }
        binding!!.map.stationFilterBar.init(baseApplication, this)
        binding!!.map.stationFilterBar.setSortOrderEnabled(false)
    }

    private fun createClusterManager() {
        // create clusterer instance
        clusterer = ClusterManager(
            binding!!.map.mapView,
            createMarkerBitmaps(), 9.toByte(),
            this
        )
        // this uses the framebuffer position, the mapview position can be out of sync with
        // what the user sees on the screen if an animation is in progress
        binding!!.map.mapView.model.frameBufferModel.addObserver(clusterer)
    }

    override fun onPause() {
        if (layer is TileDownloadLayer) {
            (layer as TileDownloadLayer?)!!.onPause()
        }
        unregisterLocationManager()
        val mapPosition = binding!!.map.mapView.model.mapViewPosition.mapPosition
        baseApplication.setLastMapPosition(mapPosition)
        destroyClusterManager()
        super.onPause()
    }

    private fun destroyClusterManager() {
        if (clusterer != null) {
            clusterer!!.destroyGeoClusterer()
            binding!!.map.mapView.model.frameBufferModel.removeObserver(clusterer)
            clusterer = null
        }
    }

    override fun onLocationChanged(location: Location) {
        myPos = LatLong(location.latitude, location.longitude)
        updatePosition()
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
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
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission has been granted
                registerLocationManager()
            } else {
                //Permission not granted
                Toast.makeText(
                    this@MapsActivity,
                    R.string.grant_location_permission,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun registerLocationManager() {
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
            val isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                )
                Log.d(TAG, "GPS Enabled")
                if (locationManager != null) {
                    val loc = locationManager!!
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    myPos = LatLong(loc!!.latitude, loc.longitude)
                }
            } else {
                // getting network status
                val isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
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
            val b = Bundle()
            b.putString("error", "Error registering LocationManager: $e")
            locationManager = null
            myPos = null
            setMyLocSwitch(false)
            return
        }
        Log.i(TAG, "LocationManager registered")
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
        if (myLocSwitch != null && myLocSwitch!!.isChecked) {
            binding!!.map.mapView.setCenter(myPos)
            binding!!.map.mapView.repaint()
        }
    }

    protected inner class BahnhofGeoItem(
        var station: Station?,
        override val isPendingUpload: Boolean
    ) : GeoItem {
        override val latLong: LatLong

        init {
            latLong = LatLong(station!!.lat, station!!.lon)
        }

        override fun getLatLong(): LatLong? {
            return latLong
        }

        override val title: String?
            get() = station!!.title

        override fun hasPhoto(): Boolean {
            return station!!.hasPhoto()
        }

        override fun ownPhoto(): Boolean {
            return hasPhoto() && station!!.photographer == nickname
        }

        override fun stationActive(): Boolean {
            return station!!.active
        }
    }

    private class MapMenuListener(
        mapsActivity: MapsActivity,
        private val baseApplication: BaseApplication?,
        private val map: String?
    ) : MenuItem.OnMenuItemClickListener {
        private val mapsActivityRef: WeakReference<MapsActivity>

        init {
            mapsActivityRef = WeakReference(mapsActivity)
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            item.isChecked = true
            if (item.itemId == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMap(null)
            } else {
                baseApplication.setMap(map)
            }
            val mapsActivity = mapsActivityRef.get()
            mapsActivity?.recreate()
            return false
        }
    }

    private class MapThemeMenuListener(
        mapsActivity: MapsActivity,
        private val baseApplication: BaseApplication?,
        private val mapThemeUri: Uri?
    ) : MenuItem.OnMenuItemClickListener {
        private val mapsActivityRef: WeakReference<MapsActivity>

        init {
            mapsActivityRef = WeakReference(mapsActivity)
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            item.isChecked = true
            if (item.itemId == R.id.default_theme) { // default theme
                baseApplication!!.setMapThemeUri(null)
            } else {
                baseApplication!!.setMapThemeUri(mapThemeUri)
            }
            val mapsActivity = mapsActivityRef.get()
            mapsActivity?.recreate()
            return false
        }
    }

    companion object {
        const val EXTRAS_LATITUDE = "Extras_Latitude"
        const val EXTRAS_LONGITUDE = "Extras_Longitude"
        const val EXTRAS_MARKER = "Extras_Marker"

        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 1 // meters

        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES: Long = 500 // minute
        private val TAG = MapsActivity::class.java.simpleName
        private const val REQUEST_FINE_LOCATION = 1
        private const val USER_AGENT = "railway-stations.org-android"
    }
}