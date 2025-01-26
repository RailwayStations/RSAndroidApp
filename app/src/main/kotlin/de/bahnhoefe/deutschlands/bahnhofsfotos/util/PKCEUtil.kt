package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64

class PKCEUtil {
    var codeVerifier = ""
        private set

    @get:Throws(NoSuchAlgorithmException::class)
    val codeChallenge: String
        get() {
            codeVerifier = generateCodeVerifier()
            return generateCodeChallenge(codeVerifier)
        }

    private fun generateCodeVerifier(): String {
        val codeVerifier = ByteArray(32)
        SecureRandom().run {
            nextBytes(codeVerifier)
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").apply<MessageDigest> {
            val bytes = codeVerifier.toByteArray(Charset.defaultCharset())
            update(bytes, 0, bytes.size)
        }.digest()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}