package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PhotoStation {

    String country;

    String id;

    String title;

    double lat;

    double lon;

    String shortCode;

    boolean inactive;

    List<Photo> photos;

}
