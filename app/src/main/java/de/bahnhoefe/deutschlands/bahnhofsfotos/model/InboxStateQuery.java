package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InboxStateQuery {

    Long id;

    String countryCode;

    String stationId;

    Double lat;

    Double lon;

    UploadState state = UploadState.UNKNOWN;

    String rejectedReason;

    Long crc32;

}
