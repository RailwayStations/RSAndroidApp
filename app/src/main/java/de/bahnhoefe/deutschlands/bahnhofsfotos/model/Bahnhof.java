package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

public class Bahnhof implements Serializable {
    private String id;  //Bahnhofsnummer
    private String title; //Bahnhofsname
    private double lat;
    private double lon;
    private long datum; // not used in the database
    private String ds100;
    private String photoUrl;
    private String photographer;
    private String photographerUrl;
    private String license;
    private String licenseUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getDatum() {
        return datum;
    }

    public void setDatum(long datum) {
        this.datum = datum;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bahnhof)) return false;

        Bahnhof bahnhof = (Bahnhof) o;

        return id.equals(bahnhof.id);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Bahnhof [id=" + id + ", title=" + title + ", lat=" + lat + ", lon=" + lon + "]";
    }

    public String getDS100() {
        return ds100;
    }

    public void setDS100(String ds100) {
        this.ds100 = ds100;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotographer(String photographer) {
        this.photographer = photographer;
    }

    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographerUrl(String photographerUrl) {
        this.photographerUrl = photographerUrl;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicense() {
        return license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public boolean hasPhoto() {
        return photoUrl != null;
    }

}
