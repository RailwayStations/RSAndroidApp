package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.Manifest
import android.app.AlertDialog
import android.app.SearchManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import de.bahnhoefe.deutschlands.bahnhofsfotos.NearbyNotificationService.StatusBinder
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMainBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.StationListAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.AppInfoFragment
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.StationFilterBar
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity(), LocationListener,
    NavigationView.OnNavigationItemSelectedListener, StationFilterBar.OnChangeListener {
    private lateinit var baseApplication: BaseApplication
    private lateinit var dbAdapter: DbAdapter
    private lateinit var binding: ActivityMainBinding
    private var stationListAdapter: StationListAdapter? = null
    private var searchString: String? = null
    private var statusBinder: StatusBinder? = null
    private lateinit var rsapiClient: RSAPIClient
    private var myPos: Location? = null
    private var locationManager: LocationManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        baseApplication = application as BaseApplication
        dbAdapter = baseApplication.dbAdapter
        rsapiClient = baseApplication.rsapiClient
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.appBarMain.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        val header = binding.navView.getHeaderView(0)
        val tvUpdate = header.findViewById<TextView>(R.id.tvUpdate)
        if (!baseApplication.firstAppStart) {
            startActivity(Intent(this, IntroSliderActivity::class.java))
            finish()
        }
        val lastUpdateDate = baseApplication.lastUpdate
        if (lastUpdateDate > 0) {
            tvUpdate.text = getString(
                R.string.last_update_at,
                SimpleDateFormat.getDateTimeInstance().format(lastUpdateDate)
            )
        } else {
            tvUpdate.setText(R.string.no_stations_in_database)
        }
        val searchIntent = intent
        if (Intent.ACTION_SEARCH == searchIntent.action) {
            searchString = searchIntent.getStringExtra(SearchManager.QUERY)
        }
        myPos = baseApplication.lastLocation
        bindToStatus()
        binding.appBarMain.main.pullToRefresh.setOnRefreshListener {
            runUpdateCountriesAndStations()
            binding.appBarMain.main.pullToRefresh.isRefreshing = false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val manager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchMenu = menu.findItem(R.id.search)
        val search = searchMenu.actionView as SearchView?
        search!!.setSearchableInfo(manager.getSearchableInfo(componentName))
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                Log.d(TAG, "onQueryTextSubmit: $s")
                searchString = s
                updateStationList()
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                Log.d(TAG, "onQueryTextChange: $s")
                searchString = s
                updateStationList()
                return false
            }
        })
        val updatePolicy = baseApplication.updatePolicy
        menu.findItem(updatePolicy.id).isChecked = true
        return true
    }

    private fun updateStationList() {
        try {
            val sortByDistance = baseApplication.sortByDistance && myPos != null
            val stationCount = dbAdapter.countStations(baseApplication.countryCodes)
            val cursor = dbAdapter.getStationsListByKeyword(
                searchString,
                baseApplication.stationFilter,
                baseApplication.countryCodes,
                sortByDistance,
                myPos
            )
            if (stationListAdapter != null) {
                stationListAdapter!!.swapCursor(cursor)
            } else {
                stationListAdapter = StationListAdapter(this, cursor, 0)
                binding.appBarMain.main.lstStations.adapter = stationListAdapter
                binding.appBarMain.main.lstStations.onItemClickListener =
                    OnItemClickListener { _, _, _, id ->
                        val intentDetails = Intent(this@MainActivity, DetailsActivity::class.java)
                        intentDetails.putExtra(
                            DetailsActivity.EXTRA_STATION,
                            dbAdapter.fetchStationByRowId(id)
                        )
                        startActivity(intentDetails)
                    }
            }
            binding.appBarMain.main.filterResult.text =
                getString(R.string.filter_result, stationListAdapter!!.count, stationCount)
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled Exception in onQueryTextSubmit", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        // necessary for the update policy submenu
        item.isChecked = !item.isChecked
        when (id) {
            R.id.rb_update_manual -> {
                baseApplication.updatePolicy = UpdatePolicy.MANUAL
            }

            R.id.rb_update_automatic -> {
                baseApplication.updatePolicy = UpdatePolicy.AUTOMATIC
            }

            R.id.rb_update_notify -> {
                baseApplication.updatePolicy = UpdatePolicy.NOTIFY
            }

            R.id.apiUrl -> {
                showApiUrlDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showApiUrlDialog() {
        SimpleDialogs.prompt(
            this,
            R.string.apiUrl,
            EditorInfo.TYPE_TEXT_VARIATION_URI,
            R.string.api_url_hint,
            baseApplication.apiUrl
        ) { v: String? ->
            baseApplication.apiUrl = v
            baseApplication.lastUpdate = 0
            recreate()
        }
    }

    private fun setNotificationIcon(active: Boolean) {
        val item = binding.navView.menu.findItem(R.id.nav_notification)
        item.icon = ContextCompat.getDrawable(
            this,
            if (active) R.drawable.ic_notifications_active_gray_24px else R.drawable.ic_notifications_off_gray_24px
        )
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_slideshow -> {
                startActivity(Intent(this, IntroSliderActivity::class.java))
                finish()
            }

            R.id.nav_your_data -> {
                startActivity(Intent(this, MyDataActivity::class.java))
            }

            R.id.nav_update_photos -> {
                runUpdateCountriesAndStations()
            }

            R.id.nav_notification -> {
                toggleNotification()
            }

            R.id.nav_highscore -> {
                startActivity(Intent(this, HighScoreActivity::class.java))
            }

            R.id.nav_outbox -> {
                startActivity(Intent(this, OutboxActivity::class.java))
            }

            R.id.nav_inbox -> {
                startActivity(Intent(this, InboxActivity::class.java))
            }

            R.id.nav_stations_map -> {
                startActivity(Intent(this, MapsActivity::class.java))
            }

            R.id.nav_web_site -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://railway-stations.org")))
            }

            R.id.nav_email -> {
                val emailIntent =
                    Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:" + getString(R.string.fab_email))
                    )
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject))
                startActivity(
                    Intent.createChooser(
                        emailIntent,
                        getString(R.string.fab_chooser_title)
                    )
                )
            }

            R.id.nav_app_info -> {
                AppInfoFragment().show(supportFragmentManager, DIALOG_TAG)
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun runUpdateCountriesAndStations() {
        binding.appBarMain.main.progressBar.visibility = View.VISIBLE
        rsapiClient.runUpdateCountriesAndStations(this, baseApplication) { success: Boolean ->
            if (success) {
                val tvUpdate = findViewById<TextView>(R.id.tvUpdate)
                tvUpdate.text = getString(
                    R.string.last_update_at,
                    SimpleDateFormat.getDateTimeInstance().format(baseApplication.lastUpdate)
                )
                updateStationList()
            }
            binding.appBarMain.main.progressBar.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterLocationManager()
    }

    public override fun onResume() {
        super.onResume()
        for (i in 0 until binding.navView.menu.size()) {
            binding.navView.menu.getItem(i).isChecked = false
        }
        if (baseApplication.lastUpdate == 0L) {
            runUpdateCountriesAndStations()
        } else if (System.currentTimeMillis() - baseApplication.lastUpdate > CHECK_UPDATE_INTERVAL) {
            baseApplication.lastUpdate = System.currentTimeMillis()
            if (baseApplication.updatePolicy !== UpdatePolicy.MANUAL) {
                for (country in baseApplication.countryCodes) {
                    rsapiClient.getStatistic(country)!!.enqueue(object : Callback<Statistic?> {
                        override fun onResponse(
                            call: Call<Statistic?>,
                            response: Response<Statistic?>
                        ) {
                            if (response.isSuccessful) {
                                checkForUpdates(response.body(), country)
                            }
                        }

                        override fun onFailure(call: Call<Statistic?>, t: Throwable) {
                            Log.e(TAG, "Error loading country statistic", t)
                        }
                    })
                }
            }
        }
        if (baseApplication.sortByDistance) {
            registerLocationManager()
        }
        binding.appBarMain.main.stationFilterBar.init(baseApplication, this)
        updateStationList()
    }

    private fun checkForUpdates(statistic: Statistic?, country: String?) {
        if (statistic == null) {
            return
        }
        val dbStat = dbAdapter.getStatistic(country)
        Log.d(TAG, "DbStat: $dbStat")
        if (statistic.total != dbStat!!.total || statistic.withPhoto != dbStat.withPhoto || statistic.withoutPhoto != dbStat.withoutPhoto) {
            if (baseApplication.updatePolicy === UpdatePolicy.AUTOMATIC) {
                runUpdateCountriesAndStations()
            } else {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.update_available)
                    .setCancelable(true)
                    .setPositiveButton(R.string.button_ok_text) { dialog: DialogInterface, _: Int ->
                        runUpdateCountriesAndStations()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel_text) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .create().show()
            }
        }
    }

    private fun bindToStatus() {
        val intent = Intent(this, NearbyNotificationService::class.java)
        intent.action = NearbyNotificationService.Companion.STATUS_INTERFACE
        if (!this.applicationContext.bindService(intent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    Log.d(TAG, "Bound to status service of NearbyNotificationService")
                    statusBinder = service as StatusBinder
                    invalidateOptionsMenu()
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    Log.d(TAG, "Unbound from status service of NearbyNotificationService")
                    statusBinder = null
                    invalidateOptionsMenu()
                }
            }, 0)) {
            Log.e(TAG, "Bind request to statistics interface failed")
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult<String, Boolean>(ActivityResultContracts.RequestPermission()) { isGranted: Boolean? ->
            if (!isGranted!!) {
                Toast.makeText(
                    this@MainActivity,
                    R.string.notification_permission_needed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun toggleNotification() {
        if (statusBinder == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        toggleNotificationWithPermissionGranted()
    }

    private fun toggleNotificationWithPermissionGranted() {
        val intent = Intent(this@MainActivity, NearbyNotificationService::class.java)
        if (statusBinder == null) {
            startService(intent)
            bindToStatus()
            setNotificationIcon(true)
        } else {
            stopService(intent)
            setNotificationIcon(false)
        }
    }

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
                // Location permission has been granted
                registerLocationManager()
            } else {
                //Permission not granted
                baseApplication.sortByDistance = false
                binding.appBarMain.main.stationFilterBar.setSortOrder(false)
            }
        }
    }

    private fun registerLocationManager() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FINE_LOCATION
                )
                return
            }
            locationManager =
                applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

            // getting GPS status
            val isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                )
                Log.d(TAG, "GPS Enabled")
                if (locationManager != null) {
                    myPos = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
            } else {
                // getting network status
                val isNetworkEnabled = locationManager!!
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
                        myPos = locationManager!!
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering LocationManager", e)
            val b = Bundle()
            b.putString("error", "Error registering LocationManager: $e")
            locationManager = null
            baseApplication.sortByDistance = false
            binding.appBarMain.main.stationFilterBar.setSortOrder(false)
            return
        }
        Log.i(TAG, "LocationManager registered")
        onLocationChanged(myPos!!)
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

    override fun onLocationChanged(location: Location) {
        myPos = location
        updateStationList()
    }

    override fun stationFilterChanged(stationFilter: StationFilter) {
        baseApplication.stationFilter = stationFilter
        updateStationList()
    }

    override fun sortOrderChanged(sortByDistance: Boolean) {
        if (sortByDistance) {
            registerLocationManager()
        }
        updateStationList()
    }

    companion object {
        private const val DIALOG_TAG = "App Info Dialog"
        private const val CHECK_UPDATE_INTERVAL = (10 * 60 * 1000 // 10 minutes
                ).toLong()
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_FINE_LOCATION = 1

        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 1000

        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES: Long = 500 // minute
    }
}