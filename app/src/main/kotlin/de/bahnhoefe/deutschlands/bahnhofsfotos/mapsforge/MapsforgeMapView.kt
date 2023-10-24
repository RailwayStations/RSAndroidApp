package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import org.mapsforge.map.android.view.MapView

class MapsforgeMapView(context: Context?, attributeSet: AttributeSet?) :
    MapView(context, attributeSet) {
    private val gestureDetector: GestureDetector
    private var onDragListener: MapDragListener? = null

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            gestureDetector.onTouchEvent(ev)
            return super.onTouchEvent(ev)
        } catch (ignored: Exception) {
        }
        return false
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (onDragListener != null) {
                onDragListener!!.onDrag()
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            if (onDragListener != null) {
                onDragListener!!.onDrag()
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }

    fun setOnMapDragListener(onDragListener: MapDragListener?) {
        this.onDragListener = onDragListener
    }

    /**
     * Notifies the parent class when a MapView has been dragged
     */
    fun interface MapDragListener {
        fun onDrag()
    }
}