package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class InboxStateQuery {

    private long id;

    private String countryCode;

    private String stationId;

    private Double lat;

    private Double lon;

    private UploadState state = UploadState.UNKNOWN;

    private String rejectedReason;

    public InboxStateQuery() {
    }

    public InboxStateQuery(final long id) {
        super();
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(final Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(final Double lon) {
        this.lon = lon;
    }

    public UploadState getState() {
        return state;
    }

    public void setState(final UploadState state) {
        this.state = state;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(final String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }
}
