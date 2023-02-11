package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.io.Serializable

data class Upload @JvmOverloads constructor(
    var id: Long? = null,
    var country: String? = null,
    var stationId: String? = null,
    var remoteId: Long? = null,
    var title: String? = null,
    var lat: Double? = null,
    var lon: Double? = null,
    var comment: String? = null,
    var inboxUrl: String? = null,
    var problemType: ProblemType? = null,
    var rejectReason: String? = null,
    var uploadState: UploadState = UploadState.NOT_YET_SENT,
    var createdAt: Long = System.currentTimeMillis(),
    var active: Boolean? = null,
    var crc32: Long? = null
) : Serializable {

    val isUploadForExistingStation: Boolean
        get() = country != null && stationId != null
    val isUploadForMissingStation: Boolean
        get() = lat != null && lon != null
    val isProblemReport: Boolean
        get() = problemType != null
    val isPendingPhotoUpload: Boolean
        get() = (isUploadForExistingStation || isUploadForMissingStation) && isPending && !isProblemReport
    val isPending: Boolean
        get() = uploadState.isPending
    val isUploaded: Boolean
        get() = remoteId != null

}