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

    public InboxStateQuery(Long id, String countryCode, String stationId) {
        this(id);
        this.countryCode = countryCode;
        this.stationId = stationId;
    }

    public InboxStateQuery(Long id) {
        super();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
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

    public UploadState getState() {
        return state;
    }

    public void setState(UploadState state) {
        this.state = state;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public Long getCrc32() {
        return crc32;
    }

    public void setCrc32(Long crc32) {
        this.crc32 = crc32;
    }
}
