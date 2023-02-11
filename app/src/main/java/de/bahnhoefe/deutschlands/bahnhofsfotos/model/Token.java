package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Token {

    @SerializedName("access_token")
    String accessToken;
    @SerializedName("refresh_token")
    String refreshToken;
    @SerializedName("expires_in")
    long expiresIn;
    @SerializedName("scope")
    String scope;
    @SerializedName("token_type")
    String tokenType;

}
