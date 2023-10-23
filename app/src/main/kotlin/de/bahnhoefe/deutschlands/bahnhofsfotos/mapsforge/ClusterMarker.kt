/*
 * Derived from https://github.com/mapsforge/mapsforge/blob/master/mapsforge-samples-android/src/main/java/org/mapsforge/samples/android/cluster/ClusterMarker.java
 */
package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import android.util.Log
import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rectangle
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.layer.Layer

/**
 * Layer extended class to display Clustered Marker.
 *
 * @param <T>
 */
class ClusterMarker<T : GeoItem>(private val cluster: Cluster<T>) : Layer() {

    /**
     * icon marker type
     */
    private var markerType = 0

    /**
     * change icon bitmaps according to the state and content size.
     */
    private fun setMarkerBitmap() {
        markerType = 0
        while (markerType < (cluster.clusterManager?.markerIconBmps?.size ?: 0)) {

            // Check if the number of items in this cluster is below or equal the limit of the MarkerBitMap
            if (cluster.items.size <= (cluster.clusterManager?.markerIconBmps?.get(markerType)?.itemMax
                    ?: 0)
            ) {
                return
            }
            markerType++
        }
        // set the markerType to maximum value ==> reduce markerType by one.
        markerType--
    }

    @Synchronized
    override fun draw(
        boundingBox: BoundingBox, zoomLevel: Byte,
        canvas: Canvas, topLeftPoint: Point
    ) {
        if (cluster.clusterManager == null ||
            latLong == null
        ) {
            return
        }
        setMarkerBitmap()
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val pixelX = MercatorProjection.longitudeToPixelX(latLong!!.longitude, mapSize)
        val pixelY = MercatorProjection.latitudeToPixelY(latLong!!.latitude, mapSize)
        val halfBitmapWidth: Double
        val halfBitmapHeight: Double
        val markerBitmap = cluster.clusterManager!!.markerIconBmps[markerType]
        val bitmap =
            markerBitmap.getBitmap(hasPhoto(), ownPhoto(), stationActive(), isPendingUpload)
        try {
            halfBitmapWidth = (bitmap.width / 2f).toDouble()
            halfBitmapHeight = (bitmap.height / 2f).toDouble()
        } catch (e: NullPointerException) {
            Log.e(TAG, e.message, e)
            return
        }
        val left = (pixelX - topLeftPoint.x - halfBitmapWidth + markerBitmap.iconOffset.x).toInt()
        val top = (pixelY - topLeftPoint.y - halfBitmapHeight + markerBitmap.iconOffset.y).toInt()
        val right = left + bitmap.width
        val bottom = top + bitmap.height
        val bitmapRectangle =
            Rectangle(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
        val canvasRectangle = Rectangle(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return
        }
        // Draw bitmap
        canvas.drawBitmap(bitmap, left, top)

        // Draw Text
        if (zoomLevel > 13) {
            if (markerType == 0) {
                // Draw bitmap
                MarkerBitmap.getBitmapFromTitle(
                    cluster.title,
                    markerBitmap.paint
                )?.let {
                    canvas.drawBitmap(
                        it, (left + halfBitmapWidth - it.width / 2).toInt(),
                        top - it.height
                    )
                }
            } else {
                val x = (left + bitmap.width * 1.3).toInt()
                val y =
                    (top + bitmap.height * 1.3 + markerBitmap.paint.getTextHeight(cluster.title) / 2).toInt()
                canvas.drawText(
                    cluster.title, x, y,
                    markerBitmap.paint
                )
            }
        }
    }

    private val latLong: LatLong?
        /**
         * get center location of the marker.
         *
         * @return GeoPoint object of current marker center.
         */
        get() = cluster.location

    /**
     * @return Gets the LatLong Position of the Layer Object
     */
    override fun getPosition(): LatLong {
        return latLong!!
    }

    @Synchronized
    override fun onTap(
        geoPoint: LatLong, viewPosition: Point,
        tapPoint: Point
    ): Boolean {
        if (cluster.items.size == 1 && contains(viewPosition, tapPoint)) {
            Log.w(
                TAG, "The Marker was touched with onTap: "
                        + this.position.toString()
            )
            cluster.clusterManager?.onTap(cluster.items[0])
            requestRedraw()
            return true
        } else if (contains(viewPosition, tapPoint)) {
            val builder = StringBuilder(cluster.items.size.toString() + " items:")
                .append(cluster.items
                    .map { i ->
                        i.title
                    }
                    .take(6)
                    .joinToString("\n- ", "\n- "))
            if (cluster.items.size > 6) {
                builder.append("\n...")
            }
            ClusterManager.toast?.let {
                it.setText(builder)
                it.show()
            }
        }
        return false
    }

    @Synchronized
    fun contains(viewPosition: Point, tapPoint: Point?): Boolean {
        return getBitmapRectangle(viewPosition)?.contains(tapPoint) ?: false
    }

    private fun getBitmapRectangle(center: Point): Rectangle? {
        val markerBitmap = cluster.clusterManager?.markerIconBmps?.get(markerType)
        val bitmap =
            markerBitmap?.getBitmap(hasPhoto(), ownPhoto(), stationActive(), isPendingUpload)
        if (markerBitmap != null && bitmap != null) {
            return Rectangle(
                center.x - bitmap.width.toFloat() + markerBitmap.iconOffset.x,
                center.y - bitmap.height.toFloat() + markerBitmap.iconOffset.y,
                center.x + bitmap.width.toFloat() + markerBitmap.iconOffset.x,
                center.y + bitmap.height.toFloat() + markerBitmap.iconOffset.y
            )
        }
        return null
    }

    fun hasPhoto(): Boolean {
        return cluster.items.size == 1 &&
                cluster.items[0]!!.hasPhoto()
    }

    private fun ownPhoto(): Boolean {
        return cluster.items.size == 1 &&
                cluster.items[0]!!.ownPhoto()
    }

    private fun stationActive(): Boolean {
        return cluster.items.size == 1 &&
                cluster.items[0]!!.stationActive()
    }

    private val isPendingUpload: Boolean
        get() = cluster.items.size == 1 &&
                cluster.items[0]!!.isPendingUpload

    companion object {
        private const val TAG = "ClusterMarker"
    }
}