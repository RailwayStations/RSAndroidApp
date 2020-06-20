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

import android.util.Log;
import android.widget.Toast;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final String TAG = ClusterManager.class.getSimpleName();
    protected static final int MIN_CLUSTER_SIZE = 5;
    /**
     * A 'Toast' to display information, intended to show information on {@link ClusterMarker}
     * with more than one {@link GeoItem} (while Marker with a single GeoItem should have their
     * own OnClick functions)
     */
    protected static Toast toast;
    /**
     * grid size for Clustering(dip).
     */
    protected final float GRIDSIZE = 60 * DisplayModel.getDeviceScaleFactor();
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
    protected final List<MarkerBitmap> markerIconBmps;

    /**
     * The current BoundingBox of the viewport.
     */
    protected BoundingBox currBoundingBox;
    /**
     * GeoItem ArrayList object that are out of viewport and need to be
     * clustered on panning.
     */
    protected final List<T> leftItems = Collections.synchronizedList(new ArrayList<>());
    /**
     * Clustered object list.
     */
    protected final List<Cluster<T>> clusters = Collections.synchronizedList(new ArrayList<>());
    /**
     * Handles on click on markers
     */
    protected TapHandler<T> tapHandler;

    /**
     * saves the actual ZoolLevel of the MapView
     */
    private double oldZoolLevel;
    /**
     * saves the actual Center as LatLong of the MapViewPosition
     */
    private LatLong oldCenterLatLong;
    private ClusterTask clusterTask;

    /**
     * @param mapView           The Mapview in which the markers are shoen
     * @param markerBitmaps     a list of well formed {@link MarkerBitmap}
     * @param maxClusteringZoom The maximum zoom level, beyond this level no clustering is performed.
     */
    public ClusterManager(final MapView mapView,
                          final List<MarkerBitmap> markerBitmaps, final byte maxClusteringZoom, final TapHandler<T> tapHandler) {
        this.mapView = mapView;

        // set to impossible values to trigger clustering at first onChange
        oldZoolLevel = -1;
        oldCenterLatLong = new LatLong(-90.0, -180.0);
        this.markerIconBmps = markerBitmaps;

        this.maxClusteringZoom = maxClusteringZoom;
        this.tapHandler = tapHandler;
    }

    /**
     * You might like to set the Toast from external, in order to make sure that only a single Toast
     * is showing up.
     *
     * @param toast A 'Toast' to display information, intended to show information on {@link ClusterMarker}
     */
    public static void setToast(final Toast toast) {
        ClusterManager.toast = toast;
    }

    public MapView getMapView() {
        return mapView;
    }

    public synchronized List<T> getAllItems() {
        final List<T> rtnList = Collections.synchronizedList(new ArrayList<>());
        synchronized (leftItems) {
            rtnList.addAll(leftItems);
        }
        synchronized (clusters) {
            for (final Cluster<T> mCluster : clusters) {
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
                if (clusterTask != null && clusterTask.isInterrupted()) {
                    return;
                }
                leftItems.add(item);
            }
        } else if (maxClusteringZoom >= mapView.getModel().mapViewPosition
                .getZoomLevel()) {
            // else add to a cluster;
            final Point pos = mapView.getMapViewProjection().toPixels(item.getLatLong());
            // check existing cluster
            synchronized (clusters) {
                for (final Cluster<T> mCluster : clusters) {
                    if (clusterTask != null && clusterTask.isInterrupted()) {
                        return;
                    }
                    if (mCluster.getItems().size() == 0) {
                        throw new IllegalArgumentException("cluster.getItems().size() == 0");
                    }

                    // find a cluster which contains the marker.
                    // use 1st element to fix the location, hinder the cluster from
                    // running around and isClustering.
                    final LatLong gpCenter = mCluster.getItems().get(0).getLatLong();
                    if (gpCenter == null) {
                        throw new IllegalArgumentException();
                    }

                    final Point ptCenter = mapView.getMapViewProjection().toPixels(gpCenter);
                    // find a cluster which contains the marker.
                    if (pos.distance(ptCenter) <= GRIDSIZE) {
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
    public Cluster<T> createCluster(final T item) {
        return new Cluster<>(this, item);
    }

    /**
     * redraws clusters
     */
    public synchronized void redraw() {
        synchronized (clusters) {
            if (!isClustering) {
                final List<Cluster<T>> removed = new ArrayList<>();
                final List<Cluster<T>> singles = new ArrayList<>();
                for (final Cluster<T> mCluster : clusters) {
                    if (mCluster.getSize() < MIN_CLUSTER_SIZE) {
                        for (final T item : mCluster.getItems()) {
                            singles.add(createCluster(item));
                        }
                        mCluster.clear();
                        removed.add(mCluster);
                    }
                }
                clusters.removeAll(removed);
                clusters.addAll(singles);

                for (final Cluster<T> mCluster : clusters) {
                    mCluster.redraw();
                }
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
            /* North-West geo point of the bound */
            final LatLong nw_ = mapView.getMapViewProjection().fromPixels(-mapView.getWidth() * 0.5, -mapView.getHeight() * 0.5);
            /* South-East geo point of the bound */
            final LatLong se_ = mapView.getMapViewProjection().fromPixels(mapView.getWidth() + mapView.getWidth() * 0.5,
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
        final ArrayList<T> currentLeftItems = new ArrayList<>(leftItems);

        synchronized (leftItems) {
            leftItems.clear();
        }
        for (final T currentLeftItem : currentLeftItems) {
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
            final IMapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

            final Point posOld = mapView.getMapViewProjection().toPixels(oldCenterLatLong);
            final Point posNew = mapView.getMapViewProjection().toPixels(mapViewPosition.getCenter());
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
    private synchronized void resetViewport(final boolean isMoving) {
        isClustering = true;
        clusterTask = new ClusterTask(isMoving);
        clusterTask.start();
    }

    public void cancelClusterTask() {
        if (clusterTask != null) {
            clusterTask.interrupt();
        }
    }

    public synchronized void destroyGeoClusterer() {
        synchronized (clusters) {
            for (final Cluster<T> cluster : clusters) {
                cluster.getClusterManager().cancelClusterTask();
                cluster.clear();
            }
            clusters.clear();
        }
        for (final MarkerBitmap markerBitmap : markerIconBmps) {
            markerBitmap.decrementRefCounters();
        }
        synchronized (leftItems) {
            leftItems.clear();
        }
        MarkerBitmap.clearCaptionBitmap();
    }

    @Override
    public void onTap(final T item) {
        if (tapHandler != null) {
            tapHandler.onTap(item);
        }
    }

    private class ClusterTask extends Thread {

        private final boolean isMoving;

        public ClusterTask(final boolean isMoving) {
            this.isMoving = isMoving;
        }

        @Override
        public void run() {
            Log.d(TAG, "Run ClusterTask");
            // If the map is moved without zoom-change: Add unclustered items.
            if (isMoving) {
                addLeftItems();
            }
            // If the cluster zoom level changed then destroy the cluster and
            // collect its markers.
            else {
                synchronized (clusters) {
                    for (final Cluster<T> mCluster : clusters) {
                        synchronized (leftItems) {
                            leftItems.addAll(mCluster.getItems());
                        }
                        mCluster.clear();
                    }
                }
                synchronized (clusters) {
                    clusters.clear();
                }
                if (!isInterrupted()) {
                    synchronized (clusters) {
                        if (clusters.size() != 0) {
                            throw new IllegalArgumentException();
                        }
                    }
                    addLeftItems();
                }
            }
            isClustering = false;
            redraw();
            Log.d(TAG, "ClusterTask finished");
        }
    }

}
