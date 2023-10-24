package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class PhotoStation @JvmOverloads constructor(
    val country: String,
    val id: String,
    val title: String,
    val lat: Double,
    val lon: Double,
    var shortCode: String? = null,
    var inactive: Boolean = false,
    val photos: List<Photo> = listOf()
)