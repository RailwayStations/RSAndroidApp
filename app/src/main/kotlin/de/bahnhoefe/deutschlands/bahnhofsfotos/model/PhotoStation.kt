package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class PhotoStation @JvmOverloads constructor(
    var country: String? = null,
    var id: String? = null,
    var title: String? = null,
    var lat: Double = 0.0,
    var lon: Double = 0.0,
    var shortCode: String? = null,
    var inactive: Boolean = false,
    var photos: List<Photo>? = null
)