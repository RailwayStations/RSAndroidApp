package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class InboxStateQuery {

    private Long id;

    private String countryCode;

    private String stationId;

    private Double lat;

    private Double lon;

    private UploadState state = UploadState.UNKNOWN;

    private String rejectedReason;

    private Long crc32;

    public InboxStateQuery(final Long id, final String countryCode, final String stationId) {
        this(id);
        this.countryCode = countryCode;
        this.stationId = stationId;
    }

    public InboxStateQuery(final Long id) {
        super();
        this.id = id;
    }

    public Long getId() {
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

    public Long getCrc32() {
        return crc32;
    }

    public void setCrc32(final Long crc32) {
        this.crc32 = crc32;
    }
}
