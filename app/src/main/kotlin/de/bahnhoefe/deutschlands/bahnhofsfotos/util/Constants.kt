package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

public class Constants {

    public static final int STORED_PHOTO_WIDTH = 1920;
    public static final int STORED_PHOTO_QUALITY = 95;
    public static final String CURSOR_ADAPTER_ID = "_id";

    /**
     * Columns of Stations table
     */
    public final static class STATIONS {
        public static final String ROWID = "rowid";
        public static final String ID = "id";
        public static final String TITLE = "title";
        public static final String NORMALIZED_TITLE = "normalizedTitle";
        public static final String COUNTRY = "country";
        public static final String LAT = "lat";
        public static final String LON = "lon";
        public static final String PHOTO_ID = "photoId";
        public static final String PHOTO_URL = "photoUrl";
        public static final String PHOTOGRAPHER = "photographer";
        public static final String PHOTOGRAPHER_URL = "photographerUrl";
        public static final String LICENSE = "license";
        public static final String LICENSE_URL = "licenseUrl";
        public static final String DS100 = "DS100";
        public static final String ACTIVE = "active";
        public static final String OUTDATED = "outdated";
    }

    /**
     * Columns of Countries table
     */
    public final static class COUNTRIES {
        public static final String COUNTRYNAME = "country";
        public static final String COUNTRYSHORTCODE = "countryflag";
        public static final String EMAIL = "mail";
        public static final String ROWID_COUNTRIES = "rowidcountries";
        public static final String TIMETABLE_URL_TEMPLATE = "timetable_url_template";
        public static final String OVERRIDE_LICENSE = "override_license";
    }

    /**
     * Columns of ProviderApps table
     */
    public final static class PROVIDER_APPS {
        public static final String COUNTRYSHORTCODE = "countryflag";
        public static final String PA_TYPE = "type";
        public static final String PA_NAME = "name";
        public static final String PA_URL = "url";
    }

    /**
     * Columns of Uploads table
     */
    public final static class UPLOADS {
        public static final String ID = "id";
        public static final String REMOTE_ID = "remoteId";
        public static final String TITLE = "title";
        public static final String COUNTRY = "country";
        public static final String STATION_ID = "stationId";
        public static final String LAT = "lat";
        public static final String LON = "lon";
        public static final String PROBLEM_TYPE = "problemType";
        public static final String INBOX_URL = "inboxUrl";
        public static final String UPLOAD_STATE = "uploadState";
        public static final String REJECTED_REASON = "rejectedReason";
        public static final String CREATED_AT = "createdAt";
        public static final String COMMENT = "comment";
        public static final String JOIN_STATION_TITLE = "stationTitle"; // only for join with station
        public static final String ACTIVE = "active";
        public static final String CRC32 = "crc32";
    }

}
