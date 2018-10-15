package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

public class Constants {

    public static final String PHOTO_DIRECTORY = "Bahnhofsfotos";

    public final static class DB_JSON_CONSTANTS {
        //Bahnhofs-Konstanten
        public static final String KEY_ROWID = "rowid";
        public static final String KEY_ID = "id";
        public static final String KEY_TITLE = "title";
        public static final String KEY_LAT = "lat";
        public static final String KEY_LON = "lon";
        public static final String KEY_PHOTO_URL = "photoUrl";
        public static final String KEY_PHOTOGRAPHER = "photographer";
        public static final String KEY_PHOTOGRAPHER_URL = "photographerUrl";
        public static final String KEY_LICENSE = "license";
        public static final String KEY_LICENSE_URL = "licenseUrl";
        public static final String KEY_DS100 = "DS100";

        //Länderkonstanten
        public static final String KEY_COUNTRYNAME = "country";
        public static final String KEY_COUNTRYSHORTCODE = "countryflag";
        public static final String KEY_EMAIL = "mail";
        public static final String KEY_TWITTERTAGS = "twitter_tags";
        public static final String KEY_ROWID_COUNTRIES = "rowidcountries";
        public static final String KEY_TIMETABLE_URL_TEMPLATE = "timetable_url_template";

    }

    // Links zusammenschrauben
    public static final String API_START_URL = "https://api.railway-stations.org";

}
