package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class UploadStateQuery {

    protected String countryCode;

    protected String id;

    protected Double lat;

    protected Double lon;

    private UploadStateState state = UploadStateState.UNKNOWN;

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

    public UploadStateState getState() {
        return state;
    }

    public void setState(UploadStateState state) {
        this.state = state;
    }

    public enum UploadStateState {
        UNKNOWN(R.string.upload_state_unknown, R.color.gridItem),
        IN_REVIEW(R.string.upload_state_in_review, R.color.gridItemReview),
        ACCEPTED(R.string.upload_state_accepted, R.color.gridItemGood),
        OTHER_USER(R.string.upload_state_other_user, R.color.gridItemError);

        private int textId;

        private int colorId;

        UploadStateState(int textId, int colorId) {
            this.textId = textId;
            this.colorId = colorId;
        }

        public int getTextId() {
            return textId;
        }

        public int getColorId() {
            return colorId;
        }
    }

}
