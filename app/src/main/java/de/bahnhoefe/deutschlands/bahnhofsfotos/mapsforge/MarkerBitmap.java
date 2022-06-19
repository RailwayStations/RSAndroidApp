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

package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

/**
 * Utility Class to handle MarkerBitmap
 * it handles grid offset to display on the map with offset
 */
public class MarkerBitmap {

    private static final Map<String, Bitmap> captionViews = new HashMap<>();
    private static WeakReference<Context> contextRef;

    /**
     * bitmap object for stations without photo
     */
    private final Bitmap iconBmpWithoutPhoto;

    /**
     * bitmap object for stations with photo
     */
    private final Bitmap iconBmpWithPhoto;

    /**
     * bitmap object for stations with photo from user
     */
    private final Bitmap iconBmpOwnPhoto;

    /**
     * bitmap object for inactive stations without photo
     */
    private final Bitmap iconBmpWithoutPhotoInactive;

    /**
     * bitmap object for inactive stations with photo
     */
    private final Bitmap iconBmpWithPhotoInactive;

    /**
     * bitmap object for inactive stations with photo from user
     */
    private final Bitmap iconBmpOwnPhotoInactive;

    /**
     * bitmap object for stations with photo from user with pending upload
     */
    private final Bitmap iconBmpPendingUpload;

    /**
     * Paint object for drawing icon
     */
    private final Paint paint;

    /**
     * offset grid of icon in Point.
     * if you are using symmetric icon image, it should be half size of width&height.
     * adjust this parameter to offset the axis of the image.
     */
    private final Point iconOffset;

    /**
     * maximum item size for the marker.
     */
    private final int itemSizeMax;

    /**
     * text size for icon
     */
    private final float textSize;

    /**
     * NOTE: all src* must be same bitmap size.
     *
     * @param srcWithoutPhoto  source Bitmap object for stations without photo
     * @param srcWithPhoto  source Bitmap object for stations with photo
     * @param srcOwnPhoto source Bitmap object for stations with photo from user
     * @param srcWithoutPhotoInactive  source Bitmap object for inactive stations without photo
     * @param srcWithPhotoInactive  source Bitmap object for inactive stations with photo
     * @param srcOwnPhotoInactive source Bitmap object for inactive stations with photo from user
     * @param srcPendingUpload source Bitmap object for stations with photo from user with pending upload
     * @param grid     grid point to be offset
     * @param textSize text size for icon
     * @param maxSize  icon size threshold
     */
    public MarkerBitmap(Context context, Bitmap srcWithoutPhoto, Bitmap srcWithPhoto, Bitmap srcOwnPhoto,
                        Bitmap srcWithoutPhotoInactive, Bitmap srcWithPhotoInactive, Bitmap srcOwnPhotoInactive,
                        Bitmap srcPendingUpload,
                        Point grid, float textSize, int maxSize, Paint paint) {
        MarkerBitmap.contextRef = new WeakReference<>(context);
        iconBmpWithoutPhoto = srcWithoutPhoto;
        iconBmpWithPhoto = srcWithPhoto;
        iconBmpOwnPhoto = srcOwnPhoto;
        iconBmpWithPhotoInactive = srcWithPhotoInactive;
        iconBmpWithoutPhotoInactive = srcWithoutPhotoInactive;
        iconBmpOwnPhotoInactive = srcOwnPhotoInactive;
        iconBmpPendingUpload = srcPendingUpload;
        iconOffset = grid;
        this.textSize = textSize * DisplayModel.getDeviceScaleFactor();
        itemSizeMax = maxSize;
        this.paint = paint;
        this.paint.setTextSize(getTextSize());
    }

    public MarkerBitmap(Context context, Bitmap bitmap, Point grid, float textSize, int maxSize, Paint paint) {
        this(context, bitmap, bitmap, bitmap, bitmap, bitmap, bitmap, bitmap, grid, textSize, maxSize, paint);
    }

    public static Bitmap getBitmapFromTitle(String title, Paint paint) {
        var context = contextRef.get();
        if (!captionViews.containsKey(title) && context != null) {
            var bubbleView = new TextView(context);
            bubbleView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            Utils.setBackground(bubbleView, ResourcesCompat.getDrawable(context.getResources(), R.drawable.caption_background, null));
            bubbleView.setGravity(Gravity.CENTER);
            bubbleView.setMaxEms(20);
            bubbleView.setTextSize(10);
            bubbleView.setPadding(5, -2, 5, -2);
            bubbleView.setTextColor(android.graphics.Color.BLACK);
            bubbleView.setText(title);
            //Measure the view at the exact dimensions (otherwise the text won't center correctly)
            int widthSpec = View.MeasureSpec.makeMeasureSpec(paint.getTextWidth(title), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(paint.getTextHeight(title), View.MeasureSpec.EXACTLY);
            bubbleView.measure(widthSpec, heightSpec);

            //Layout the view at the width and height
            bubbleView.layout(0, 0, paint.getTextWidth(title), paint.getTextHeight(title));

            captionViews.put(title, Utils.viewToBitmap(context, bubbleView));
            Objects.requireNonNull(captionViews.get(title)).incrementRefCount(); // FIXME: is never reduced!
        }
        return captionViews.get(title);
    }

    protected static void clearCaptionBitmap() {
        captionViews.values().forEach(Bitmap::decrementRefCount);
        captionViews.clear();
    }

    /**
     * @return bitmap object according to the state of the stations
     */
    public Bitmap getBitmap(boolean hasPhoto, boolean ownPhoto, boolean stationActive, boolean inbox) {
        if (inbox) {
            return iconBmpPendingUpload;
        }

        if (ownPhoto) {
            if (stationActive) {
                return iconBmpOwnPhoto;
            }
            return iconBmpOwnPhotoInactive;
        } else if (hasPhoto) {
            if (stationActive) {
                return iconBmpWithPhoto;
            }
            return iconBmpWithPhotoInactive;
        }

        if (stationActive) {
            return iconBmpWithoutPhoto;
        }

        return iconBmpWithoutPhotoInactive;
    }

    /**
     * @return get offset of the icon
     */
    public Point getIconOffset() {
        return iconOffset;
    }

    /**
     * @return text size already adjusted with DisplayModel.getDeviceScaleFactor(), i.e.
     * the scaling factor for fonts displayed on the display.
     */
    public float getTextSize() {
        return textSize;
    }

    /**
     * @return icon size threshold
     */
    public int getItemMax() {
        return itemSizeMax;
    }

    /**
     * @return Paint object for drawing icon
     */
    public Paint getPaint() {
        return paint;
    }

    public void decrementRefCounters() {
        if (iconBmpOwnPhoto != null) {
            iconBmpOwnPhoto.decrementRefCount();
        }
        if (iconBmpWithPhoto != null) {
            iconBmpWithPhoto.decrementRefCount();
        }
        if (iconBmpWithoutPhoto != null) {
            iconBmpWithoutPhoto.decrementRefCount();
        }
        if (iconBmpOwnPhotoInactive != null) {
            iconBmpOwnPhotoInactive.decrementRefCount();
        }
        if (iconBmpWithPhotoInactive != null) {
            iconBmpWithPhotoInactive.decrementRefCount();
        }
        if (iconBmpWithoutPhotoInactive != null) {
            iconBmpWithoutPhotoInactive.decrementRefCount();
        }
    }
}


