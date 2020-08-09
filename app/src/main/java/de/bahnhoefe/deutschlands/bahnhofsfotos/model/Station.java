package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Station implements Serializable {

    private String country;

    @SerializedName("idStr") private String id;  //Bahnhofsnummer
    private String title; //Bahnhofsname
    private double lat;
    private double lon;
    private long datum; // not used in the database

    @SerializedName("DS100") private String ds100;
    private String photoUrl;
    private String photographer;
    private String photographerUrl;
    private String license;
    private String licenseUrl;
    private boolean active;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(final double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(final double lon) {
        this.lon = lon;
    }

    public long getDatum() {
        return datum;
    }

    public void setDatum(final long datum) {
        this.datum = datum;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Station)) return false;

        final Station station = (Station) o;

        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Bahnhof [id=" + id + ", title=" + title + ", lat=" + lat + ", lon=" + lon + ", DS100=" + ds100 + "]";
    }

    public String getDs100() {
        return ds100;
    }

    public void setDs100(final String ds100) {
        this.ds100 = ds100;
    }

    public void setPhotoUrl(final String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotographer(final String photographer) {
        this.photographer = photographer;
    }

    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographerUrl(final String photographerUrl) {
        this.photographerUrl = photographerUrl;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    public String getLicense() {
        return license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(final String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public boolean hasPhoto() {
        return photoUrl != null;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }
}
