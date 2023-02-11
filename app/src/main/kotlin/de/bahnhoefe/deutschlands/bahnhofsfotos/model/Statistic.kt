package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class Statistic @JvmOverloads constructor(
    var total: Int = 0,
    var withPhoto: Int = 0,
    var withoutPhoto: Int = 0,
    var photographers: Int = 0
)