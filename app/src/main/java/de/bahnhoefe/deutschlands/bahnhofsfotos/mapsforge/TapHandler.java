package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge;

public interface TapHandler<T extends GeoItem> {

    void onTap(final T item);

}
