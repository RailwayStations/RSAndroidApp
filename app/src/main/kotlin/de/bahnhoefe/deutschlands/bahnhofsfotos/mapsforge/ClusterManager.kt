/*
 * Derived from https://github.com/mapsforge/mapsforge/blob/master/mapsforge-samples-android/src/main/java/org/mapsforge/samples/android/cluster/ClusterManager.java
 */
package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import android.util.Log
import android.widget.Toast
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.model.common.Observer
import org.mapsforge.map.view.MapView
import java.util.Collections
import java.util.function.Consumer

private val TAG = ClusterManager::class.java.simpleName
private const val MIN_CLUSTER_SIZE = 5

/**
 * A 'Toast' to display information, intended to show information on [ClusterMarker]
 * with more than one [GeoItem] (while Marker with a single GeoItem should have their
 * own OnClick functions)
 */
var toast: Toast? = null

/**
 * Class for Clustering geotagged content
 */
class ClusterManager<T : GeoItem>(
    val mapView: MapView,
    markerBitmaps: List<MarkerBitmap>, maxClusteringZoom: Byte, tapHandler: TapHandler<T>?
) : Observer, TapHandler<T> {

    /**
     * grid size for Clustering(dip).
     */
    private val gridsize = 60 * DisplayModel.getDeviceScaleFactor()

    /**
     * The maximum (highest) zoom level for clustering the items.,
     */
    private val maxClusteringZoom: Byte

    /**
     * Lock for the re-Clustering of the items.
     */
    var isClustering = false

    /**
     * MarkerBitmap object for marker icons, uses Static access.
     */
    val markerIconBmps: List<MarkerBitmap>

    /**
     * The current BoundingBox of the viewport.
     */
    private var currBoundingBox: BoundingBox? = null

    /**
     * GeoItem ArrayList object that are out of viewport and need to be
     * clustered on panning.
     */
    val leftItems: MutableList<T> = Collections.synchronizedList(ArrayList<T>())

    /**
     * Clustered object list.
     */
    val clusters: MutableList<Cluster<T>> = Collections.synchronizedList(ArrayList<Cluster<T>>())

    /**
     * Handles on click on markers
     */
    private val tapHandler: TapHandler<T>?

    /**
     * saves the actual ZoomLevel of the MapView
     */
    private var oldZoomLevel: Double

    /**
     * saves the actual Center as LatLong of the MapViewPosition
     */
    private var oldCenterLatLong: LatLong
    private var clusterTask: ClusterTask? = null

    init {
        // set to impossible values to trigger clustering at first onChange
        oldZoomLevel = -1.0
        oldCenterLatLong = LatLong(-90.0, -180.0)
        markerIconBmps = markerBitmaps
        this.maxClusteringZoom = maxClusteringZoom
        this.tapHandler = tapHandler
    }

    /**
     * add item and do isClustering. NOTE: this method will not redraw screen.
     * after adding all items, you must call redraw() method.
     *
     * @param item GeoItem to be clustered.
     */
    @Synchronized
    fun addItem(item: T) {
        if (mapView.width == 0 || !isItemInViewport(item)) {
            synchronized(leftItems) {
                if (clusterTask != null && clusterTask!!.isInterrupted) {
                    return
                }
                leftItems.add(item)
            }
        } else if (maxClusteringZoom >= mapView.model.mapViewPosition
                .zoomLevel
        ) {
            // else add to a cluster;
            val pos = mapView.mapViewProjection.toPixels(item.latLong)
            // check existing cluster
            synchronized(clusters) {
                for (mCluster in clusters) {
                    if (clusterTask != null && clusterTask!!.isInterrupted) {
                        return
                    }
                    require(mCluster.getItems().isNotEmpty()) { "cluster.getItems().size() == 0" }

                    // find a cluster which contains the marker.
                    // use 1st element to fix the location, hinder the cluster from
                    // running around and isClustering.
                    val gpCenter =
                        mCluster.getItems()[0].latLong
                    val ptCenter = mapView.mapViewProjection.toPixels(gpCenter)
                    // find a cluster which contains the marker.
                    if (pos.distance(ptCenter) <= gridsize) {
                        mCluster.addItem(item)
                        return
                    }
                }
                // No cluster contain the marker, create a new cluster.
                clusters.add(createCluster(item))
            }
        } else {
            // No clustering allowed, create a new cluster with single item.
            synchronized(clusters) { clusters.add(createCluster(item)) }
        }
    }

    /**
     * Create Cluster Object. override this method, if you want to use custom
     * GeoCluster class.
     *
     * @param item GeoItem to be set to cluster.
     */
    private fun createCluster(item: T): Cluster<T> {
        return Cluster(this, item)
    }

    /**
     * redraws clusters
     */
    @Synchronized
    fun redraw() {
        synchronized(clusters) {
            if (!isClustering) {
                val removed = mutableSetOf<Cluster<T>>()
                val singles = mutableListOf<Cluster<T>>()
                clusters
                    .filter { mCluster -> mCluster.size < MIN_CLUSTER_SIZE }
                    .forEach { mCluster ->
                        mCluster.getItems()
                            .forEach(Consumer { item -> singles.add(createCluster(item)) })
                        mCluster.clear()
                        removed.add(mCluster)
                    }
                clusters.removeAll(removed)
                clusters.addAll(singles)
                for (mCluster in clusters) {
                    mCluster.redraw()
                }
            }
        }
    }

    /**
     * check if the item is within current viewport.
     *
     * @return true if item is within viewport.
     */
    private fun isItemInViewport(item: GeoItem): Boolean {
        val curBounds = curBounds
        return curBounds != null && curBounds.contains(item.latLong)
    }

    @get:Synchronized
    val curBounds: BoundingBox?
        /**
         * get the current BoundingBox of the viewport
         *
         * @return current BoundingBox of the viewport
         */
        get() {
            if (currBoundingBox == null) {
                require(!(mapView.width <= 0 || mapView.height <= 0)) {
                    (" mapView.getWidth() <= 0 " +
                            "|| mapView.getHeight() <= 0 "
                            + mapView.width + " || " + mapView.height)
                }
                /* North-West geo point of the bound */
                val nw = mapView.mapViewProjection.fromPixels(
                    -mapView.width * 0.5,
                    -mapView.height * 0.5
                )
                /* South-East geo point of the bound */
                val se = mapView.mapViewProjection.fromPixels(
                    mapView.width + mapView.width * 0.5,
                    mapView.height + mapView.height * 0.5
                )
                if (se != null && nw != null) {
                    currBoundingBox = if (se.latitude > nw.latitude) {
                        BoundingBox(
                            nw.latitude, se.longitude, se.latitude,
                            nw.longitude
                        )
                    } else {
                        BoundingBox(
                            se.latitude, nw.longitude, nw.latitude,
                            se.longitude
                        )
                    }
                }
            }
            return currBoundingBox
        }

    /**
     * add items that were not clustered in last isClustering.
     */
    private fun addLeftItems() {
        if (leftItems.size == 0) {
            return
        }
        val currentLeftItems = ArrayList(leftItems)
        synchronized(leftItems) { leftItems.clear() }
        currentLeftItems.forEach(Consumer { item: T -> addItem(item) })
    }

    // *********************************************************************************************************************
    // Methods to implement 'Observer'
    // *********************************************************************************************************************
    @Synchronized
    override fun onChange() {
        currBoundingBox = null
        if (isClustering) {
            return
        }
        if (oldZoomLevel != mapView.model.mapViewPosition.zoomLevel.toDouble()) {
            // react on zoom changes
            oldZoomLevel = mapView.model.mapViewPosition.zoomLevel.toDouble()
            resetViewport(false)
        } else {
            // react on position changes
            val mapViewPosition = mapView.model.mapViewPosition
            val posOld = mapView.mapViewProjection.toPixels(oldCenterLatLong)
            val posNew = mapView.mapViewProjection.toPixels(mapViewPosition.center)
            if (posOld != null && posOld.distance(posNew) > gridsize / 2) {
                oldCenterLatLong = mapViewPosition.center
                resetViewport(true)
            }
        }
    }

    /**
     * reset current viewport, re-cluster the items when zoom has changed, else
     * add not clustered items .
     */
    @Synchronized
    private fun resetViewport(isMoving: Boolean) {
        isClustering = true
        clusterTask = ClusterTask(isMoving)
        clusterTask!!.start()
    }

    private fun cancelClusterTask() {
        if (clusterTask != null) {
            clusterTask!!.interrupt()
        }
    }

    @Synchronized
    fun destroyGeoClusterer() {
        synchronized(clusters) {
            clusters.forEach(Consumer { cluster: Cluster<T> ->
                cluster.clusterManager?.cancelClusterTask()
                cluster.clear()
            })
            clusters.clear()
        }
        markerIconBmps.forEach(Consumer { markerBitmap -> markerBitmap!!.decrementRefCounters() })
        synchronized(leftItems) { leftItems.clear() }
        clearCaptionBitmap()
    }

    override fun onTap(item: T) {
        tapHandler?.onTap(item)
    }

    private inner class ClusterTask(private val isMoving: Boolean) : Thread() {
        override fun run() {
            Log.d(TAG, "Run ClusterTask")
            // If the map is moved without zoom-change: Add unclustered items.
            if (isMoving) {
                addLeftItems()
            } else {
                synchronized(clusters) {
                    for (mCluster in clusters) {
                        synchronized(leftItems) { leftItems.addAll(mCluster.getItems()) }
                        mCluster.clear()
                    }
                }
                synchronized(clusters) { clusters.clear() }
                if (!isInterrupted) {
                    synchronized(clusters) { require(clusters.size == 0) }
                    addLeftItems()
                }
            }
            isClustering = false
            redraw()
            Log.d(TAG, "ClusterTask finished")
        }
    }

}