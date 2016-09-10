package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

/**
 * Created by android_oma on 29.05.16.
 */

public class Constants {
    public final static class DB_JSON_CONSTANTS {
        public static final String KEY_ROWID = "rowid";
        public static final String KEY_ID = "id";
        public static final String KEY_TITLE = "title";
        public static final String KEY_LAT = "lat";
        public static final String KEY_LON = "lon";
        public static final String KEY_DATE = "datum";
        public static final String KEY_PHOTOFLAG = "photoflag";
    }

    public static final String BAHNHOEFE_OHNE_PHOTO_URL = "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de/bahnhoefe-withoutPhoto.json";
    public static final String BAHNHOEFE_MIT_PHOTO_URL =  "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de/bahnhoefe-withPhoto.json";
}
