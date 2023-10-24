package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class PhotoStations @JvmOverloads constructor(
    var photoBaseUrl: String? = null,
    var licenses: List<PhotoLicense> = ArrayList(),
    var photographers: List<Photographer> = ArrayList(),
    var stations: List<PhotoStation> = ArrayList()
) {

    fun getPhotographerUrl(photographer: String): String? {
        return photographers
            .filter { p: Photographer -> p.name == photographer }
            .map { p: Photographer -> p.url.toString() }
            .firstOrNull()
    }

    fun getLicenseName(licenseId: String): String? {
        return licenses
            .filter { license: PhotoLicense -> license.id == licenseId }
            .map(PhotoLicense::name)
            .firstOrNull()
    }

    fun getLicenseUrl(licenseId: String): String? {
        return licenses
            .filter { license: PhotoLicense -> license.id == licenseId }
            .map { l: PhotoLicense -> l.url.toString() }
            .firstOrNull()
    }
}