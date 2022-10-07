package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Profile {
    String nickname;
    License license;
    boolean photoOwner;
    boolean anonymous;
    String link;
    String newPassword;
    boolean emailVerified;
    String email;

    transient String password;

}
