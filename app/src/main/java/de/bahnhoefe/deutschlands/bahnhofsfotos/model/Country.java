package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;

public class Country implements Serializable {
    private String countryName;
    private String countryShortCode;
    private String email;
    private String twitterTags;
    private String timetableUrlTemplate;

    public Country() {

    }

    public Country(String countryName, String countryShortCode, String email, String twitterTags, String timetableUrlTemplate) {
        this.countryName = countryName;
        this.countryShortCode = countryShortCode;
        this.email = email;
        this.twitterTags = twitterTags;
        this.timetableUrlTemplate = timetableUrlTemplate;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryShortCode() {
        return countryShortCode;
    }

    public void setCountryShortCode(String countryShortCode) {
        this.countryShortCode = countryShortCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTwitterTags() {
        return twitterTags;
    }

    public void setTwitterTags(String twitterTags) {
        this.twitterTags = twitterTags;
    }

    public String getTimetableUrlTemplate() {
        return timetableUrlTemplate;
    }

    public void setTimetableUrlTemplate(String timetableUrlTemplate) {
        this.timetableUrlTemplate = timetableUrlTemplate;
    }

    @Override
    public String toString() {
        return "Land [Laendername=" + countryName + ", Laenderkuerzel=" + countryShortCode + ", E-Mail=" + email + ", TwitterTags=" + twitterTags + ", TimetableUrlTemplate=" + timetableUrlTemplate + "]";
    }

}
