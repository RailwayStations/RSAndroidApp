package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

/**
 * Created by android_oma on 29.05.16.
 */

public class Constants {

    public final static class DB_JSON_CONSTANTS {
        //Bahnhofs-Konstanten
        public static final String KEY_ROWID = "rowid";
        public static final String KEY_ID = "id";
        public static final String KEY_TITLE = "title";
        public static final String KEY_LAT = "lat";
        public static final String KEY_LON = "lon";
        public static final String KEY_PHOTOFLAG = "photoflag";
        public static final String KEY_PHOTOGRAPHER = "photographer";

        //Länderkonstanten
        public static final String KEY_COUNTRYNAME = "country";
        public static final String KEY_COUNTRYSHORTCODE = "countryflag";
        public static final String KEY_EMAIL = "mail";
        public static final String KEY_TWITTERTAGS = "twitter_tags";
        public static final String KEY_ROWID_COUNTRIES = "rowidcountries";
    }

    // Links zusammenschrauben
    public static final String API_START_URL = "https://api.railway-stations.org";
    public static final String LAENDERDATEN_URL = "https://railway-stations.org/laenderdaten.json";

}
