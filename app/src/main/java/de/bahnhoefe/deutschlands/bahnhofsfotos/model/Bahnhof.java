package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

/**
 * Created by android_oma on 29.05.16.
 */

public class Bahnhof implements Serializable{
    private int id;  //Bahnhofsnummer
    private String title; //Bahnhofsname
    private float lat;
    private float lon;
    private long datum; // not used in the database
    private String photoflag; // not used in the database

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

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public long getDatum() {
        return datum;
    }

    public void setDatum(long datum) {
        this.datum = datum;
    }

    public String getPhotoflag() {
        return photoflag;
    }

    public void setPhotoflag(String photoflag) {
        this.photoflag = photoflag;
    }

    @Override
    public String toString(){
        return "Bahnhof [id=" + id + ", title=" + title + ", lat=" + lat + ", lon=" + lon  + "]";
    }
}
