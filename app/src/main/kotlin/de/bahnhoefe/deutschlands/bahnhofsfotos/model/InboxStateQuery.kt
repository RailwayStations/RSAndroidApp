package de.bahnhoefe.deutschlands.bahnhofsfotos.model

class InboxStateQuery @JvmOverloads constructor(
    var id: Long? = null,
    var countryCode: String? = null,
    var stationId: String? = null,
    var lat: Double? = null,
    var lon: Double? = null,
    var state: UploadState = UploadState.UNKNOWN,
    var rejectedReason: String? = null,
    var crc32: Long? = null
)
