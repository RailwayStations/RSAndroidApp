package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProblemReport {

    String countryCode;
    String stationId;
    String comment;
    ProblemType type;
    Long photoId;
    Double lat;
    Double lon;

}
