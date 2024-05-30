/*
 * Copyright 2009 Huan Erdao
 * Copyright 2014 Martin Vennekamp
 * Copyright 2015 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.model.Point
import java.lang.ref.WeakReference
import java.util.function.Consumer

/**
 * Utility Class to handle MarkerBitmap
 * it handles grid offset to display on the map with offset
 */
class MarkerBitmap(
    context: Context,
    private val iconBmpWithoutPhoto: Bitmap,
    private val iconBmpWithPhoto: Bitmap,
    private val iconBmpOwnPhoto: Bitmap,
    private val iconBmpWithoutPhotoInactive: Bitmap,
    private val iconBmpWithPhotoInactive: Bitmap,
    private val iconBmpOwnPhotoInactive: Bitmap,
    private val iconBmpPendingUpload: Bitmap,
    val iconOffset: Point,
    private val textSize: Float,
    val itemMax: Int,
    val paint: Paint
) {
    init {
        contextRef = WeakReference(context)
        this.paint.setTextSize(this.textSize)
    }

    constructor(
        context: Context,
        bitmap: Bitmap,
        grid: Point,
        textSize: Float,
        maxSize: Int,
        paint: Paint
    ) : this(
        context,
        bitmap,
        bitmap,
        bitmap,
        bitmap,
        bitmap,
        bitmap,
        bitmap,
        grid,
        textSize,
        maxSize,
        paint
    )

    /**
     * @return bitmap object according to the state of the stations
     */
    fun getBitmap(
        hasPhoto: Boolean,
        ownPhoto: Boolean,
        stationActive: Boolean,
        inbox: Boolean
    ): Bitmap {
        if (inbox) {
            return iconBmpPendingUpload
        }
        if (ownPhoto) {
            return if (stationActive) {
                iconBmpOwnPhoto
            } else iconBmpOwnPhotoInactive
        } else if (hasPhoto) {
            return if (stationActive) {
                iconBmpWithPhoto
            } else iconBmpWithPhotoInactive
        }
        return if (stationActive) {
            iconBmpWithoutPhoto
        } else iconBmpWithoutPhotoInactive
    }

    fun decrementRefCounters() {
        iconBmpOwnPhoto.decrementRefCount()
        iconBmpWithPhoto.decrementRefCount()
        iconBmpWithoutPhoto.decrementRefCount()
        iconBmpOwnPhotoInactive.decrementRefCount()
        iconBmpWithPhotoInactive.decrementRefCount()
        iconBmpWithoutPhotoInactive.decrementRefCount()
    }
}

private val captionViews: MutableMap<String, Bitmap> = HashMap()
private lateinit var contextRef: WeakReference<Context>

fun getBitmapFromTitle(title: String, paint: Paint): Bitmap? {
    val context = contextRef.get()
    if (!captionViews.containsKey(title) && context != null) {
        val bubbleView = TextView(context)
        bubbleView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        Utils.setBackground(
            bubbleView,
            ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.caption_background,
                null
            )
        )
        bubbleView.gravity = Gravity.CENTER
        bubbleView.maxEms = 20
        bubbleView.textSize = 10f
        bubbleView.setPadding(5, -2, 5, -2)
        bubbleView.setTextColor(Color.BLACK)
        bubbleView.text = title
        //Measure the view at the exact dimensions (otherwise the text won't center correctly)
        val widthSpec =
            MeasureSpec.makeMeasureSpec(paint.getTextWidth(title), MeasureSpec.EXACTLY)
        val heightSpec =
            MeasureSpec.makeMeasureSpec(paint.getTextHeight(title), MeasureSpec.EXACTLY)
        bubbleView.measure(widthSpec, heightSpec)

        //Layout the view at the width and height
        bubbleView.layout(0, 0, paint.getTextWidth(title), paint.getTextHeight(title))
        captionViews[title] =
            Utils.viewToBitmap(
                context,
                bubbleView
            )
        captionViews[title]?.incrementRefCount() // FIXME: is never reduced!
    }
    return captionViews[title]
}

fun clearCaptionBitmap() {
    captionViews.values.forEach(Consumer { bitmap: Bitmap -> bitmap.decrementRefCount() })
    captionViews.clear()
}
