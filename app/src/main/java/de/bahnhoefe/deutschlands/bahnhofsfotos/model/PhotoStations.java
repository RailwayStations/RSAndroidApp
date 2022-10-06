package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class PhotoStations {

    @NonNull
    String photoBaseUrl;

    @NonNull
    List<PhotoLicense> licenses;

    @NonNull
    List<Photographer> photographers;

    @NonNull
    List<PhotoStation> stations;

}
