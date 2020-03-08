package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class UploadStateQuery {

    protected String countryCode;

    protected String id;

    protected Double lat;

    protected Double lon;

    private UploadState state = UploadState.UNKNOWN;

    public UploadStateQuery() {
    }

    public UploadStateQuery(final String countryCode, final String id) {
        super();
        this.countryCode = countryCode;
        this.id = id;
    }

    public UploadStateQuery(final Double lat, final Double lon) {
        super();
        this.lat = lat;
        this.lon = lon;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

}
