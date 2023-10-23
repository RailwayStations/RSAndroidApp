package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import android.util.Log
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStation
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.COUNTRIES
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.PROVIDER_APPS
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.STATIONS
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.UPLOADS
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter
import org.apache.commons.lang3.StringUtils
import java.text.Normalizer
import java.util.Arrays
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.math.cos
import kotlin.math.pow

class DbAdapter(private val context: Context) {
    private var dbHelper: DbOpenHelper? = null
    private var db: SQLiteDatabase? = null
    fun open() {
        dbHelper = DbOpenHelper(context)
        db = dbHelper!!.writableDatabase
    }

    fun close() {
        db!!.close()
        dbHelper!!.close()
    }

    fun insertStations(photoStations: PhotoStations, countryCode: String?) {
        db!!.beginTransaction()
        try {
            deleteStations(java.util.Set.of(countryCode))
            photoStations.stations.forEach(Consumer { station: PhotoStation ->
                db!!.insert(
                    DATABASE_TABLE_STATIONS, null, toContentValues(station, photoStations)
                )
            })
            db!!.setTransactionSuccessful()
        } finally {
            db!!.endTransaction()
        }
    }

    private fun toContentValues(
        station: PhotoStation,
        photoStations: PhotoStations
    ): ContentValues {
        val values = ContentValues()
        values.put(STATIONS.ID, station.id)
        values.put(STATIONS.COUNTRY, station.country)
        values.put(STATIONS.TITLE, station.title)
        values.put(
            STATIONS.NORMALIZED_TITLE, StringUtils.replaceChars(
                StringUtils.deleteWhitespace(
                    StringUtils.stripAccents(
                        Normalizer.normalize(
                            station.title,
                            Normalizer.Form.NFC
                        )
                    )
                ), "-_()", null
            )
        )
        values.put(STATIONS.LAT, station.lat)
        values.put(STATIONS.LON, station.lon)
        values.put(STATIONS.DS100, station.shortCode)
        values.put(STATIONS.ACTIVE, !station.inactive)
        if (station.photos!!.isNotEmpty()) {
            val (id, photographer, path, _, license, outdated) = station.photos!![0]
            values.put(STATIONS.PHOTO_ID, id)
            values.put(STATIONS.PHOTO_URL, photoStations.photoBaseUrl + path)
            values.put(STATIONS.PHOTOGRAPHER, photographer)
            values.put(STATIONS.OUTDATED, outdated)
            values.put(STATIONS.PHOTOGRAPHER_URL, photoStations.getPhotographerUrl(photographer))
            values.put(STATIONS.LICENSE, photoStations.getLicenseName(license))
            values.put(STATIONS.LICENSE_URL, photoStations.getLicenseUrl(license))
        }
        return values
    }

    fun insertCountries(countries: List<Country>) {
        if (countries.isEmpty()) {
            return
        }
        db!!.beginTransaction()
        try {
            deleteCountries()
            countries.forEach(Consumer { country: Country -> insertCountry(country) })
            db!!.setTransactionSuccessful()
        } finally {
            db!!.endTransaction()
        }
    }

    private fun insertCountry(country: Country) {
        db!!.insert(DATABASE_TABLE_COUNTRIES, null, toContentValues(country))
        country.providerApps.stream()
            .map { p: ProviderApp -> toContentValues(country.code, p) }
            .forEach { values: ContentValues? ->
                db!!.insert(
                    DATABASE_TABLE_PROVIDER_APPS,
                    null,
                    values
                )
            }
    }

    private fun toContentValues(countryCode: String, app: ProviderApp): ContentValues {
        val values = ContentValues()
        values.put(PROVIDER_APPS.COUNTRYSHORTCODE, countryCode)
        values.put(PROVIDER_APPS.PA_TYPE, app.type)
        values.put(PROVIDER_APPS.PA_NAME, app.name)
        values.put(PROVIDER_APPS.PA_URL, app.url)
        return values
    }

    private fun toContentValues(country: Country): ContentValues {
        val values = ContentValues()
        values.put(COUNTRIES.COUNTRYSHORTCODE, country.code)
        values.put(COUNTRIES.COUNTRYNAME, country.name)
        values.put(COUNTRIES.EMAIL, country.email)
        values.put(COUNTRIES.TIMETABLE_URL_TEMPLATE, country.timetableUrlTemplate)
        values.put(COUNTRIES.OVERRIDE_LICENSE, country.overrideLicense)
        return values
    }

    fun insertUpload(upload: Upload): Upload {
        upload.id = db!!.insert(DATABASE_TABLE_UPLOADS, null, toContentValues(upload))
        return upload
    }

    private fun deleteStations(countryCodes: Set<String?>?) {
        db!!.delete(DATABASE_TABLE_STATIONS, whereCountryCodeIn(countryCodes), null)
    }

    private fun deleteCountries() {
        db!!.delete(DATABASE_TABLE_PROVIDER_APPS, null, null)
        db!!.delete(DATABASE_TABLE_COUNTRIES, null, null)
    }

    private fun getStationOrderBy(sortByDistance: Boolean, myPos: Location?): String {
        var orderBy = STATIONS.TITLE + " ASC"
        if (sortByDistance) {
            val fudge = cos(
                Math.toRadians(
                    myPos!!.latitude
                )
            ).pow(2.0)
            orderBy =
                "((" + myPos.latitude + " - " + STATIONS.LAT + ") * (" + myPos.latitude + " - " + STATIONS.LAT + ") + " +
                        "(" + myPos.longitude + " - " + STATIONS.LON + ") * (" + myPos.longitude + " - " + STATIONS.LON + ") * " + fudge + ")"
        }
        return orderBy
    }

    val countryList: Cursor?
        get() {
            val selectCountries =
                "SELECT " + COUNTRIES.ROWID_COUNTRIES + " AS " + Constants.CURSOR_ADAPTER_ID + ", " +
                        COUNTRIES.COUNTRYSHORTCODE + ", " + COUNTRIES.COUNTRYNAME +
                        " FROM " + DATABASE_TABLE_COUNTRIES + " ORDER BY " + COUNTRIES.COUNTRYNAME + " ASC"
            Log.d(TAG, selectCountries)
            val cursor = db!!.rawQuery(selectCountries, null)
            if (!cursor.moveToFirst()) {
                cursor.close()
                return null
            }
            return cursor
        }

    /**
     * Return a cursor on station ids where the station's title matches the given string
     *
     * @param search         the search keyword
     * @param stationFilter  if stations need to be filtered by photo available or not
     * @param countryCodes   countries to search for
     * @param sortByDistance sort by distance or by alphabet
     * @param myPos          current location
     * @return a Cursor representing the matching results
     */
    fun getStationsListByKeyword(
        search: String?,
        stationFilter: StationFilter,
        countryCodes: Set<String>,
        sortByDistance: Boolean,
        myPos: Location?
    ): Cursor? {
        var selectQuery = whereCountryCodeIn(countryCodes)
        val queryArgs = mutableListOf<String>()
        if (StringUtils.isNotBlank(search)) {
            selectQuery += String.format(" AND %s LIKE ?", STATIONS.NORMALIZED_TITLE)
            queryArgs.add(
                "%" + StringUtils.replaceChars(
                    StringUtils.stripAccents(
                        StringUtils.trimToEmpty(
                            search
                        )
                    ), " -_()", "%%%%%"
                ) + "%"
            )
        }
        if (stationFilter.nickname != null) {
            selectQuery += " AND " + STATIONS.PHOTOGRAPHER + " = ?"
            queryArgs.add(stationFilter.nickname!!)
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + STATIONS.PHOTO_URL + " IS " + (if (stationFilter.hasPhoto()!!) "NOT" else "") + " NULL"
        }
        if (stationFilter.isActive != null) {
            selectQuery += " AND " + STATIONS.ACTIVE + " = ?"
            queryArgs.add(if (stationFilter.isActive!!) "1" else "0")
        }
        Log.w(TAG, selectQuery)
        val cursor = db!!.query(
            DATABASE_TABLE_STATIONS, arrayOf<String?>(
                STATIONS.ROWID + " AS " + Constants.CURSOR_ADAPTER_ID,
                STATIONS.ID,
                STATIONS.TITLE,
                STATIONS.PHOTO_URL,
                STATIONS.COUNTRY
            ),
            selectQuery,
            queryArgs.toTypedArray(), null, null, getStationOrderBy(sortByDistance, myPos)
        )
        if (!cursor.moveToFirst()) {
            Log.w(TAG, String.format("Query '%s' returned no result", search))
            cursor.close()
            return null
        }
        return cursor
    }

    private fun whereCountryCodeIn(countryCodes: Set<String?>?): String {
        return STATIONS.COUNTRY +
                " IN (" +
                countryCodes!!.stream().map { c: String? -> "'$c'" }
                    .collect(Collectors.joining(",")) +
                ")"
    }

    fun getStatistic(country: String?): Statistic? {
        db!!.rawQuery(
            "SELECT COUNT(*), COUNT(" + STATIONS.PHOTO_URL + "), COUNT(DISTINCT(" + STATIONS.PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE_STATIONS + " WHERE " + STATIONS.COUNTRY + " = ?",
            arrayOf(country)
        ).use { cursor ->
            if (cursor.moveToNext()) {
                return Statistic(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getInt(0) - cursor.getInt(1),
                    cursor.getInt(2)
                )
            }
        }
        return null
    }

    val photographerNicknames: Array<String>
        get() {
            val photographers = ArrayList<String>()
            db!!.rawQuery(
                "SELECT distinct " + STATIONS.PHOTOGRAPHER + " FROM " + DATABASE_TABLE_STATIONS + " WHERE " + STATIONS.PHOTOGRAPHER + " IS NOT NULL ORDER BY " + STATIONS.PHOTOGRAPHER,
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    photographers.add(cursor.getString(0))
                }
            }
            return photographers.toTypedArray()
        }

    fun countStations(countryCodes: Set<String?>?): Int {
        db!!.rawQuery(
            "SELECT COUNT(*) FROM $DATABASE_TABLE_STATIONS WHERE " + whereCountryCodeIn(
                countryCodes
            ), null
        ).use { query ->
            if (query.moveToFirst()) {
                return query.getInt(0)
            }
        }
        return 0
    }

    fun updateUpload(upload: Upload?) {
        db!!.beginTransaction()
        try {
            db!!.update(
                DATABASE_TABLE_UPLOADS, toContentValues(upload), UPLOADS.ID + " = ?", arrayOf(
                    upload!!.id.toString()
                )
            )
            db!!.setTransactionSuccessful()
        } finally {
            db!!.endTransaction()
        }
    }

    private fun toContentValues(upload: Upload?): ContentValues {
        val values = ContentValues()
        values.put(UPLOADS.COMMENT, upload!!.comment)
        values.put(UPLOADS.COUNTRY, upload.country)
        values.put(UPLOADS.CREATED_AT, upload.createdAt)
        values.put(UPLOADS.INBOX_URL, upload.inboxUrl)
        values.put(UPLOADS.LAT, upload.lat)
        values.put(UPLOADS.LON, upload.lon)
        values.put(
            UPLOADS.PROBLEM_TYPE,
            if (upload.problemType != null) upload.problemType!!.name else null
        )
        values.put(UPLOADS.REJECTED_REASON, upload.rejectReason)
        values.put(UPLOADS.REMOTE_ID, upload.remoteId)
        values.put(UPLOADS.STATION_ID, upload.stationId)
        values.put(UPLOADS.TITLE, upload.title)
        values.put(UPLOADS.UPLOAD_STATE, upload.uploadState.name)
        values.put(UPLOADS.ACTIVE, upload.active)
        values.put(UPLOADS.CRC32, upload.crc32)
        values.put(UPLOADS.REMOTE_ID, upload.remoteId)
        return values
    }

    fun getPendingUploadsForStation(station: Station): List<Upload?> {
        val uploads = ArrayList<Upload?>()
        db!!.query(
            DATABASE_TABLE_UPLOADS,
            null,
            UPLOADS.COUNTRY + " = ? AND " + UPLOADS.STATION_ID + " = ? AND " + pendingUploadWhereClause,
            arrayOf(station.country, station.id),
            null,
            null,
            UPLOADS.CREATED_AT + " DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor))
            }
        }
        return uploads
    }

    fun getPendingUploadForCoordinates(lat: Double, lon: Double): Upload? {
        db!!.query(
            DATABASE_TABLE_UPLOADS,
            null,
            UPLOADS.LAT + " = ? AND " + UPLOADS.LON + " = ? AND " + pendingUploadWhereClause,
            arrayOf(lat.toString(), lon.toString()),
            null,
            null,
            UPLOADS.CREATED_AT + " DESC"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor)
            }
        }
        return null
    }

    private fun getUploadWhereClause(predicate: Predicate<UploadState>): String {
        return UPLOADS.UPLOAD_STATE + " IN (" +
                Arrays.stream(UploadState.values())
                    .filter(predicate)
                    .map { s: UploadState -> "'" + s.name + "'" }
                    .collect(Collectors.joining(",")) +
                ')'
    }

    private val pendingUploadWhereClause: String
        get() = getUploadWhereClause(UploadState::isPending)
    private val completedUploadWhereClause: String
        get() = getUploadWhereClause { s: UploadState -> !s.isPending }
    val outbox: Cursor
        get() {
            val queryBuilder = SQLiteQueryBuilder()
            queryBuilder.tables = (DATABASE_TABLE_UPLOADS
                    + " LEFT JOIN "
                    + DATABASE_TABLE_STATIONS
                    + " ON "
                    + DATABASE_TABLE_STATIONS + "." + STATIONS.COUNTRY
                    + " = "
                    + DATABASE_TABLE_UPLOADS + "." + UPLOADS.COUNTRY
                    + " AND "
                    + DATABASE_TABLE_STATIONS + "." + STATIONS.ID
                    + " = "
                    + DATABASE_TABLE_UPLOADS + "." + UPLOADS.STATION_ID)
            return queryBuilder.query(
                db, arrayOf(
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.ID + " AS " + Constants.CURSOR_ADAPTER_ID,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.REMOTE_ID,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.COUNTRY,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.STATION_ID,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.TITLE,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.UPLOAD_STATE,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.PROBLEM_TYPE,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.COMMENT,
                    DATABASE_TABLE_UPLOADS + "." + UPLOADS.REJECTED_REASON,
                    DATABASE_TABLE_STATIONS + "." + UPLOADS.TITLE + " AS " + UPLOADS.JOIN_STATION_TITLE
                ), null, null, null, null, UPLOADS.CREATED_AT + " DESC"
            )
        }

    fun getUploadById(id: Long): Upload? {
        db!!.query(
            DATABASE_TABLE_UPLOADS,
            null,
            UPLOADS.ID + "=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor)
            }
        }
        return null
    }

    fun deleteUpload(id: Long) {
        db!!.delete(DATABASE_TABLE_UPLOADS, UPLOADS.ID + "=?", arrayOf(id.toString()))
    }

    fun getPendingUploads(withRemoteId: Boolean): List<Upload> {
        var selection = pendingUploadWhereClause
        if (withRemoteId) {
            selection += " AND " + UPLOADS.REMOTE_ID + " IS NOT NULL"
        }
        val uploads = mutableListOf<Upload>()
        db!!.query(
            DATABASE_TABLE_UPLOADS, null, selection,
            null, null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor))
            }
        }
        return uploads
    }

    fun updateUploadStates(stateQueries: List<InboxStateQuery>) {
        db!!.beginTransaction()
        try {
            stateQueries.forEach(Consumer { state: InboxStateQuery ->
                db!!.update(
                    DATABASE_TABLE_UPLOADS,
                    toUploadStatesContentValues(state),
                    UPLOADS.REMOTE_ID + " = ?",
                    arrayOf(state.id.toString())
                )
            })
            db!!.setTransactionSuccessful()
        } finally {
            db!!.endTransaction()
        }
    }

    private fun toUploadStatesContentValues(state: InboxStateQuery): ContentValues {
        val values = ContentValues()
        values.put(UPLOADS.UPLOAD_STATE, state.state.name)
        values.put(UPLOADS.REJECTED_REASON, state.rejectedReason)
        values.put(UPLOADS.CRC32, state.crc32)
        return values
    }

    val completedUploads: List<Upload>
        get() {
            val uploads = ArrayList<Upload>()
            db!!.query(
                DATABASE_TABLE_UPLOADS, null,
                UPLOADS.REMOTE_ID + " IS NOT NULL AND " + completedUploadWhereClause,
                null, null, null, null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    uploads.add(createUploadFromCursor(cursor))
                }
            }
            return uploads
        }

    internal class DbOpenHelper(context: Context?) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            Log.i(TAG, "Creating database")
            db.execSQL(CREATE_STATEMENT_STATIONS)
            db.execSQL(CREATE_STATEMENT_STATIONS_IDX)
            db.execSQL(CREATE_STATEMENT_COUNTRIES)
            db.execSQL(CREATE_STATEMENT_PROVIDER_APPS)
            db.execSQL(CREATE_STATEMENT_UPLOADS)
            Log.i(TAG, "Database structure created.")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(TAG, "Upgrade database from version$oldVersion to $newVersion")
            db.beginTransaction()
            if (oldVersion < 13) {
                // up to version 13 we dropped all tables and recreated them
                db.execSQL(DROP_STATEMENT_STATIONS_IDX)
                db.execSQL(DROP_STATEMENT_STATIONS)
                db.execSQL(DROP_STATEMENT_COUNTRIES)
                db.execSQL(DROP_STATEMENT_PROVIDER_APPS)
                onCreate(db)
            } else {
                // from now on we need to preserve user data and perform schema changes selectively
                if (oldVersion < 14) {
                    db.execSQL(CREATE_STATEMENT_UPLOADS)
                }
                if (oldVersion < 15) {
                    db.execSQL(DROP_STATEMENT_STATIONS_IDX)
                    db.execSQL(CREATE_STATEMENT_STATIONS_IDX)
                }
                if (oldVersion < 16) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_COUNTRIES + " ADD COLUMN " + COUNTRIES.OVERRIDE_LICENSE + " TEXT")
                }
                if (oldVersion < 17) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_UPLOADS + " ADD COLUMN " + UPLOADS.ACTIVE + " INTEGER")
                }
                if (oldVersion < 18) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + STATIONS.NORMALIZED_TITLE + " TEXT")
                    db.execSQL("UPDATE " + DATABASE_TABLE_STATIONS + " SET " + STATIONS.NORMALIZED_TITLE + " = " + STATIONS.TITLE)
                }
                if (oldVersion < 19) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_UPLOADS + " ADD COLUMN " + UPLOADS.CRC32 + " INTEGER")
                }
                if (oldVersion < 20) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + STATIONS.OUTDATED + " INTEGER")
                }
                if (oldVersion < 21) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + STATIONS.PHOTO_ID + " INTEGER")
                }
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }
    }

    private fun createStationFromCursor(cursor: Cursor): Station {
        return Station(
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.COUNTRY)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.TITLE)),
            cursor.getDouble(cursor.getColumnIndexOrThrow(STATIONS.LAT)),
            cursor.getDouble(cursor.getColumnIndexOrThrow(STATIONS.LON)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.DS100)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.PHOTO_URL)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.PHOTOGRAPHER)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.PHOTOGRAPHER_URL)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.LICENSE)),
            cursor.getString(cursor.getColumnIndexOrThrow(STATIONS.LICENSE_URL)),
            java.lang.Boolean.TRUE == getBoolean(cursor, STATIONS.ACTIVE),
            java.lang.Boolean.TRUE == getBoolean(cursor, STATIONS.OUTDATED),
            cursor.getLong(cursor.getColumnIndexOrThrow(STATIONS.PHOTO_ID))
        )
    }

    private fun createCountryFromCursor(cursor: Cursor): Country {
        return Country(
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYSHORTCODE)),
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.COUNTRYNAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.EMAIL)),
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.TIMETABLE_URL_TEMPLATE)),
            cursor.getString(cursor.getColumnIndexOrThrow(COUNTRIES.OVERRIDE_LICENSE))
        )
    }

    private fun createProviderAppFromCursor(cursor: Cursor): ProviderApp {
        return ProviderApp(
            cursor.getString(cursor.getColumnIndexOrThrow(PROVIDER_APPS.PA_TYPE)),
            cursor.getString(cursor.getColumnIndexOrThrow(PROVIDER_APPS.PA_NAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(PROVIDER_APPS.PA_URL))
        )
    }

    private fun createUploadFromCursor(cursor: Cursor): Upload {
        val upload = Upload(
            cursor.getLong(cursor.getColumnIndexOrThrow(UPLOADS.ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.COUNTRY)),
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.STATION_ID)),
            getLong(cursor, UPLOADS.REMOTE_ID),
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.TITLE)),
            getDouble(cursor, UPLOADS.LAT),
            getDouble(cursor, UPLOADS.LON),
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.COMMENT)),
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.INBOX_URL)),
            null,
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.REJECTED_REASON)),
            UploadState.UNKNOWN,
            cursor.getLong(cursor.getColumnIndexOrThrow(UPLOADS.CREATED_AT)),
            getBoolean(cursor, UPLOADS.ACTIVE),
            getLong(cursor, UPLOADS.CRC32)
        )
        val problemType = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.PROBLEM_TYPE))
        if (problemType != null) {
            upload.problemType = ProblemType.valueOf(problemType)
        }
        val uploadState = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.UPLOAD_STATE))
        if (uploadState != null) {
            upload.uploadState = UploadState.valueOf(uploadState)
        }
        return upload
    }

    private fun getBoolean(cursor: Cursor, columnName: String?): Boolean? {
        return if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            cursor.getInt(cursor.getColumnIndexOrThrow(columnName)) == 1
        } else null
    }

    private fun getDouble(cursor: Cursor, columnName: String?): Double? {
        return if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            cursor.getDouble(cursor.getColumnIndexOrThrow(columnName))
        } else null
    }

    private fun getLong(cursor: Cursor, columnName: String?): Long? {
        return if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            cursor.getLong(cursor.getColumnIndexOrThrow(columnName))
        } else null
    }

    fun fetchStationByRowId(id: Long): Station? {
        db!!.query(
            DATABASE_TABLE_STATIONS, null, STATIONS.ROWID + "=?", arrayOf(
                id.toString() + ""
            ), null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor)
            }
        }
        return null
    }

    fun getStationByKey(country: String?, id: String?): Station? {
        db!!.query(
            DATABASE_TABLE_STATIONS,
            null,
            STATIONS.COUNTRY + "=? AND " + STATIONS.ID + "=?",
            arrayOf(country, id),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor)
            }
        }
        return null
    }

    fun getStationForUpload(upload: Upload): Station? {
        db!!.query(
            DATABASE_TABLE_STATIONS,
            null,
            STATIONS.COUNTRY + "=? AND " + STATIONS.ID + "=?",
            arrayOf(upload.country, upload.stationId),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor)
            }
        }
        return null
    }

    fun fetchCountriesWithProviderApps(countryCodes: Set<String?>?): Set<Country> {
        val countryList = countryCodes!!.stream()
            .map { c: String? -> "'$c'" }
            .collect(Collectors.joining(","))
        val countries = HashSet<Country>()
        db!!.query(
            DATABASE_TABLE_COUNTRIES,
            null,
            COUNTRIES.COUNTRYSHORTCODE + " IN (" + countryList + ")",
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val country = createCountryFromCursor(cursor)
                    countries.add(country)
                    db!!.query(
                        DATABASE_TABLE_PROVIDER_APPS,
                        null,
                        PROVIDER_APPS.COUNTRYSHORTCODE + " = ?",
                        arrayOf<String>(country.code),
                        null,
                        null,
                        null
                    ).use { cursorPa ->
                        if (cursorPa != null && cursorPa.moveToFirst()) {
                            do {
                                country.providerApps.add(createProviderAppFromCursor(cursorPa))
                            } while (cursorPa.moveToNext())
                        }
                    }
                } while (cursor.moveToNext())
            }
        }
        return countries
    }

    fun getAllStations(stationFilter: StationFilter, countryCodes: Set<String>): List<Station> {
        val stationList = ArrayList<Station>()
        var selectQuery =
            "SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + whereCountryCodeIn(countryCodes)
        val queryArgs = ArrayList<String?>()
        if (stationFilter.nickname != null) {
            selectQuery += " AND " + STATIONS.PHOTOGRAPHER + " = ?"
            queryArgs.add(stationFilter.nickname)
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + STATIONS.PHOTO_URL + " IS " + (if (stationFilter.hasPhoto()!!) "NOT" else "") + " NULL"
        }
        if (stationFilter.isActive != null) {
            selectQuery += " AND " + STATIONS.ACTIVE + " = ?"
            queryArgs.add(if (stationFilter.isActive!!) "1" else "0")
        }
        db!!.rawQuery(selectQuery, queryArgs.toArray(arrayOf())).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        }
        return stationList
    }

    fun getStationByLatLngRectangle(
        lat: Double,
        lng: Double,
        stationFilter: StationFilter
    ): List<Station> {
        val stationList = ArrayList<Station>()
        // Select All Query with rectangle - might be later change with it
        var selectQuery =
            ("SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + STATIONS.LAT + " < " + (lat + 0.5) + " AND " + STATIONS.LAT + " > " + (lat - 0.5)
                    + " AND " + STATIONS.LON + " < " + (lng + 0.5) + " AND " + STATIONS.LON + " > " + (lng - 0.5))
        val queryArgs = ArrayList<String?>()
        if (stationFilter.nickname != null) {
            selectQuery += " AND " + STATIONS.PHOTOGRAPHER + " = ?"
            queryArgs.add(stationFilter.nickname)
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + STATIONS.PHOTO_URL + " IS " + (if (stationFilter.hasPhoto()!!) "NOT" else "") + " NULL"
        }
        if (stationFilter.isActive != null) {
            selectQuery += " AND " + STATIONS.ACTIVE + " = ?"
            queryArgs.add(if (stationFilter.isActive!!) "1" else "0")
        }
        db!!.rawQuery(selectQuery, queryArgs.toArray(arrayOf())).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        }
        return stationList
    }

    val allCountries: Set<Country>
        get() {
            val countryList = HashSet<Country>()
            val query = "SELECT * FROM $DATABASE_TABLE_COUNTRIES"
            Log.d(TAG, query)
            db!!.rawQuery(query, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        countryList.add(createCountryFromCursor(cursor))
                    } while (cursor.moveToNext())
                }
            }
            return countryList
        }

    companion object {
        private val TAG = DbAdapter::class.java.simpleName
        private const val DATABASE_TABLE_STATIONS = "bahnhoefe"
        private const val DATABASE_TABLE_COUNTRIES = "laender"
        private const val DATABASE_TABLE_PROVIDER_APPS = "providerApps"
        private const val DATABASE_TABLE_UPLOADS = "uploads"
        private const val DATABASE_NAME = "bahnhoefe.db"
        private const val DATABASE_VERSION = 22
        private const val CREATE_STATEMENT_STATIONS =
            ("CREATE TABLE " + DATABASE_TABLE_STATIONS + " ("
                    + STATIONS.ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + STATIONS.COUNTRY + " TEXT, "
                    + STATIONS.ID + " TEXT, "
                    + STATIONS.TITLE + " TEXT, "
                    + STATIONS.NORMALIZED_TITLE + " TEXT, "
                    + STATIONS.LAT + " REAL, "
                    + STATIONS.LON + " REAL, "
                    + STATIONS.PHOTO_ID + " INTEGER, "
                    + STATIONS.PHOTO_URL + " TEXT, "
                    + STATIONS.PHOTOGRAPHER + " TEXT, "
                    + STATIONS.PHOTOGRAPHER_URL + " TEXT, "
                    + STATIONS.LICENSE + " TEXT, "
                    + STATIONS.LICENSE_URL + " TEXT, "
                    + STATIONS.DS100 + " TEXT, "
                    + STATIONS.ACTIVE + " INTEGER, "
                    + STATIONS.OUTDATED + " INTEGER)")
        private const val CREATE_STATEMENT_STATIONS_IDX =
            ("CREATE INDEX " + DATABASE_TABLE_STATIONS + "_IDX "
                    + "ON " + DATABASE_TABLE_STATIONS + "(" + STATIONS.COUNTRY + ", " + STATIONS.ID + ")")
        private const val CREATE_STATEMENT_COUNTRIES =
            ("CREATE TABLE " + DATABASE_TABLE_COUNTRIES + " ("
                    + COUNTRIES.ROWID_COUNTRIES + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COUNTRIES.COUNTRYSHORTCODE + " TEXT, "
                    + COUNTRIES.COUNTRYNAME + " TEXT, "
                    + COUNTRIES.EMAIL + " TEXT, "
                    + COUNTRIES.TIMETABLE_URL_TEMPLATE + " TEXT, "
                    + COUNTRIES.OVERRIDE_LICENSE + " TEXT)")
        private const val CREATE_STATEMENT_PROVIDER_APPS =
            ("CREATE TABLE " + DATABASE_TABLE_PROVIDER_APPS + " ("
                    + PROVIDER_APPS.COUNTRYSHORTCODE + " TEXT,"
                    + PROVIDER_APPS.PA_TYPE + " TEXT,"
                    + PROVIDER_APPS.PA_NAME + " TEXT, "
                    + PROVIDER_APPS.PA_URL + " TEXT)")
        private const val CREATE_STATEMENT_UPLOADS =
            ("CREATE TABLE " + DATABASE_TABLE_UPLOADS + " ("
                    + UPLOADS.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + UPLOADS.STATION_ID + " TEXT, "
                    + UPLOADS.COUNTRY + " TEXT, "
                    + UPLOADS.REMOTE_ID + " INTEGER, "
                    + UPLOADS.TITLE + " TEXT, "
                    + UPLOADS.LAT + " REAL, "
                    + UPLOADS.LON + " REAL, "
                    + UPLOADS.COMMENT + " TEXT, "
                    + UPLOADS.INBOX_URL + " TEXT, "
                    + UPLOADS.PROBLEM_TYPE + " TEXT, "
                    + UPLOADS.REJECTED_REASON + " TEXT, "
                    + UPLOADS.UPLOAD_STATE + " TEXT, "
                    + UPLOADS.CREATED_AT + " INTEGER, "
                    + UPLOADS.ACTIVE + " INTEGER, "
                    + UPLOADS.CRC32 + " INTEGER)")
        private const val DROP_STATEMENT_STATIONS_IDX =
            "DROP INDEX IF EXISTS " + DATABASE_TABLE_STATIONS + "_IDX"
        private const val DROP_STATEMENT_STATIONS =
            "DROP TABLE IF EXISTS $DATABASE_TABLE_STATIONS"
        private const val DROP_STATEMENT_COUNTRIES =
            "DROP TABLE IF EXISTS $DATABASE_TABLE_COUNTRIES"
        private const val DROP_STATEMENT_PROVIDER_APPS =
            "DROP TABLE IF EXISTS $DATABASE_TABLE_PROVIDER_APPS"
    }
}