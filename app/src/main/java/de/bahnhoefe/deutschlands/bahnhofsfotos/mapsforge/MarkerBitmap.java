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
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;

/**
 * Utility Class to handle MarkerBitmap
 * it handles grid offset to display on the map with offset
 */
public class MarkerBitmap {

    private static Map<String, Bitmap> captionViews = new HashMap<String, Bitmap>();
    private static Context context;

    /**
     * bitmap object for stations without photo
     */
    protected final Bitmap iconBmpWithoutPhoto;

    /**
     * bitmap object for stations with photo
     */
    protected final Bitmap iconBmpWithPhoto;

    /**
     * bitmap object for stations with photo from user
     */
    protected final Bitmap iconBmpOwnPhoto;

    /**
     * Paint object for drawing icon
     */
    protected final Paint paint;

    /**
     * offset grid of icon in Point.
     * if you are using symmetric icon image, it should be half size of width&height.
     * adjust this parameter to offset the axis of the image.
     */
    protected Point iconOffset;

    /**
     * maximum item size for the marker.
     */
    protected int itemSizeMax;

    /**
     * text size for icon
     */
    protected float textSize;

    /**
     * NOTE: all src* must be same bitmap size.
     *
     * @param srcWithoutPhoto  source Bitmap object for stations without photo
     * @param srcWithPhoto  source Bitmap object for stations with photo
     * @param srcOwnPhoto source Bitmap object for stations with photo from user
     * @param grid     grid point to be offset
     * @param textSize text size for icon
     * @param maxSize  icon size threshold
     */
    public MarkerBitmap(Context context, Bitmap srcWithoutPhoto, Bitmap srcWithPhoto, Bitmap srcOwnPhoto,
                        Point grid, float textSize, int maxSize, Paint paint) {
        MarkerBitmap.context = context;
        iconBmpWithoutPhoto = srcWithoutPhoto;
        iconBmpWithPhoto = srcWithPhoto;
        iconBmpOwnPhoto = srcOwnPhoto;
        iconOffset = grid;
        this.textSize = textSize * DisplayModel.getDeviceScaleFactor();
        itemSizeMax = maxSize;
        this.paint = paint;
        this.paint.setTextSize(getTextSize());
    }

    public static Bitmap getBitmapFromTitle(String title, Paint paint) {
        if (!captionViews.containsKey(title)) {
            TextView bubbleView = new TextView(context);
            Utils.setBackground(bubbleView, context.getResources().getDrawable(R.drawable.caption_background));
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
            captionViews.get(title).incrementRefCount(); // FIXME: is never reduced!
        }
        return captionViews.get(title);
    }

    protected static void clearCaptionBitmap() {
        for (Bitmap bitmap : captionViews.values()) {
            bitmap.decrementRefCount();
        }
        captionViews.clear();
    }

    /**
     * @return bitmap object according to the state of the stations
     */
    public final Bitmap getBitmap(boolean hasPhoto, boolean ownPhoto) {
        if (ownPhoto) {
            return iconBmpOwnPhoto;
        } else if (hasPhoto) {
            return iconBmpWithPhoto;
        }
        return iconBmpWithoutPhoto;
    }

    /**
     * @return get offset of the icon
     */
    public final Point getIconOffset() {
        return iconOffset;
    }

    /**
     * @return text size already adjusted with DisplayModel.getDeviceScaleFactor(), i.e.
     * the scaling factor for fonts displayed on the display.
     */
    public final float getTextSize() {
        return textSize;
    }

    /**
     * @return icon size threshold
     */
    public final int getItemMax() {
        return itemSizeMax;
    }

    /**
     * @return Paint object for drawing icon
     */
    public Paint getPaint() {
        return paint;
    }
}


