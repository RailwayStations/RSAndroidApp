package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

fun interface TapHandler<T : GeoItem?> {
    fun onTap(item: T)
}