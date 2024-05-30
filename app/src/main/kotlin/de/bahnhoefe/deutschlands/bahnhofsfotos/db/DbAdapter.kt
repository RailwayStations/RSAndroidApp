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
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.Countries
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.ProviderApps
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.Stations
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.Uploads
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter
import org.apache.commons.lang3.StringUtils
import java.text.Normalizer
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.cos
import kotlin.math.pow

class DbAdapter(private val context: Context) {
    private lateinit var dbHelper: DbOpenHelper
    private lateinit var db: SQLiteDatabase
    fun open() {
        dbHelper = DbOpenHelper(context)
        db = dbHelper.writableDatabase
    }

    fun insertStations(photoStations: PhotoStations, countryCode: String) {
        db.beginTransaction()
        try {
            deleteStations(mutableSetOf(countryCode))
            photoStations.stations.forEach(Consumer { station: PhotoStation ->
                db.insert(
                    DATABASE_TABLE_STATIONS, null, toContentValues(station, photoStations)
                )
            })
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun toContentValues(
        station: PhotoStation,
        photoStations: PhotoStations
    ): ContentValues {
        val values = ContentValues()
        values.put(Stations.ID, station.id)
        values.put(Stations.COUNTRY, station.country)
        values.put(Stations.TITLE, station.title)
        values.put(
            Stations.NORMALIZED_TITLE, StringUtils.replaceChars(
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
        values.put(Stations.LAT, station.lat)
        values.put(Stations.LON, station.lon)
        values.put(Stations.DS100, station.shortCode)
        values.put(Stations.ACTIVE, !station.inactive)
        if (station.photos.isNotEmpty()) {
            val (id, photographer, path, _, license, outdated) = station.photos[0]
            values.put(Stations.PHOTO_ID, id)
            values.put(Stations.PHOTO_URL, photoStations.photoBaseUrl + path)
            values.put(Stations.PHOTOGRAPHER, photographer)
            values.put(Stations.OUTDATED, outdated)
            values.put(Stations.PHOTOGRAPHER_URL, photoStations.getPhotographerUrl(photographer))
            values.put(Stations.LICENSE, photoStations.getLicenseName(license))
            values.put(Stations.LICENSE_URL, photoStations.getLicenseUrl(license))
        }
        return values
    }

    fun insertCountries(countries: List<Country>) {
        if (countries.isEmpty()) {
            return
        }
        db.beginTransaction()
        try {
            deleteCountries()
            countries.forEach(Consumer { country: Country -> insertCountry(country) })
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertCountry(country: Country) {
        db.insert(DATABASE_TABLE_COUNTRIES, null, toContentValues(country))
        country.providerApps
            .map { p: ProviderApp -> toContentValues(country.code, p) }
            .forEach { values: ContentValues ->
                db.insert(
                    DATABASE_TABLE_PROVIDER_APPS,
                    null,
                    values
                )
            }
    }

    private fun toContentValues(countryCode: String, app: ProviderApp): ContentValues {
        val values = ContentValues()
        values.put(ProviderApps.COUNTRYSHORTCODE, countryCode)
        values.put(ProviderApps.PA_TYPE, app.type)
        values.put(ProviderApps.PA_NAME, app.name)
        values.put(ProviderApps.PA_URL, app.url)
        return values
    }

    private fun toContentValues(country: Country): ContentValues {
        val values = ContentValues()
        values.put(Countries.COUNTRYSHORTCODE, country.code)
        values.put(Countries.COUNTRYNAME, country.name)
        values.put(Countries.EMAIL, country.email)
        values.put(Countries.TIMETABLE_URL_TEMPLATE, country.timetableUrlTemplate)
        values.put(Countries.OVERRIDE_LICENSE, country.overrideLicense)
        return values
    }

    fun insertUpload(upload: Upload): Upload {
        upload.id = db.insert(DATABASE_TABLE_UPLOADS, null, toContentValues(upload))
        return upload
    }

    private fun deleteStations(countryCodes: Set<String>) {
        db.delete(DATABASE_TABLE_STATIONS, whereCountryCodeIn(countryCodes), null)
    }

    private fun deleteCountries() {
        db.delete(DATABASE_TABLE_PROVIDER_APPS, null, null)
        db.delete(DATABASE_TABLE_COUNTRIES, null, null)
    }

    private fun getStationOrderBy(sortByDistance: Boolean, myPos: Location?): String {
        var orderBy = Stations.TITLE + " ASC"
        if (sortByDistance) {
            val fudge = cos(
                Math.toRadians(
                    myPos!!.latitude
                )
            ).pow(2.0)
            orderBy =
                "((" + myPos.latitude + " - " + Stations.LAT + ") * (" + myPos.latitude + " - " + Stations.LAT + ") + " +
                        "(" + myPos.longitude + " - " + Stations.LON + ") * (" + myPos.longitude + " - " + Stations.LON + ") * " + fudge + ")"
        }
        return orderBy
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
            selectQuery += String.format(" AND %s LIKE ?", Stations.NORMALIZED_TITLE)
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
            selectQuery += " AND " + Stations.PHOTOGRAPHER + " = ?"
            queryArgs.add(stationFilter.nickname!!)
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + Stations.PHOTO_URL + " IS " + (if (stationFilter.hasPhoto()!!) "NOT" else "") + " NULL"
        }
        if (stationFilter.isActive != null) {
            selectQuery += " AND " + Stations.ACTIVE + " = ?"
            queryArgs.add(if (stationFilter.isActive!!) "1" else "0")
        }
        Log.w(TAG, selectQuery)
        val cursor = db.query(
            DATABASE_TABLE_STATIONS, arrayOf(
                Stations.ROWID + " AS " + Constants.CURSOR_ADAPTER_ID,
                Stations.ID,
                Stations.TITLE,
                Stations.PHOTO_URL,
                Stations.COUNTRY
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

    private fun whereCountryCodeIn(countryCodes: Set<String>): String {
        return Stations.COUNTRY +
                " IN (" +
                countryCodes.joinToString(",") { c: String -> "'$c'" } +
                ")"
    }

    fun getStatistic(country: String): Statistic? {
        db.rawQuery(
            "SELECT COUNT(*), COUNT(" + Stations.PHOTO_URL + "), COUNT(DISTINCT(" + Stations.PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Stations.COUNTRY + " = ?",
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
            db.rawQuery(
                "SELECT distinct " + Stations.PHOTOGRAPHER + " FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Stations.PHOTOGRAPHER + " IS NOT NULL ORDER BY " + Stations.PHOTOGRAPHER,
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    photographers.add(cursor.getString(0))
                }
            }
            return photographers.toTypedArray()
        }

    fun countStations(countryCodes: Set<String>): Int {
        db.rawQuery(
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

    fun updateUpload(upload: Upload) {
        db.beginTransaction()
        try {
            db.update(
                DATABASE_TABLE_UPLOADS, toContentValues(upload), Uploads.ID + " = ?", arrayOf(
                    upload.id.toString()
                )
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun toContentValues(upload: Upload): ContentValues {
        val values = ContentValues()
        values.put(Uploads.COMMENT, upload.comment)
        values.put(Uploads.COUNTRY, upload.country)
        values.put(Uploads.CREATED_AT, upload.createdAt)
        values.put(Uploads.INBOX_URL, upload.inboxUrl)
        values.put(Uploads.LAT, upload.lat)
        values.put(Uploads.LON, upload.lon)
        values.put(
            Uploads.PROBLEM_TYPE,
            if (upload.problemType != null) upload.problemType!!.name else null
        )
        values.put(Uploads.REJECTED_REASON, upload.rejectReason)
        values.put(Uploads.REMOTE_ID, upload.remoteId)
        values.put(Uploads.STATION_ID, upload.stationId)
        values.put(Uploads.TITLE, upload.title)
        values.put(Uploads.UPLOAD_STATE, upload.uploadState.name)
        values.put(Uploads.ACTIVE, upload.active)
        values.put(Uploads.CRC32, upload.crc32)
        values.put(Uploads.REMOTE_ID, upload.remoteId)
        return values
    }

    fun getPendingUploadsForStation(station: Station): List<Upload> {
        val uploads = mutableListOf<Upload>()
        db.query(
            DATABASE_TABLE_UPLOADS,
            null,
            Uploads.COUNTRY + " = ? AND " + Uploads.STATION_ID + " = ? AND " + pendingUploadWhereClause,
            arrayOf(station.country, station.id),
            null,
            null,
            Uploads.CREATED_AT + " DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor))
            }
        }
        return uploads
    }

    fun getPendingUploadForCoordinates(lat: Double, lon: Double): Upload? {
        db.query(
            DATABASE_TABLE_UPLOADS,
            null,
            Uploads.LAT + " = ? AND " + Uploads.LON + " = ? AND " + pendingUploadWhereClause,
            arrayOf(lat.toString(), lon.toString()),
            null,
            null,
            Uploads.CREATED_AT + " DESC"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor)
            }
        }
        return null
    }

    private fun getUploadWhereClause(predicate: Predicate<UploadState>): String {
        return Uploads.UPLOAD_STATE + " IN (" +
                UploadState.entries
                    .filter { predicate.test(it) }
                    .joinToString(",") { "'" + it.name + "'" } +
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
                    + DATABASE_TABLE_STATIONS + "." + Stations.COUNTRY
                    + " = "
                    + DATABASE_TABLE_UPLOADS + "." + Uploads.COUNTRY
                    + " AND "
                    + DATABASE_TABLE_STATIONS + "." + Stations.ID
                    + " = "
                    + DATABASE_TABLE_UPLOADS + "." + Uploads.STATION_ID)
            return queryBuilder.query(
                db, arrayOf(
                    DATABASE_TABLE_UPLOADS + "." + Uploads.ID + " AS " + Constants.CURSOR_ADAPTER_ID,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.REMOTE_ID,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.COUNTRY,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.STATION_ID,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.TITLE,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.UPLOAD_STATE,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.PROBLEM_TYPE,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.COMMENT,
                    DATABASE_TABLE_UPLOADS + "." + Uploads.REJECTED_REASON,
                    DATABASE_TABLE_STATIONS + "." + Uploads.TITLE + " AS " + Uploads.JOIN_STATION_TITLE
                ), null, null, null, null, Uploads.CREATED_AT + " DESC"
            )
        }

    fun getUploadById(id: Long): Upload? {
        db.query(
            DATABASE_TABLE_UPLOADS,
            null,
            Uploads.ID + "=?",
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
        db.delete(DATABASE_TABLE_UPLOADS, Uploads.ID + " = ?", arrayOf(id.toString()))
    }

    fun getPendingUploads(withRemoteId: Boolean): List<Upload> {
        var selection = pendingUploadWhereClause
        if (withRemoteId) {
            selection += " AND " + Uploads.REMOTE_ID + " IS NOT NULL"
        }
        val uploads = mutableListOf<Upload>()
        db.query(
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
        db.beginTransaction()
        try {
            stateQueries.forEach(Consumer { state: InboxStateQuery ->
                db.update(
                    DATABASE_TABLE_UPLOADS,
                    toUploadStatesContentValues(state),
                    Uploads.REMOTE_ID + " = ?",
                    arrayOf(state.id.toString())
                )
            })
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun toUploadStatesContentValues(state: InboxStateQuery): ContentValues {
        val values = ContentValues()
        values.put(Uploads.UPLOAD_STATE, state.state.name)
        values.put(Uploads.REJECTED_REASON, state.rejectedReason)
        values.put(Uploads.CRC32, state.crc32)
        return values
    }

    val completedUploads: List<Upload>
        get() {
            val uploads = ArrayList<Upload>()
            db.query(
                DATABASE_TABLE_UPLOADS, null,
                Uploads.REMOTE_ID + " IS NOT NULL AND " + completedUploadWhereClause,
                null, null, null, null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    uploads.add(createUploadFromCursor(cursor))
                }
            }
            return uploads
        }

    internal class DbOpenHelper(context: Context) :
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
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_COUNTRIES + " ADD COLUMN " + Countries.OVERRIDE_LICENSE + " TEXT")
                }
                if (oldVersion < 17) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_UPLOADS + " ADD COLUMN " + Uploads.ACTIVE + " INTEGER")
                }
                if (oldVersion < 18) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + Stations.NORMALIZED_TITLE + " TEXT")
                    db.execSQL("UPDATE " + DATABASE_TABLE_STATIONS + " SET " + Stations.NORMALIZED_TITLE + " = " + Stations.TITLE)
                }
                if (oldVersion < 19) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_UPLOADS + " ADD COLUMN " + Uploads.CRC32 + " INTEGER")
                }
                if (oldVersion < 20) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + Stations.OUTDATED + " INTEGER")
                }
                if (oldVersion < 21) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + Stations.PHOTO_ID + " INTEGER")
                }
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }
    }

    private fun createStationFromCursor(cursor: Cursor): Station {
        return Station(
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.COUNTRY)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.TITLE)),
            cursor.getDouble(cursor.getColumnIndexOrThrow(Stations.LAT)),
            cursor.getDouble(cursor.getColumnIndexOrThrow(Stations.LON)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.DS100)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.PHOTO_URL)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.PHOTOGRAPHER)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.PHOTOGRAPHER_URL)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.LICENSE)),
            cursor.getString(cursor.getColumnIndexOrThrow(Stations.LICENSE_URL)),
            java.lang.Boolean.TRUE == getBoolean(cursor, Stations.ACTIVE),
            java.lang.Boolean.TRUE == getBoolean(cursor, Stations.OUTDATED),
            cursor.getLong(cursor.getColumnIndexOrThrow(Stations.PHOTO_ID))
        )
    }

    private fun createCountryFromCursor(cursor: Cursor): Country {
        return Country(
            cursor.getString(cursor.getColumnIndexOrThrow(Countries.COUNTRYSHORTCODE)),
            cursor.getString(cursor.getColumnIndexOrThrow(Countries.COUNTRYNAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(Countries.EMAIL)),
            cursor.getString(cursor.getColumnIndexOrThrow(Countries.TIMETABLE_URL_TEMPLATE)),
            cursor.getString(cursor.getColumnIndexOrThrow(Countries.OVERRIDE_LICENSE))
        )
    }

    private fun createProviderAppFromCursor(cursor: Cursor): ProviderApp {
        return ProviderApp(
            cursor.getString(cursor.getColumnIndexOrThrow(ProviderApps.PA_TYPE)),
            cursor.getString(cursor.getColumnIndexOrThrow(ProviderApps.PA_NAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(ProviderApps.PA_URL))
        )
    }

    private fun createUploadFromCursor(cursor: Cursor): Upload {
        val upload = Upload(
            cursor.getLong(cursor.getColumnIndexOrThrow(Uploads.ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(Uploads.COUNTRY)),
            cursor.getString(cursor.getColumnIndexOrThrow(Uploads.STATION_ID)),
            getLong(cursor, Uploads.REMOTE_ID),
            cursor.getString(cursor.getColumnIndexOrThrow(Uploads.TITLE)),
            getDouble(cursor, Uploads.LAT),
            getDouble(cursor, Uploads.LON),
            cursor.getString(cursor.getColumnIndexOrThrow(Uploads.COMMENT)),
            cursor.getString(cursor.getColumnIndexOrThrow(Uploads.INBOX_URL)),
            null,
            cursor.getString(cursor.getColumnIndexOrThrow(Uploads.REJECTED_REASON)),
            UploadState.UNKNOWN,
            cursor.getLong(cursor.getColumnIndexOrThrow(Uploads.CREATED_AT)),
            getBoolean(cursor, Uploads.ACTIVE),
            getLong(cursor, Uploads.CRC32)
        )
        val problemType = cursor.getString(cursor.getColumnIndexOrThrow(Uploads.PROBLEM_TYPE))
        if (problemType != null) {
            upload.problemType = ProblemType.valueOf(problemType)
        }
        val uploadState = cursor.getString(cursor.getColumnIndexOrThrow(Uploads.UPLOAD_STATE))
        if (uploadState != null) {
            upload.uploadState = UploadState.valueOf(uploadState)
        }
        return upload
    }

    private fun getBoolean(cursor: Cursor, columnName: String): Boolean? {
        return if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            cursor.getInt(cursor.getColumnIndexOrThrow(columnName)) == 1
        } else null
    }

    private fun getDouble(cursor: Cursor, columnName: String): Double? {
        return if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            cursor.getDouble(cursor.getColumnIndexOrThrow(columnName))
        } else null
    }

    private fun getLong(cursor: Cursor, columnName: String): Long? {
        return if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            cursor.getLong(cursor.getColumnIndexOrThrow(columnName))
        } else null
    }

    fun fetchStationByRowId(id: Long): Station? {
        db.query(
            DATABASE_TABLE_STATIONS, null, Stations.ROWID + "=?", arrayOf(
                id.toString() + ""
            ), null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor)
            }
        }
        return null
    }

    fun getStationByKey(country: String, id: String): Station? {
        db.query(
            DATABASE_TABLE_STATIONS,
            null,
            Stations.COUNTRY + "=? AND " + Stations.ID + "=?",
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
        db.query(
            DATABASE_TABLE_STATIONS,
            null,
            Stations.COUNTRY + " = ? AND " + Stations.ID + " = ?",
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

    fun fetchCountriesWithProviderApps(countryCodes: Set<String>): Set<Country> {
        val countryList = countryCodes.joinToString(",") { c: String -> "'$c'" }
        val countries = HashSet<Country>()
        db.query(
            DATABASE_TABLE_COUNTRIES,
            null,
            Countries.COUNTRYSHORTCODE + " IN (" + countryList + ")",
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    var country = createCountryFromCursor(cursor)
                    val providerApps = mutableListOf<ProviderApp>()
                    db.query(
                        DATABASE_TABLE_PROVIDER_APPS,
                        null,
                        ProviderApps.COUNTRYSHORTCODE + " = ?",
                        arrayOf(country.code),
                        null,
                        null,
                        null
                    ).use { cursorPa ->
                        if (cursorPa != null && cursorPa.moveToFirst()) {
                            do {
                                providerApps.add(createProviderAppFromCursor(cursorPa))
                            } while (cursorPa.moveToNext())
                        }
                    }
                    country = country.copy(providerApps = providerApps)
                    countries.add(country)
                } while (cursor.moveToNext())
            }
        }
        return countries
    }

    fun getAllStations(stationFilter: StationFilter, countryCodes: Set<String>): List<Station> {
        val stationList = ArrayList<Station>()
        var selectQuery =
            "SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + whereCountryCodeIn(countryCodes)
        val queryArgs = ArrayList<String>()
        stationFilter.nickname?.let {
            selectQuery += " AND " + Stations.PHOTOGRAPHER + " = ?"
            queryArgs.add(it)
        }
        stationFilter.hasPhoto()?.let {
            selectQuery += " AND " + Stations.PHOTO_URL + " IS " + (if (it) "NOT" else "") + " NULL"
        }
        stationFilter.isActive?.let {
            selectQuery += " AND " + Stations.ACTIVE + " = ?"
            queryArgs.add(if (it) "1" else "0")
        }
        db.rawQuery(selectQuery, queryArgs.toArray(arrayOf())).use { cursor ->
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
            ("SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Stations.LAT + " < " + (lat + 0.5) + " AND " + Stations.LAT + " > " + (lat - 0.5)
                    + " AND " + Stations.LON + " < " + (lng + 0.5) + " AND " + Stations.LON + " > " + (lng - 0.5))
        val queryArgs = ArrayList<String>()
        stationFilter.nickname?.let {
            selectQuery += " AND " + Stations.PHOTOGRAPHER + " = ?"
            queryArgs.add(it)
        }
        stationFilter.hasPhoto()?.let {
            selectQuery += " AND " + Stations.PHOTO_URL + " IS " + (if (it) "NOT" else "") + " NULL"
        }
        stationFilter.isActive?.let {
            selectQuery += " AND " + Stations.ACTIVE + " = ?"
            queryArgs.add(if (it) "1" else "0")
        }
        db.rawQuery(selectQuery, queryArgs.toArray(arrayOf())).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        }
        return stationList
    }

    val allCountries: List<Country>
        get() {
            val countryList = ArrayList<Country>()
            val query =
                "SELECT * FROM $DATABASE_TABLE_COUNTRIES ORDER BY ${Countries.COUNTRYSHORTCODE}"
            Log.d(TAG, query)
            db.rawQuery(query, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        countryList.add(createCountryFromCursor(cursor))
                    } while (cursor.moveToNext())
                }
            }
            return countryList
        }
}

private val TAG = DbAdapter::class.java.simpleName
private const val DATABASE_TABLE_STATIONS = "bahnhoefe"
private const val DATABASE_TABLE_COUNTRIES = "laender"
private const val DATABASE_TABLE_PROVIDER_APPS = "providerApps"
private const val DATABASE_TABLE_UPLOADS = "uploads"
private const val DATABASE_NAME = "bahnhoefe.db"
private const val DATABASE_VERSION = 22
private const val CREATE_STATEMENT_STATIONS =
    """
        CREATE TABLE $DATABASE_TABLE_STATIONS (
            ${Stations.ROWID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Stations.COUNTRY} TEXT, 
            ${Stations.ID} TEXT, 
            ${Stations.TITLE} TEXT, 
            ${Stations.NORMALIZED_TITLE} TEXT, 
            ${Stations.LAT} REAL, 
            ${Stations.LON} REAL, 
            ${Stations.PHOTO_ID} INTEGER, 
            ${Stations.PHOTO_URL} TEXT, 
            ${Stations.PHOTOGRAPHER} TEXT, 
            ${Stations.PHOTOGRAPHER_URL} TEXT, 
            ${Stations.LICENSE} TEXT, 
            ${Stations.LICENSE_URL} TEXT, 
            ${Stations.DS100} TEXT, 
            ${Stations.ACTIVE} INTEGER, 
            ${Stations.OUTDATED} INTEGER
        )
    """
private const val CREATE_STATEMENT_STATIONS_IDX =
    "CREATE INDEX ${DATABASE_TABLE_STATIONS}_IDX ON $DATABASE_TABLE_STATIONS(${Stations.COUNTRY}, ${Stations.ID})"
private const val CREATE_STATEMENT_COUNTRIES =
    """
        CREATE TABLE $DATABASE_TABLE_COUNTRIES (
            ${Countries.ROWID_COUNTRIES} INTEGER PRIMARY KEY AUTOINCREMENT, 
            ${Countries.COUNTRYSHORTCODE} TEXT, 
            ${Countries.COUNTRYNAME} TEXT, 
            ${Countries.EMAIL} TEXT, 
            ${Countries.TIMETABLE_URL_TEMPLATE} TEXT, 
            ${Countries.OVERRIDE_LICENSE} TEXT
        )
    """
private const val CREATE_STATEMENT_PROVIDER_APPS =
    """
        CREATE TABLE $DATABASE_TABLE_PROVIDER_APPS (
            ${ProviderApps.COUNTRYSHORTCODE} TEXT,
            ${ProviderApps.PA_TYPE} TEXT,
            ${ProviderApps.PA_NAME} TEXT, 
            ${ProviderApps.PA_URL} TEXT
        )
    """
private const val CREATE_STATEMENT_UPLOADS =
    """
        CREATE TABLE $DATABASE_TABLE_UPLOADS (
            ${Uploads.ID} INTEGER PRIMARY KEY AUTOINCREMENT, 
            ${Uploads.STATION_ID} TEXT, 
            ${Uploads.COUNTRY} TEXT, 
            ${Uploads.REMOTE_ID} INTEGER, 
            ${Uploads.TITLE} TEXT, 
            ${Uploads.LAT} REAL, 
            ${Uploads.LON} REAL, 
            ${Uploads.COMMENT} TEXT, 
            ${Uploads.INBOX_URL} TEXT, 
            ${Uploads.PROBLEM_TYPE} TEXT, 
            ${Uploads.REJECTED_REASON} TEXT, 
            ${Uploads.UPLOAD_STATE} TEXT, 
            ${Uploads.CREATED_AT} INTEGER, 
            ${Uploads.ACTIVE} INTEGER, 
            ${Uploads.CRC32} INTEGER
        )
    """
private const val DROP_STATEMENT_STATIONS_IDX =
    "DROP INDEX IF EXISTS ${DATABASE_TABLE_STATIONS}_IDX"
private const val DROP_STATEMENT_STATIONS =
    "DROP TABLE IF EXISTS $DATABASE_TABLE_STATIONS"
private const val DROP_STATEMENT_COUNTRIES =
    "DROP TABLE IF EXISTS $DATABASE_TABLE_COUNTRIES"
private const val DROP_STATEMENT_PROVIDER_APPS =
    "DROP TABLE IF EXISTS $DATABASE_TABLE_PROVIDER_APPS"
