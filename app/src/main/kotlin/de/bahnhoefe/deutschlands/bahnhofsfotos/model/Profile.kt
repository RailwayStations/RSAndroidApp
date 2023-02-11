package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class Profile @JvmOverloads constructor(
    var nickname: String? = null,
    var license: License? = null,
    var photoOwner: Boolean = false,
    var anonymous: Boolean = false,
    var link: String? = null,
    var email: String? = null,
    var emailVerified: Boolean = false
) {

    fun isAllowedToUploadPhoto(): Boolean {
        return emailVerified && license === License.CC0 && photoOwner
    }

}