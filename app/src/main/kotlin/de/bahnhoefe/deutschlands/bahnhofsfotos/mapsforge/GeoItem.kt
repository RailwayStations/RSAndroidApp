/*
 * Derived from https://github.com/mapsforge/mapsforge/blob/master/mapsforge-samples-android/src/main/java/org/mapsforge/samples/android/cluster/GeoItem.java
 */
package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import org.mapsforge.core.model.LatLong

/**
 * Utility Class to handle GeoItem for ClusterMarker
 */
interface GeoItem {
    /**
     * getLatLong
     *
     * @return item location in LatLong.
     */
    val latLong: LatLong

    /**
     * getTitle
     *
     * @return Title of the item, might be used as Caption text.
     */
    val title: String
    fun hasPhoto(): Boolean
    fun ownPhoto(): Boolean

    /**
     * @return true if the station is active
     */
    fun stationActive(): Boolean
    val isPendingUpload: Boolean
}