package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge;

public interface TapHandler<T extends GeoItem> {

    void onTap(T item);

}
