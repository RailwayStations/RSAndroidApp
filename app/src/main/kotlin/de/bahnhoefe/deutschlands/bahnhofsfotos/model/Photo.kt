package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Photo {

    long id;

    String photographer;

    String path;

    long createdAt;

    String license;

    boolean outdated;

}
