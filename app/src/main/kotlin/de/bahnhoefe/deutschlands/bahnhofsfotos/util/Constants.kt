package de.bahnhoefe.deutschlands.bahnhofsfotos.util

object Constants {
    const val STORED_PHOTO_WIDTH = 1920
    const val STORED_PHOTO_QUALITY = 95
    const val CURSOR_ADAPTER_ID = "_id"

    /**
     * Columns of Stations table
     */
    object STATIONS {
        const val ROWID = "rowid"
        const val ID = "id"
        const val TITLE = "title"
        const val NORMALIZED_TITLE = "normalizedTitle"
        const val COUNTRY = "country"
        const val LAT = "lat"
        const val LON = "lon"
        const val PHOTO_ID = "photoId"
        const val PHOTO_URL = "photoUrl"
        const val PHOTOGRAPHER = "photographer"
        const val PHOTOGRAPHER_URL = "photographerUrl"
        const val LICENSE = "license"
        const val LICENSE_URL = "licenseUrl"
        const val DS100 = "DS100"
        const val ACTIVE = "active"
        const val OUTDATED = "outdated"
    }

    /**
     * Columns of Countries table
     */
    object COUNTRIES {
        const val COUNTRYNAME = "country"
        const val COUNTRYSHORTCODE = "countryflag"
        const val EMAIL = "mail"
        const val ROWID_COUNTRIES = "rowidcountries"
        const val TIMETABLE_URL_TEMPLATE = "timetable_url_template"
        const val OVERRIDE_LICENSE = "override_license"
    }

    /**
     * Columns of ProviderApps table
     */
    object PROVIDER_APPS {
        const val COUNTRYSHORTCODE = "countryflag"
        const val PA_TYPE = "type"
        const val PA_NAME = "name"
        const val PA_URL = "url"
    }

    /**
     * Columns of Uploads table
     */
    object UPLOADS {
        const val ID = "id"
        const val REMOTE_ID = "remoteId"
        const val TITLE = "title"
        const val COUNTRY = "country"
        const val STATION_ID = "stationId"
        const val LAT = "lat"
        const val LON = "lon"
        const val PROBLEM_TYPE = "problemType"
        const val INBOX_URL = "inboxUrl"
        const val UPLOAD_STATE = "uploadState"
        const val REJECTED_REASON = "rejectedReason"
        const val CREATED_AT = "createdAt"
        const val COMMENT = "comment"
        const val JOIN_STATION_TITLE = "stationTitle" // only for join with station
        const val ACTIVE = "active"
        const val CRC32 = "crc32"
    }
}