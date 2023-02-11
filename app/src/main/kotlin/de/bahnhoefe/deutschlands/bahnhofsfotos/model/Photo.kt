package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class Photo @JvmOverloads constructor(
    val id: Long,
    val photographer: String,
    val path: String,
    val createdAt: Long,
    val license: String,
    val outdated: Boolean = false
)
