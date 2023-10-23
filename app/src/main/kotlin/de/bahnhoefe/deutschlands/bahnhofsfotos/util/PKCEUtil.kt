package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PKCEUtil {

    private String verifier = "";

    public String getCodeVerifier() {
        return verifier;
    }

    public String getCodeChallenge() throws NoSuchAlgorithmException {
        verifier = generateCodeVerifier();
        return generateCodeChallenge(verifier);
    }

    private String generateCodeVerifier() {
        var secureRandom = new SecureRandom();
        var codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        var bytes = codeVerifier.getBytes(Charset.defaultCharset());
        var messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        var digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

}
