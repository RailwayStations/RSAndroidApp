package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicInbox {

    String title;
    String countryCode;
    String stationId;
    Double lat;
    Double lon;

}
