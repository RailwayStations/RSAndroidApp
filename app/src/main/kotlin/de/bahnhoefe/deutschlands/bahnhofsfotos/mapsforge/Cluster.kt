/*
 * Derived from https://github.com/mapsforge/mapsforge/blob/master/mapsforge-samples-android/src/main/java/org/mapsforge/samples/android/cluster/Cluster.java
 */
package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import org.mapsforge.core.model.LatLong
import java.util.Collections

/**
 * Cluster class.
 * contains single marker object(ClusterMarker). mostly wraps methods in ClusterMarker.
 */
class Cluster<T : GeoItem>(
    var clusterManager: ClusterManager<T>?, item: T
) {
    /**
     * Center of cluster
     */
    var location: LatLong? = null
        private set

    /**
     * List of GeoItem within cluster
     */
    private val items = Collections.synchronizedList(ArrayList<T>())

    private var clusterMarker: ClusterMarker<T>?

    init {
        clusterMarker = ClusterMarker(this)
        addItem(item)
    }

    val title: String
        get() {
            if (getItems().size == 1) {
                return getItems()[0].title
            }
            synchronized(items) {
                return items.count { obj: T -> obj.hasPhoto() }
                    .toString() + "/" + getItems().size
            }
        }

    /**
     * add item to cluster object
     *
     * @param item GeoItem object to be added.
     */
    @Synchronized
    fun addItem(item: T) {
        synchronized(items) { items.add(item) }
        if (location == null) {
            location = item.latLong
        } else {
            // computing the centroid
            var lat = 0.0
            var lon = 0.0
            var n = 0
            synchronized(items) {
                items.forEach {
                    lat += it.latLong.latitude
                    lon += it.latLong.longitude
                    n++
                }
            }
            location = LatLong(lat / n, lon / n)
        }
    }

    /**
     * get list of GeoItem.
     *
     * @return list of GeoItem within cluster.
     */
    @Synchronized
    fun getItems(): List<T> {
        return items
    }

    /**
     * clears cluster object and removes the cluster from the layers collection.
     */
    fun clear() {
        if (clusterMarker != null) {
            val mapOverlays = clusterManager?.mapView?.layerManager?.layers
            if (mapOverlays != null) {
                if (mapOverlays.contains(clusterMarker)) {
                    mapOverlays.remove(clusterMarker)
                }
            }
            clusterManager = null
            clusterMarker = null
        }
        synchronized(items) { items.clear() }
    }

    /**
     * add the ClusterMarker to the Layers if is within Viewport, otherwise remove.
     */
    fun redraw() {
        if (clusterManager == null || clusterMarker == null) {
            return
        }
        val mapOverlays = clusterManager?.mapView?.layerManager?.layers
        if (mapOverlays != null) {
            if (clusterMarker != null && location != null && clusterManager?.curBounds != null && !clusterManager!!.curBounds
                    ?.contains(
                        location
                    )!!
                && mapOverlays.contains(clusterMarker)
            ) {
                mapOverlays.remove(clusterMarker)
                return
            }
        }
        if (mapOverlays != null) {
            if (clusterMarker != null && mapOverlays.size() > 0 && !mapOverlays.contains(
                    clusterMarker
                )
                && !clusterManager!!.isClustering
            ) {
                mapOverlays.add(1, clusterMarker)
            }
        }
    }

    val size: Int
        get() = items.size
}