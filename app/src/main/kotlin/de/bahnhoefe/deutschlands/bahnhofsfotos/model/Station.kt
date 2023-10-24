package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.io.Serializable

data class Station @JvmOverloads constructor(
    val country: String,
    val id: String,
    val title: String,
    val lat: Double,
    val lon: Double,
    val ds100: String? = null,
    val photoUrl: String? = null,
    val photographer: String? = null,
    val photographerUrl: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val active: Boolean = false,
    val outdated: Boolean = false,
    val photoId: Long?,
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
        var result = country.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

}