package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

import com.google.android.gms.maps.model.LatLng;

public class Bahnhof implements Serializable {
    private int id;  //Bahnhofsnummer
    private String title; //Bahnhofsname
    private double lat;
    private double lon;
    private long datum; // not used in the database
    private String photoflag; // not used in the database
    private String ds100;

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public LatLng getPosition() {
        return new LatLng(lat, lon);
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

        if (id != bahnhof.id) return false;
        if (Double.compare(bahnhof.lat, lat) != 0) return false;
        if (Double.compare(bahnhof.lon, lon) != 0) return false;
        if (datum != bahnhof.datum) return false;
        if (!title.equals(bahnhof.title)) return false;
        return photoflag != null ? photoflag.equals(bahnhof.photoflag) : bahnhof.photoflag == null;

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getPhotoflag() {
        return photoflag;
    }

    public void setPhotoflag(String photoflag) {
        this.photoflag = photoflag;
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
}
