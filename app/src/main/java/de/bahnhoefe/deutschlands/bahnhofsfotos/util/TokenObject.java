package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

/**
 * Created by android_oma on 19.03.17.
 */

import com.google.gson.annotations.SerializedName;

public class TokenObject {
    @SerializedName("token")
    private String token;

    public TokenObject(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}