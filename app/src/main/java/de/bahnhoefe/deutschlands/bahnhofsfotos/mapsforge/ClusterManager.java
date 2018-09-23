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

import android.os.AsyncTask;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.MapView;

/**
 * Class for Clustering geotagged content. this clustering came from
 * "markerclusterer" which is available as opensource at
 * https://code.google.com/p/android-maps-extensions/, resp
 * https://github.com/googlemaps/android-maps-utils this is android ported
 * version with modification to fit to the application refer also to other
 * implementations:
 * https://code.google.com/p/osmbonuspack/source/browse/#svn%2Ftrunk
 * %2FOSMBonusPack%2Fsrc%2Forg%2Fosmdroid%2Fbonuspack%2Fclustering
 * http://developer.nokia.com/community/wiki/
 * Map_Marker_Clustering_Strategies_for_the_Maps_API_for_Java_ME
 * <p/>
 * based on http://ge.tt/7Zq63CH/v/1
 * <p/>
 * Should be added as Observer on Mapsforge frameBufferModel.
 */
public class ClusterManager<T extends GeoItem> implements Observer, TapHandler<T> {
    /**
     * A 'Toast' to display information, intended to show information on {@link ClusterMarker}
     * with more than one {@link GeoItem} (while Marker with a single GeoItem should have their
     * own OnClick functions)
     */
    protected static Toast toast;
    /**
     * grid size for Clustering(dip).
     */
    protected final float GRIDSIZE = 28 * DisplayModel.getDeviceScaleFactor();
    /**
     * MapView object.
     */
    protected final MapView mapView;

    /**
     * The maximum (highest) zoom level for clustering the items.,
     */
    protected final byte maxClusteringZoom;
    /**
     * Lock for the re-Clustering of the items.
     */
    public boolean isClustering = false;
    /**
     * MarkerBitmap object for marker icons, uses Static access.
     */
    protected List<MarkerBitmap> markerIconBmps = Collections
            .synchronizedList(new ArrayList<MarkerBitmap>());
    /**
     * The current BoundingBox of the viewport.
     */
    protected BoundingBox currBoundingBox;
    /**
     * GeoItem ArrayList object that are out of viewport and need to be
     * clustered on panning.
     */
    protected List<T> leftItems = Collections
            .synchronizedList(new ArrayList<T>());
    /**
     * Clustered object list.
     */
    protected List<Cluster<T>> clusters = Collections
            .synchronizedList(new ArrayList<Cluster<T>>());
    /**
     * Handles on click on markers
     */
    protected TapHandler<T> tapHandler = null;

    /**
     * saves the actual ZoolLevel of the MapView
     */
    private double oldZoolLevel;
    /**
     * saves the actual Center as LatLong of the MapViewPosition
     */
    private LatLong oldCenterLatLong;
    private AsyncTask<Boolean, Void, Void> clusterTask;

    /**
     * @param mapView           The Mapview in which the markers are shoen
     * @param markerBitmaps     a list of well formed {@link MarkerBitmap}
     * @param maxClusteringZoom The maximum zoom level, beyond this level no clustering is performed.
     */
    public ClusterManager(MapView mapView,
                          List<MarkerBitmap> markerBitmaps, byte maxClusteringZoom, TapHandler<T> tapHandler) {
        this.mapView = mapView;

        // set to impossible values to trigger clustering at first onChange
        oldZoolLevel = -1;
        oldCenterLatLong = new LatLong(-90.0, -180.0);
        synchronized (this.markerIconBmps) {
            this.markerIconBmps = markerBitmaps;
        }

        this.maxClusteringZoom = maxClusteringZoom;
        this.tapHandler = tapHandler;
    }

    /**
     * You might like to set the Toast from external, in order to make sure that only a single Toast
     * is showing up.
     *
     * @param toast A 'Toast' to display information, intended to show information on {@link ClusterMarker}
     */
    public static void setToast(Toast toast) {
        ClusterManager.toast = toast;
    }

    public MapView getMapView() {
        return mapView;
    }

    public synchronized List<T> getAllItems() {
        List<T> rtnList = Collections.synchronizedList(new ArrayList<T>());
        synchronized (leftItems) {
            rtnList.addAll(leftItems);
        }
        synchronized (clusters) {
            for (Cluster<T> mCluster : clusters) {
                rtnList.addAll(mCluster.getItems());
            }
        }
        return rtnList;
    }

    /**
     * add item and do isClustering. NOTE: this method will not redraw screen.
     * after adding all items, you must call redraw() method.
     *
     * @param item GeoItem to be clustered.
     */
    public synchronized void addItem(final T item) {
        if (mapView.getWidth() == 0 || !isItemInViewport(item)) {
            synchronized (leftItems) {
                if (clusterTask != null && clusterTask.isCancelled()) return;
                leftItems.add(item);
            }
        } else if (maxClusteringZoom >= mapView.getModel().mapViewPosition
                .getZoomLevel()) {
            // else add to a cluster;
            Point pos = mapView.getMapViewProjection().toPixels(item.getLatLong());
            // check existing cluster
            synchronized (clusters) {
                for (Cluster<T> mCluster : clusters) {
                    if (clusterTask != null && clusterTask.isCancelled()) return;
                    if (mCluster.getItems().size() == 0)
                        throw new IllegalArgumentException("cluster.getItems().size() == 0");
                    // find a cluster which contains the marker.
                    // use 1st element to fix the location, hinder the cluster from
                    // running around and isClustering.
                    LatLong gpCenter = mCluster.getItems().get(0)
                            .getLatLong();
                    if (gpCenter == null)
                        throw new IllegalArgumentException();
                    Point ptCenter = mapView.getMapViewProjection().toPixels(gpCenter);
                    // find a cluster which contains the marker.
                    if (pos.distance(ptCenter) <= GRIDSIZE
                    /*
                     * pos.x >= ptCenter.x - GRIDSIZE && pos.x <= ptCenter.x +
                     * GRIDSIZE && pos.y >= ptCenter.y - GRIDSIZE && pos.y <=
                     * ptCenter.y + GRIDSIZE
                     */) {
                        mCluster.addItem(item);
                        return;
                    }
                }
                // No cluster contain the marker, create a new cluster.
                clusters.add(createCluster(item));
            }
        } else {
            // No clustering allowed, create a new cluster with single item.
            synchronized (clusters) {
                clusters.add(createCluster(item));
            }
        }
    }

    /**
     * Create Cluster Object. override this method, if you want to use custom
     * GeoCluster class.
     *
     * @param item GeoItem to be set to cluster.
     */
    public Cluster<T> createCluster(T item) {
        return new Cluster<T>(this, item);
    }

    /**
     * redraws clusters
     */
    public synchronized void redraw() {
        synchronized (clusters) {
            if (!isClustering)
                for (Cluster<T> mCluster : clusters) {
                    mCluster.redraw();
                }
        }
    }

    /**
     * check if the item is within current viewport.
     *
     * @return true if item is within viewport.
     */
    protected boolean isItemInViewport(final GeoItem item) {
        final BoundingBox curBounds = getCurBounds();
        return curBounds != null && curBounds.contains(item.getLatLong());
    }

    /**
     * get the current BoundingBox of the viewport
     *
     * @return current BoundingBox of the viewport
     */
    protected synchronized BoundingBox getCurBounds() {
        if (currBoundingBox == null) {
            if (mapView == null) {
                throw new NullPointerException("mapView == null");
            }
            if (mapView.getWidth() <= 0 || mapView.getHeight() <= 0) {
                throw new IllegalArgumentException(" mapView.getWidth() <= 0 " +
                        "|| mapView.getHeight() <= 0 "
                        + mapView.getWidth() + " || " + mapView.getHeight());
            }
            /** North-West geo point of the bound */
            LatLong nw_ = mapView.getMapViewProjection().fromPixels(-mapView.getWidth() * 0.5, -mapView.getHeight() * 0.5);
            /** South-East geo point of the bound */
            LatLong se_ = mapView.getMapViewProjection().fromPixels(mapView.getWidth() + mapView.getWidth() * 0.5,
                    mapView.getHeight() + mapView.getHeight() * 0.5);
            if (se_ != null && nw_ != null) {
                if (se_.latitude > nw_.latitude) {
                    currBoundingBox = new BoundingBox(nw_.latitude, se_.longitude, se_.latitude,
                            nw_.longitude);
                } else {
                    currBoundingBox = new BoundingBox(se_.latitude, nw_.longitude, nw_.latitude,
                            se_.longitude);
                }
            }
        }
        return currBoundingBox;
    }

    /**
     * add items that were not clustered in last isClustering.
     */
    private void addLeftItems() {
        if (leftItems.size() == 0) {
            return;
        }
        ArrayList<T> currentLeftItems = new ArrayList<T>();
        currentLeftItems.addAll(leftItems);

        synchronized (leftItems) {
            leftItems.clear();
        }
        for (T currentLeftItem : currentLeftItems) {
            addItem(currentLeftItem);
        }
    }

    // *********************************************************************************************************************
    // Methods to implement 'Observer'
    // *********************************************************************************************************************

    @Override
    public synchronized void onChange() {
        currBoundingBox = null;
        if (isClustering) {
            return;
        }

        if (oldZoolLevel != mapView.getModel().mapViewPosition.getZoomLevel()) {
            // react on zoom changes
            oldZoolLevel = mapView.getModel().mapViewPosition.getZoomLevel();
            resetViewport(false);
        } else {
            // react on position changes
            MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

            Point posOld = mapView.getMapViewProjection().toPixels(oldCenterLatLong);
            Point posNew = mapView.getMapViewProjection().toPixels(mapViewPosition.getCenter());
            if (posOld != null && posOld.distance(posNew) > GRIDSIZE / 2) {
                // Log.d(TAG, "moving...");
                oldCenterLatLong = mapViewPosition.getCenter();
                resetViewport(true);
            }
        }
    }

    /**
     * reset current viewport, re-cluster the items when zoom has changed, else
     * add not clustered items .
     */
    private synchronized void resetViewport(boolean isMoving) {
        isClustering = true;
        clusterTask = new ClusterTask();
        clusterTask.execute(new Boolean[]{isMoving});
    }

    public void cancelClusterTask() {
        if (clusterTask != null) {
            clusterTask.cancel(true);
        }
    }

    public synchronized void destroyGeoClusterer() {
        synchronized (clusters) {
            for (Cluster<T> cluster : clusters) {
                cluster.getClusterManager().cancelClusterTask();
                cluster.clear();
            }
            clusters.clear();
        }
        for (MarkerBitmap markerBitmap : markerIconBmps) {
            if (markerBitmap.getBitmap(true, false) != null) {
                markerBitmap.getBitmap(true, false).decrementRefCount();
            }
            if (markerBitmap.getBitmap(true, true) != null) {
                markerBitmap.getBitmap(true, true).decrementRefCount();
            }
            if (markerBitmap.getBitmap(false, false) != null) {
                markerBitmap.getBitmap(false, false).decrementRefCount();
            }
        }
        synchronized (leftItems) {
            leftItems.clear();
        }
        MarkerBitmap.clearCaptionBitmap();
    }

    @Override
    public void onTap(T item) {
        if (tapHandler != null) {
            tapHandler.onTap(item);
        }
    }

    private class ClusterTask extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected Void doInBackground(Boolean... params) {
            // If the map is moved without zoom-change: Add unclustered items.
            if (params[0]) {
                addLeftItems();
            }
            // If the cluster zoom level changed then destroy the cluster and
            // collect its markers.
            else {
                synchronized (clusters) {
                    for (Cluster<T> mCluster : clusters) {
                        synchronized (leftItems) {
                            leftItems.addAll(mCluster.getItems());
                        }
                        mCluster.clear();
                    }
                }
                synchronized (clusters) {
                    clusters.clear();
                }
                if (!isCancelled()) {
                    synchronized (clusters) {
                        if (clusters.size() != 0) {
                            throw new IllegalArgumentException();
                        }
                    }
                    addLeftItems();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            isClustering = false;
            redraw();
        }
    }
}
