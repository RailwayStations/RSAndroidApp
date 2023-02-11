package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Station implements Serializable {

    @EqualsAndHashCode.Include
    String country;
    @EqualsAndHashCode.Include
    String id;

    String title;
    double lat;
    double lon;
    String ds100;
    String photoUrl;
    String photographer;
    String photographerUrl;
    String license;
    String licenseUrl;
    boolean active;
    boolean outdated;
    long photoId;

    public boolean hasPhoto() {
        return photoUrl != null;
    }

}
