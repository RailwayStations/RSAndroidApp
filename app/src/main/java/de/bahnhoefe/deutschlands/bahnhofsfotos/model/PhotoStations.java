package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PhotoStations {

    String photoBaseUrl;

    List<PhotoLicense> licenses;

    List<Photographer> photographers;

    List<PhotoStation> stations;

}
