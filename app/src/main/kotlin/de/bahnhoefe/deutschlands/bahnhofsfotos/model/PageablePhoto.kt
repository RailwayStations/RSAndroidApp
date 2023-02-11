package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import android.graphics.Bitmap

data class PageablePhoto @JvmOverloads constructor(
    var id: Long = 0,
    var url: String? = null,
    var photographer: String? = null,
    var photographerUrl: String? = null,
    var license: String? = null,
    var licenseUrl: String? = null,
    var bitmap: Bitmap? = null
) {
    constructor(station: Station, bitmap: Bitmap) : this(
        station.photoId,
        station.photoUrl,
        station.photographer,
        station.photographerUrl,
        station.license,
        station.licenseUrl,
        bitmap
    ) {

    }
}