package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class PublicInbox @JvmOverloads constructor(
    var title: String? = null,
    var countryCode: String? = null,
    var stationId: String? = null,
    var lat: Double? = null,
    var lon: Double? = null
)