package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import com.google.gson.annotations.SerializedName

data class Token @JvmOverloads constructor(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Long = 0,

    @SerializedName("scope")
    val scope: String? = null

)