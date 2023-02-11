package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class HighScoreItem @JvmOverloads constructor(
    var name: String? = null,
    var photos: Int = 0,
    var position: Int = 0
)