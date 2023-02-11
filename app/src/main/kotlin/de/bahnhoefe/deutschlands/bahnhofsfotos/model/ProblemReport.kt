package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class ProblemReport @JvmOverloads constructor(
    var countryCode: String? = null,
    var stationId: String? = null,
    var comment: String? = null,
    var type: ProblemType? = null,
    var photoId: Long? = null,
    var lat: Double? = null,
    var lon: Double? = null
)