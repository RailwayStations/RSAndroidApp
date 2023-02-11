package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class PhotoStations @JvmOverloads constructor(
    var photoBaseUrl: String? = null,
    var licenses: List<PhotoLicense> = ArrayList(),
    var photographers: List<Photographer> = ArrayList(),
    var stations: List<PhotoStation> = ArrayList()
) {

    fun getPhotographerUrl(photographer: String): String? {
        return photographers.stream()
            .filter { p: Photographer -> p.name == photographer }
            .findAny()
            .map { p: Photographer -> p.url.toString() }
            .orElse(null)
    }

    fun getLicenseName(licenseId: String): String? {
        return licenses.stream()
            .filter { license: PhotoLicense -> license.id == licenseId }
            .findAny()
            .map(PhotoLicense::name)
            .orElse(null)
    }

    fun getLicenseUrl(licenseId: String): String? {
        return licenses.stream()
            .filter { license: PhotoLicense -> license.id == licenseId }
            .findAny()
            .map { l: PhotoLicense -> l.url.toString() }
            .orElse(null)
    }
}