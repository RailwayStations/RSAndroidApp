package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Upload implements Serializable {

    Long id;
    String country;
    String stationId;
    Long remoteId;
    String title;
    Double lat;
    Double lon;
    String comment;
    String inboxUrl;
    ProblemType problemType;
    String rejectReason;
    @Builder.Default
    UploadState uploadState = UploadState.NOT_YET_SENT;
    @Builder.Default
    Long createdAt = System.currentTimeMillis();
    Boolean active;
    Long crc32;

    public boolean isUploadForExistingStation() {
        return country != null && stationId != null;
    }

    public boolean isUploadForMissingStation() {
        return lat != null && lon != null;
    }

    public boolean isProblemReport() {
        return problemType != null;
    }

    public boolean isPendingPhotoUpload() {
        return (isUploadForExistingStation() || isUploadForMissingStation()) && isPending() && !isProblemReport();
    }

    public boolean isPending() {
        return uploadState.isPending();
    }

    public boolean isUploaded() {
        return remoteId != null;
    }

}
