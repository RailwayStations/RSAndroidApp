/*
 * Derived from https://github.com/mapsforge/mapsforge/blob/master/mapsforge-samples-android/src/main/java/org/mapsforge/samples/android/Utils.java
 */
package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.view.drawToBitmap
import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory

/**
 * Utility functions that can be used across different mapsforge based
 * activities.
 */
object Utils {

    /**
     * Compatibility method.
     *
     * @param view       the view to set the background on
     * @param background the background
     */
    fun setBackground(view: View, background: Drawable?) {
        view.background = background
    }

    fun viewToBitmap(c: Context, view: View): Bitmap {
        view.measure(
            MeasureSpec.getSize(view.measuredWidth),
            MeasureSpec.getSize(view.measuredHeight)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val drawable = BitmapDrawable(
            c.resources,
            view.drawToBitmap()
        )
        return AndroidGraphicFactory.convertToBitmap(drawable)
    }
}