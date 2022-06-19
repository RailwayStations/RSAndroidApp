package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class ProblemReport {

    private String countryCode;
    private String stationId;
    private String comment;
    private ProblemType type;

    public ProblemReport(String countryCode, String stationId, String comment, ProblemType type) {
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.comment = comment;
        this.type = type;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ProblemType getType() {
        return type;
    }

    public void setType(ProblemType type) {
        this.type = type;
    }
}
