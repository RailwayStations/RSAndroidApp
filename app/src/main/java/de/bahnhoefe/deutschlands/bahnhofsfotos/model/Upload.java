package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

public class Upload implements Serializable {

    private Long id;
    private String country;
    private String stationId;
    private Long remoteId;
    private String title;
    private Double lat;
    private Double lon;
    private String comment;
    private String inboxUrl;
    private ProblemType problemType;
    private String rejectReason;
    private UploadState uploadState = UploadState.UNKNOWN;
    private Long createdAt = System.currentTimeMillis();

    public Upload() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public Long getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(Long remoteId) {
        this.remoteId = remoteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setInboxUrl(String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public ProblemType getProblemType() {
        return problemType;
    }

    public void setProblemType(ProblemType problemType) {
        this.problemType = problemType;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public UploadState getUploadState() {
        return uploadState;
    }

    public void setUploadState(UploadState uploadState) {
        this.uploadState = uploadState;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

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

    private boolean isPending() {
        return uploadState.isPending();
    }
}
