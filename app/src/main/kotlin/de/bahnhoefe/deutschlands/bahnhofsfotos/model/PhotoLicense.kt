package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.net.URL

data class PhotoLicense @JvmOverloads constructor(
    var id: String? = null,
    var name: String? = null,
    var url: URL? = null
)