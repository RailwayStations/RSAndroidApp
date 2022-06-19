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

import static java.util.stream.Collectors.joining;

import android.util.Log;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * Layer extended class to display Clustered Marker.
 *
 * @param <T>
 */
public class ClusterMarker<T extends GeoItem> extends Layer {
    private static final String TAG = "ClusterMarker";

    /**
     * cluster object
     */
    protected final Cluster<T> cluster;

    /**
     * icon marker type
     */
    protected int markerType = 0;

    /**
     * @param cluster a cluster to be rendered for this marker
     */
    public ClusterMarker(Cluster<T> cluster) {
        this.cluster = cluster;
    }

    /**
     * change icon bitmaps according to the state and content size.
     */
    private void setMarkerBitmap() {
        for (markerType = 0; markerType < cluster.getClusterManager().markerIconBmps.size(); markerType++) {
            // Check if the number of items in this cluster is below or equal the limit of the MarkerBitMap
            if (cluster.getItems().size() <= cluster.getClusterManager()
                    .markerIconBmps.get(markerType).getItemMax()) {
                return;
            }
        }
        // set the markerType to maximum value ==> reduce markerType by one.
        markerType--;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel,
                                  org.mapsforge.core.graphics.Canvas canvas, Point topLeftPoint) {
        if (cluster.getClusterManager() == null ||
                this.getLatLong() == null) {
            return;
        }
        setMarkerBitmap();
        var mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        var pixelX = MercatorProjection.longitudeToPixelX(this.getLatLong().longitude, mapSize);
        var pixelY = MercatorProjection.latitudeToPixelY(this.getLatLong().latitude, mapSize);
        double halfBitmapWidth;
        double halfBitmapHeight;
        var markerBitmap = cluster.getClusterManager().markerIconBmps.get(markerType);
        var bitmap = markerBitmap.getBitmap(hasPhoto(), ownPhoto(), stationActive(), isPendingUpload());
        try {
            halfBitmapWidth = bitmap.getWidth() / 2f;
            halfBitmapHeight = bitmap.getHeight() / 2f;
        } catch (NullPointerException e) {
            Log.e(ClusterMarker.TAG, e.getMessage(), e);
            return;
        }
        int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + markerBitmap.getIconOffset().x);
        int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + markerBitmap.getIconOffset().y);
        int right = (left + bitmap.getWidth());
        int bottom = (top + bitmap.getHeight());
        var bitmapRectangle = new Rectangle(left, top, right, bottom);
        var canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }
        // Draw bitmap
        canvas.drawBitmap(bitmap, left, top);

        // Draw Text
        if (markerType == 0) {
            // Draw bitmap
            var bubble = MarkerBitmap.getBitmapFromTitle(cluster.getTitle(),
                    markerBitmap.getPaint());
            canvas.drawBitmap(bubble,
                    (int) (left + halfBitmapWidth - bubble.getWidth() / 2),
                    (top - bubble.getHeight()));
        } else {
            int x = (int) (left + bitmap.getWidth() * 1.3);
            int y = (int) (top + bitmap.getHeight() * 1.3
                    + markerBitmap.getPaint().getTextHeight(cluster.getTitle()) / 2);
            canvas.drawText(cluster.getTitle(), x, y,
                    markerBitmap.getPaint());
        }

    }

    /**
     * get center location of the marker.
     *
     * @return GeoPoint object of current marker center.
     */
    public LatLong getLatLong() {
        return cluster.getLocation();
    }

    /**
     * @return Gets the LatLong Position of the Layer Object
     */
    @Override
    public LatLong getPosition() {
        return getLatLong();
    }

    @Override
    public synchronized boolean onTap(LatLong geoPoint, Point viewPosition,
                                      Point tapPoint) {
        if (cluster.getItems().size() == 1 && contains(viewPosition, tapPoint)) {
            Log.w(ClusterMarker.TAG, "The Marker was touched with onTap: "
                    + this.getPosition().toString());
            cluster.getClusterManager().onTap(cluster.getItems().get(0));
            requestRedraw();
            return true;
        } else if (contains(viewPosition, tapPoint)) {
            var builder = new StringBuilder(cluster.getItems().size() + " items:")
                    .append(cluster.getItems().stream()
                    .map(i -> "\n- " + i.getTitle())
                    .limit(6)
                    .collect(joining()));

            if (cluster.getItems().size() > 6) {
                builder.append("\n...");
            }

            if (ClusterManager.toast != null) {
                ClusterManager.toast.setText(builder);
                ClusterManager.toast.show();
            }
        }
        return false;
    }

    public synchronized boolean contains(Point viewPosition, Point tapPoint) {
        return getBitmapRectangle(viewPosition).contains(tapPoint);
    }

    private Rectangle getBitmapRectangle(Point center) {
        var markerBitmap = cluster.getClusterManager().markerIconBmps.get(markerType);
        var bitmap = markerBitmap.getBitmap(hasPhoto(), ownPhoto(), stationActive(), isPendingUpload());
        return new Rectangle(
                center.x - (float) bitmap.getWidth() + markerBitmap.getIconOffset().x,
                center.y - (float) bitmap.getHeight() + markerBitmap.getIconOffset().y,
                center.x + (float) bitmap.getWidth() + markerBitmap.getIconOffset().x,
                center.y + (float) bitmap.getHeight() + markerBitmap.getIconOffset().y);
    }

    public Boolean hasPhoto() {
        return (cluster.getItems().size() == 1 &&
                cluster.getItems().get(0).hasPhoto());
    }

    public Boolean ownPhoto() {
        return (cluster.getItems().size() == 1 &&
                cluster.getItems().get(0).ownPhoto());
    }

    public Boolean stationActive() {
        return (cluster.getItems().size() == 1 &&
                cluster.getItems().get(0).stationActive());
    }

    private boolean isPendingUpload() {
        return (cluster.getItems().size() == 1 &&
                cluster.getItems().get(0).isPendingUpload());
    }

}
