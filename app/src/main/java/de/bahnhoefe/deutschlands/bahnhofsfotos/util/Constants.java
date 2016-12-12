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
        public static final String KEY_DATE = "datum";
        public static final String KEY_PHOTOFLAG = "photoflag";

        //Länderkonstanten
        public static final String KEY_COUNTRYNAME = "country";
        public static final String KEY_COUNTRYSHORTCODE = "countryflag";
        public static final String KEY_EMAIL = "mail";
        public static final String KEY_TWITTERTAGS = "twitter_tags";
        public static final String KEY_ROWID_COUNTRIES = "rowidcountries";
    }

    public static final String BAHNHOEFE_OHNE_PHOTO_URL = "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de/bahnhoefe-withoutPhoto.json";
    //public static final String BAHNHOEFE_OHNE_PHOTO_URL = "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de/de/bahnhoefe?hasPhoto=false";
    public static final String INTERNATIONALE_BAHNHOEFE_OHNE_PHOTO_URL = "http://www.flying-snail.de/transit-train_station.json";
    public static final String BAHNHOEFE_MIT_PHOTO_URL =  "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de/bahnhoefe-withPhoto.json";
    //public static final String BAHNHOEFE_MIT_PHOTO_URL =  "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de/de/bahnhoefe?hasPhoto=true";

    // Links zusammenschrauben
    public static final String BAHNHOEFE_START_URL = "http://fotouebersicht.xn--deutschlands-bahnhfe-lbc.de";
    public static final String BAHNHOEFE_END_URL = "bahnhoefe?hasPhoto=";
    public static final String LAENDERDATEN_URL = "http://www.deutschlands-bahnhoefe.org/laenderdaten.json";

}
