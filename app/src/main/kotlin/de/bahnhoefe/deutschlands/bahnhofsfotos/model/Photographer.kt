package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.net.URL

data class Photographer @JvmOverloads constructor(
    var name: String? = null,
    var url: URL? = null
)