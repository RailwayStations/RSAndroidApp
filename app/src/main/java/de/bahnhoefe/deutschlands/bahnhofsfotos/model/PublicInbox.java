package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class PublicInbox {

    private String title;
    private String countryCode;
    private String stationId;
    private Double lat;
    private Double lon;

    public Double getLon() {
        return lon;
    }

    public void setLon(final Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(final Double lat) {
        this.lat = lat;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }
}
