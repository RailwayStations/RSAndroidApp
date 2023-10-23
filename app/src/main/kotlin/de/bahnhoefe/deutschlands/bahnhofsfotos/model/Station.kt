package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.io.Serializable

data class Station @JvmOverloads constructor(
    var country: String? = null,
    var id: String,
    var title: String,
    var lat: Double = 0.0,
    var lon: Double = 0.0,
    var ds100: String? = null,
    var photoUrl: String? = null,
    var photographer: String? = null,
    var photographerUrl: String? = null,
    var license: String? = null,
    var licenseUrl: String? = null,
    var active: Boolean = false,
    var outdated: Boolean = false,
    var photoId: Long = 0
) : Serializable {

    fun hasPhoto(): Boolean {
        return photoUrl != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Station

        if (country != other.country) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = country?.hashCode() ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }

}