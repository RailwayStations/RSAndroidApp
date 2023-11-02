package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class ProblemReport @JvmOverloads constructor(
    var countryCode: String,
    var stationId: String,
    var comment: String? = null,
    var type: ProblemType,
    var photoId: Long? = null,
    var lat: Double? = null,
    var lon: Double? = null,
    var title: String? = null,
)