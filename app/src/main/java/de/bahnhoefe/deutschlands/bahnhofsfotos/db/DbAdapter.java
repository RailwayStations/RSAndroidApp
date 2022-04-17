package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import static java.util.stream.Collectors.joining;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxStateQuery;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;

public class DbAdapter {

    private static final String TAG = DbAdapter.class.getSimpleName();

    private static final String DATABASE_TABLE_STATIONS = "bahnhoefe";
    private static final String DATABASE_TABLE_COUNTRIES = "laender";
    private static final String DATABASE_TABLE_PROVIDER_APPS = "providerApps";
    private static final String DATABASE_TABLE_UPLOADS = "uploads";
    private static final String DATABASE_NAME = "bahnhoefe.db";
    private static final int DATABASE_VERSION = 20;

    private static final String CREATE_STATEMENT_STATIONS = "CREATE TABLE " + DATABASE_TABLE_STATIONS + " ("
            + Constants.STATIONS.ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Constants.STATIONS.COUNTRY + " TEXT, "
            + Constants.STATIONS.ID + " TEXT, "
            + Constants.STATIONS.TITLE + " TEXT, "
            + Constants.STATIONS.NORMALIZED_TITLE + " TEXT, "
            + Constants.STATIONS.LAT + " REAL, "
            + Constants.STATIONS.LON + " REAL, "
            + Constants.STATIONS.PHOTO_URL + " TEXT, "
            + Constants.STATIONS.PHOTOGRAPHER + " TEXT, "
            + Constants.STATIONS.PHOTOGRAPHER_URL + " TEXT, "
            + Constants.STATIONS.LICENSE + " TEXT, "
            + Constants.STATIONS.LICENSE_URL + " TEXT, "
            + Constants.STATIONS.DS100 + " TEXT, "
            + Constants.STATIONS.ACTIVE + " INTEGER, "
            + Constants.STATIONS.OUTDATED + " INTEGER)";
    private static final String CREATE_STATEMENT_STATIONS_IDX = "CREATE INDEX " + DATABASE_TABLE_STATIONS + "_IDX "
            + "ON " + DATABASE_TABLE_STATIONS + "(" + Constants.STATIONS.COUNTRY + ", " + Constants.STATIONS.ID + ")";
    private static final String CREATE_STATEMENT_COUNTRIES = "CREATE TABLE " + DATABASE_TABLE_COUNTRIES + " ("
            + Constants.COUNTRIES.ROWID_COUNTRIES + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Constants.COUNTRIES.COUNTRYSHORTCODE + " TEXT, "
            + Constants.COUNTRIES.COUNTRYNAME + " TEXT, "
            + Constants.COUNTRIES.EMAIL + " TEXT, "
            + Constants.COUNTRIES.TWITTERTAGS + " TEXT, "
            + Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE + " TEXT, "
            + Constants.COUNTRIES.OVERRIDE_LICENSE + " TEXT)";
    private static final String CREATE_STATEMENT_PROVIDER_APPS = "CREATE TABLE " + DATABASE_TABLE_PROVIDER_APPS + " ("
            + Constants.PROVIDER_APPS.COUNTRYSHORTCODE + " TEXT,"
            + Constants.PROVIDER_APPS.PA_TYPE + " TEXT,"
            + Constants.PROVIDER_APPS.PA_NAME + " TEXT, "
            + Constants.PROVIDER_APPS.PA_URL + " TEXT)";
    private static final String CREATE_STATEMENT_UPLOADS = "CREATE TABLE " + DATABASE_TABLE_UPLOADS + " ("
            + Constants.UPLOADS.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Constants.UPLOADS.STATION_ID + " TEXT, "
            + Constants.UPLOADS.COUNTRY + " TEXT, "
            + Constants.UPLOADS.REMOTE_ID + " INTEGER, "
            + Constants.UPLOADS.TITLE + " TEXT, "
            + Constants.UPLOADS.LAT + " REAL, "
            + Constants.UPLOADS.LON + " REAL, "
            + Constants.UPLOADS.COMMENT + " TEXT, "
            + Constants.UPLOADS.INBOX_URL + " TEXT, "
            + Constants.UPLOADS.PROBLEM_TYPE + " TEXT, "
            + Constants.UPLOADS.REJECTED_REASON + " TEXT, "
            + Constants.UPLOADS.UPLOAD_STATE + " TEXT, "
            + Constants.UPLOADS.CREATED_AT + " INTEGER, "
            + Constants.UPLOADS.ACTIVE + " INTEGER, "
            + Constants.UPLOADS.CRC32 + " INTEGER)";

    private static final String DROP_STATEMENT_STATIONS_IDX = "DROP INDEX IF EXISTS " + DATABASE_TABLE_STATIONS + "_IDX";
    private static final String DROP_STATEMENT_STATIONS = "DROP TABLE IF EXISTS " + DATABASE_TABLE_STATIONS;
    private static final String DROP_STATEMENT_COUNTRIES = "DROP TABLE IF EXISTS " + DATABASE_TABLE_COUNTRIES;
    private static final String DROP_STATEMENT_PROVIDER_APPS = "DROP TABLE IF EXISTS " + DATABASE_TABLE_PROVIDER_APPS;

    private final Context context;
    private DbOpenHelper dbHelper;
    private SQLiteDatabase db;

    public DbAdapter(final Context context) {
        this.context = context;
    }

    public void open() {
        dbHelper = new DbOpenHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        db.close();
        dbHelper.close();
    }

    public void insertStations(final List<Station> stations, final Set<String> countryCodes) {
        if (stations.isEmpty()) {
            return;
        }

        db.beginTransaction();
        try {
            deleteStations(countryCodes);
            stations.forEach(station -> db.insert(DATABASE_TABLE_STATIONS, null, toContentValues(station)));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(final Station station) {
        final var values = new ContentValues();
        values.put(Constants.STATIONS.ID, station.getId());
        values.put(Constants.STATIONS.COUNTRY, station.getCountry());
        values.put(Constants.STATIONS.TITLE, station.getTitle());
        values.put(Constants.STATIONS.NORMALIZED_TITLE, StringUtils.replaceChars(StringUtils.deleteWhitespace(StringUtils.stripAccents(Normalizer.normalize(station.getTitle(), Normalizer.Form.NFC))), "-_()", null));
        values.put(Constants.STATIONS.LAT, station.getLat());
        values.put(Constants.STATIONS.LON, station.getLon());
        values.put(Constants.STATIONS.PHOTO_URL, station.getPhotoUrl());
        values.put(Constants.STATIONS.PHOTOGRAPHER, station.getPhotographer());
        values.put(Constants.STATIONS.PHOTOGRAPHER_URL, station.getPhotographerUrl());
        values.put(Constants.STATIONS.LICENSE, station.getLicense());
        values.put(Constants.STATIONS.LICENSE_URL, station.getLicenseUrl());
        values.put(Constants.STATIONS.DS100, station.getDs100());
        values.put(Constants.STATIONS.ACTIVE, station.isActive());
        values.put(Constants.STATIONS.OUTDATED, station.isOutdated());
        return values;
    }

    public void insertCountries(final List<Country> countries) {
        if (countries.isEmpty()) {
            return;
        }
        db.beginTransaction();
        try {
            deleteCountries();

            countries.stream()
                    .map(this::toContentValues)
                    .forEach(values -> db.insert(DATABASE_TABLE_COUNTRIES, null, values));

            countries.stream()
                    .flatMap(c -> c.getProviderApps().stream())
                    .map(this::toContentValues)
                    .forEach(values -> db.insert(DATABASE_TABLE_PROVIDER_APPS, null, values));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(final ProviderApp app) {
        final var values = new ContentValues();
        values.put(Constants.PROVIDER_APPS.COUNTRYSHORTCODE, app.getCountryCode());
        values.put(Constants.PROVIDER_APPS.PA_TYPE, app.getType());
        values.put(Constants.PROVIDER_APPS.PA_NAME, app.getName());
        values.put(Constants.PROVIDER_APPS.PA_URL, app.getUrl());
        return values;
    }

    private ContentValues toContentValues(final Country country) {
        final var values = new ContentValues();
        values.put(Constants.COUNTRIES.COUNTRYSHORTCODE, country.getCode());
        values.put(Constants.COUNTRIES.COUNTRYNAME, country.getName());
        values.put(Constants.COUNTRIES.EMAIL, country.getEmail());
        values.put(Constants.COUNTRIES.TWITTERTAGS, country.getTwitterTags());
        values.put(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE, country.getTimetableUrlTemplate());
        values.put(Constants.COUNTRIES.OVERRIDE_LICENSE, country.getOverrideLicense());
        return values;
    }

    public Upload insertUpload(final Upload upload) {
        upload.setId(db.insert(DATABASE_TABLE_UPLOADS, null, toContentValues(upload)));
        return upload;
    }

    public void deleteStations(final Set<String> countryCodes) {
        db.delete(DATABASE_TABLE_STATIONS, whereCountryCodeIn(countryCodes), null);
    }

    public void deleteCountries() {
        db.delete(DATABASE_TABLE_PROVIDER_APPS, null, null);
        db.delete(DATABASE_TABLE_COUNTRIES, null, null);
    }

    private String getStationOrderBy(final boolean sortByDistance, final Location myPos) {
        var orderBy = Constants.STATIONS.TITLE + " ASC";

        if (sortByDistance) {
            final var fudge = Math.pow(Math.cos(Math.toRadians(myPos.getLatitude())), 2);
            orderBy = "((" + myPos.getLatitude() + " - " + Constants.STATIONS.LAT + ") * (" + myPos.getLatitude() + " - " + Constants.STATIONS.LAT + ") + " +
                    "(" + myPos.getLongitude() + " - " + Constants.STATIONS.LON + ") * (" + myPos.getLongitude() + " - " + Constants.STATIONS.LON + ") * " + fudge + ")";
        }

        return orderBy;
    }

    public Cursor getCountryList() {
        final var selectCountries = "SELECT " + Constants.COUNTRIES.ROWID_COUNTRIES + " AS " + Constants.CURSOR_ADAPTER_ID + ", " +
                Constants.COUNTRIES.COUNTRYSHORTCODE + ", " + Constants.COUNTRIES.COUNTRYNAME +
                " FROM " + DATABASE_TABLE_COUNTRIES + " ORDER BY " + Constants.COUNTRIES.COUNTRYNAME + " ASC";
        Log.d(TAG, selectCountries);

        final var cursor = db.rawQuery(selectCountries, null);

        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }


    /**
     * Return a cursor on station ids where the station's title matches the given string
     *
     * @param search the search keyword
     * @param stationFilter if stations need to be filtered by photo available or not
     * @param countryCodes countries to search for
     * @param sortByDistance sort by distance or by alphabet
     * @param myPos current location
     * @return a Cursor representing the matching results
     */
    public Cursor getStationsListByKeyword(final String search, final StationFilter stationFilter, final Set<String> countryCodes, final boolean sortByDistance, final Location myPos) {
        var selectQuery = whereCountryCodeIn(countryCodes);
        final var queryArgs = new ArrayList<String>();

        if (StringUtils.isNotBlank(search)) {
            selectQuery += String.format(" AND %s LIKE ?", Constants.STATIONS.NORMALIZED_TITLE);
            queryArgs.add("%" + StringUtils.replaceChars(StringUtils.stripAccents(StringUtils.trimToEmpty(search)), " -_()", "%%%%%") + "%");
        }

        if (stationFilter.getNickname() != null) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
            queryArgs.add(stationFilter.getNickname());
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (stationFilter.hasPhoto() ? "NOT" : "") + " NULL";
        }
        if (stationFilter.isActive() != null) {
            selectQuery += " AND " + Constants.STATIONS.ACTIVE + " = ?";
            queryArgs.add(stationFilter.isActive() ? "1" : "0");
        }

        Log.w(TAG, selectQuery);

        final var cursor = db.query(DATABASE_TABLE_STATIONS,
            new String[]{
                Constants.STATIONS.ROWID + " AS " + Constants.CURSOR_ADAPTER_ID,
                Constants.STATIONS.ID,
                Constants.STATIONS.TITLE,
                Constants.STATIONS.PHOTO_URL,
                Constants.STATIONS.COUNTRY
            },
            selectQuery,
            queryArgs.toArray(new String[0]), null, null, getStationOrderBy(sortByDistance, myPos));

        if (!cursor.moveToFirst()) {
            Log.w(TAG, String.format("Query '%s' returned no result", search));
            cursor.close();
            return null;
        }
        return cursor;
    }

    private String whereCountryCodeIn(final Set<String> countryCodes) {
        return Constants.STATIONS.COUNTRY +
                " IN (" +
                countryCodes.stream().map(c -> "'" + c + "'").collect(joining(",")) +
                ")";
    }

    public Statistic getStatistic(final String country) {
        try (final var cursor = db.rawQuery("SELECT COUNT(*), COUNT(" + Constants.STATIONS.PHOTO_URL + "), COUNT(DISTINCT(" + Constants.STATIONS.PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.COUNTRY + " = ?", new String[]{country})) {
            if (cursor.moveToNext()) {
                return new Statistic(cursor.getInt(0), cursor.getInt(1), cursor.getInt(0) - cursor.getInt(1), cursor.getInt(2));
            }
        }
        return null;
    }

    public String[] getPhotographerNicknames() {
        final var photographers = new ArrayList<String>();
        try (final var cursor = db.rawQuery("SELECT distinct " + Constants.STATIONS.PHOTOGRAPHER + " FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " IS NOT NULL ORDER BY " + Constants.STATIONS.PHOTOGRAPHER, null)) {
            while (cursor.moveToNext()) {
                photographers.add(cursor.getString(0));
            }
        }
        return photographers.toArray(new String[0]);
    }

    public int countStations(final Set<String> countryCodes) {
        try (final var query = db.rawQuery("SELECT COUNT(*) FROM " + DATABASE_TABLE_STATIONS + " WHERE " + whereCountryCodeIn(countryCodes), null)) {
            if (query.moveToFirst()) {
                return query.getInt(0);
            }
        }
        return 0;
    }

    public void updateUpload(final Upload upload) {
        db.beginTransaction();
        try {
            db.update(DATABASE_TABLE_UPLOADS, toContentValues(upload), Constants.UPLOADS.ID + " = ?", new String[]{String.valueOf(upload.getId())});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(final Upload upload) {
        final var values = new ContentValues();
        values.put(Constants.UPLOADS.COMMENT, upload.getComment());
        values.put(Constants.UPLOADS.COUNTRY, upload.getCountry());
        values.put(Constants.UPLOADS.CREATED_AT, upload.getCreatedAt());
        values.put(Constants.UPLOADS.INBOX_URL, upload.getInboxUrl());
        values.put(Constants.UPLOADS.LAT, upload.getLat());
        values.put(Constants.UPLOADS.LON, upload.getLon());
        values.put(Constants.UPLOADS.PROBLEM_TYPE, upload.getProblemType() != null ? upload.getProblemType().name() : null);
        values.put(Constants.UPLOADS.REJECTED_REASON, upload.getRejectReason());
        values.put(Constants.UPLOADS.REMOTE_ID, upload.getRemoteId());
        values.put(Constants.UPLOADS.STATION_ID, upload.getStationId());
        values.put(Constants.UPLOADS.TITLE, upload.getTitle());
        values.put(Constants.UPLOADS.UPLOAD_STATE, upload.getUploadState().name());
        values.put(Constants.UPLOADS.ACTIVE, upload.getActive());
        values.put(Constants.UPLOADS.CRC32, upload.getCrc32());
        values.put(Constants.UPLOADS.REMOTE_ID, upload.getRemoteId());
        return values;
    }

    public Upload getPendingUploadForStation(final Station station) {
        try (final var cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.COUNTRY + "=? AND " + Constants.UPLOADS.STATION_ID + "=? AND " + getPendingUploadWhereClause(),
                new String[]{ station.getCountry(), station.getId()}, null, null, Constants.UPLOADS.CREATED_AT + " DESC")) {
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor);
            }
        }

        return null;
    }

    public Upload getPendingUploadForCoordinates(final double lat, final double lon) {
        try (final var cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.LAT + "=? AND " + Constants.UPLOADS.LON + "=? AND " + getPendingUploadWhereClause(),
                new String[]{ String.valueOf(lat), String.valueOf(lon)}, null, null, Constants.UPLOADS.CREATED_AT + " DESC")) {
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor);
            }
        }

        return null;
    }

    private String getUploadWhereClause(final Predicate<UploadState> predicate) {
        return Constants.UPLOADS.UPLOAD_STATE + " IN (" +
                Arrays.stream(UploadState.values())
                        .filter(predicate)
                        .map(s -> "'" + s.name() + "'")
                        .collect(joining(",")) +
                ')';
    }

    private String getPendingUploadWhereClause() {
        return getUploadWhereClause(UploadState::isPending);
    }

    private String getCompletedUploadWhereClause() {
        return getUploadWhereClause(s -> !s.isPending());
    }

    public Cursor getOutbox() {
        final var queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DATABASE_TABLE_UPLOADS
                        + " LEFT JOIN "
                        + DATABASE_TABLE_STATIONS
                        + " ON "
                        + DATABASE_TABLE_STATIONS + "." + Constants.STATIONS.COUNTRY
                        + " = "
                        + DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.COUNTRY
                        + " AND "
                        + DATABASE_TABLE_STATIONS + "." + Constants.STATIONS.ID
                        + " = "
                        + DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.STATION_ID);
        return queryBuilder.query(db, new String[]{
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.ID + " AS " + Constants.CURSOR_ADAPTER_ID,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.REMOTE_ID,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.COUNTRY,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.STATION_ID,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.TITLE,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.UPLOAD_STATE,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.PROBLEM_TYPE,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.COMMENT,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.REJECTED_REASON,
                DATABASE_TABLE_STATIONS + "." + Constants.UPLOADS.TITLE + " AS " + Constants.UPLOADS.JOIN_STATION_TITLE
        }, null, null, null, null, Constants.UPLOADS.CREATED_AT + " DESC");
    }

    public Upload getUploadById(final long id) {
        try (final var cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.ID + "=?",
                new String[]{ String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor);
            }
        }

        return null;
    }

    public void deleteUpload(final long id) {
        db.delete(DATABASE_TABLE_UPLOADS, Constants.UPLOADS.ID + "=?", new String[]{ String.valueOf(id)});
    }

    public List<Upload> getPendingUploads(final boolean withRemoteId) {
        var selection = getPendingUploadWhereClause();
        if (withRemoteId) {
            selection += " AND " + Constants.UPLOADS.REMOTE_ID + " IS NOT NULL";
        }
        final var uploads = new ArrayList<Upload>();
        try (final var cursor = db.query(DATABASE_TABLE_UPLOADS, null, selection,
                null,null, null, null)) {
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor));
            }
        }

        return uploads;
    }

    public void updateUploadStates(final List<InboxStateQuery> stateQueries) {
        db.beginTransaction();
        try {
            stateQueries.forEach(state -> db.update(DATABASE_TABLE_UPLOADS, toUploadStatesContentValues(state), Constants.UPLOADS.REMOTE_ID + " = ?", new String[]{String.valueOf(state.getId())}));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toUploadStatesContentValues(final InboxStateQuery state) {
        final var values = new ContentValues();
        values.put(Constants.UPLOADS.UPLOAD_STATE, state.getState().name());
        values.put(Constants.UPLOADS.REJECTED_REASON, state.getRejectedReason());
        values.put(Constants.UPLOADS.CRC32, state.getCrc32());
        return values;
    }

    public List<Upload> getCompletedUploads() {
        final var uploads = new ArrayList<Upload>();
        try (final var cursor = db.query(DATABASE_TABLE_UPLOADS, null,
                Constants.UPLOADS.REMOTE_ID + " IS NOT NULL AND " + getCompletedUploadWhereClause(),
                null,null, null, null)) {
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor));
            }
        }

        return uploads;
    }

    static class DbOpenHelper extends SQLiteOpenHelper {

        DbOpenHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "Creating database");
            db.execSQL(CREATE_STATEMENT_STATIONS);
            db.execSQL(CREATE_STATEMENT_STATIONS_IDX);
            db.execSQL(CREATE_STATEMENT_COUNTRIES);
            db.execSQL(CREATE_STATEMENT_PROVIDER_APPS);
            db.execSQL(CREATE_STATEMENT_UPLOADS);
            Log.i(TAG, "Database structure created.");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.w(TAG, "Upgrade database from version" + oldVersion + " to " + newVersion);

            db.beginTransaction();

            if (oldVersion < 13) {
                // up to version 13 we dropped all tables and recreated them
                db.execSQL(DROP_STATEMENT_STATIONS_IDX);
                db.execSQL(DROP_STATEMENT_STATIONS);
                db.execSQL(DROP_STATEMENT_COUNTRIES);
                db.execSQL(DROP_STATEMENT_PROVIDER_APPS);
                onCreate(db);
            } else {
                // from now on we need to preserve user data and perform schema changes selectively

                if (oldVersion < 14) {
                    db.execSQL(CREATE_STATEMENT_UPLOADS);
                }

                if (oldVersion < 15) {
                    db.execSQL(DROP_STATEMENT_STATIONS_IDX);
                    db.execSQL(CREATE_STATEMENT_STATIONS_IDX);
                }

                if (oldVersion < 16) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_COUNTRIES + " ADD COLUMN " + Constants.COUNTRIES.OVERRIDE_LICENSE + " TEXT");
                }

                if (oldVersion < 17) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_UPLOADS + " ADD COLUMN " + Constants.UPLOADS.ACTIVE + " INTEGER");
                }

                if (oldVersion < 18) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + Constants.STATIONS.NORMALIZED_TITLE + " TEXT");
                    db.execSQL("UPDATE " + DATABASE_TABLE_STATIONS + " SET " + Constants.STATIONS.NORMALIZED_TITLE + " = " + Constants.STATIONS.TITLE);
                }

                if (oldVersion < 19) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_UPLOADS + " ADD COLUMN " + Constants.UPLOADS.CRC32 + " INTEGER");
                }

                if (oldVersion < 20) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + Constants.STATIONS.OUTDATED + " INTEGER");
                }
            }

            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    @NonNull
    private Station createStationFromCursor(@NonNull final Cursor cursor) {
        final var station = new Station();
        station.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.TITLE)));
        station.setCountry(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.COUNTRY)));
        station.setId(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.ID)));
        station.setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.STATIONS.LAT)));
        station.setLon(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.STATIONS.LON)));
        station.setPhotoUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTO_URL)));
        station.setPhotographer(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTOGRAPHER)));
        station.setPhotographerUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTOGRAPHER_URL)));
        station.setLicense(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.LICENSE)));
        station.setLicenseUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.LICENSE_URL)));
        station.setDs100(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.DS100)));
        station.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.STATIONS.ACTIVE)) == 1);
        station.setOutdated(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.STATIONS.OUTDATED)) == 1);
        return station;
    }

    @NonNull
    private Country createCountryFromCursor(@NonNull final Cursor cursor) {
        final var country = new Country();
        country.setCode(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE)));
        country.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYNAME)));
        country.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.EMAIL)));
        country.setTwitterTags(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TWITTERTAGS)));
        country.setTimetableUrlTemplate(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE)));
        country.setOverrideLicense(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.OVERRIDE_LICENSE)));
        return country;
    }

    @NonNull
    private ProviderApp createProviderAppFromCursor(@NonNull final Cursor cursor) {
        final var providerApp = new ProviderApp();
        providerApp.setCountryCode(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.COUNTRYSHORTCODE)));
        providerApp.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_NAME)));
        providerApp.setType(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_TYPE)));
        providerApp.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_URL)));
        return providerApp;
    }

    @NonNull
    private Upload createUploadFromCursor(@NonNull final Cursor cursor) {
        final var upload = new Upload();
        upload.setComment(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COMMENT)));
        upload.setCountry(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COUNTRY)));
        upload.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.CREATED_AT)));
        upload.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.ID)));
        upload.setInboxUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.INBOX_URL)));
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(Constants.UPLOADS.LAT))) {
            upload.setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.UPLOADS.LAT)));
        }
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(Constants.UPLOADS.LON))) {
            upload.setLon(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.UPLOADS.LON)));
        }
        final var problemType = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.PROBLEM_TYPE));
        if (problemType != null) {
            upload.setProblemType(ProblemType.valueOf(problemType));
        }
        upload.setRejectReason(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REJECTED_REASON)));
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REMOTE_ID))) {
            upload.setRemoteId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REMOTE_ID)));
        }
        upload.setStationId(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.STATION_ID)));
        upload.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.TITLE)));
        final var uploadState = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.UPLOAD_STATE));
        if (uploadState != null) {
            upload.setUploadState(UploadState.valueOf(uploadState));
        }
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(Constants.UPLOADS.ACTIVE))) {
            upload.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.UPLOADS.ACTIVE)) == 1);
        }
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(Constants.UPLOADS.CRC32))) {
            upload.setCrc32(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.CRC32)));
        }
        return upload;
    }

    public Station fetchStationByRowId(final long id) {
        try (final var cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.ROWID + "=?", new String[]{
                id + ""}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor);
            }
        }
        return null;
    }

    public Station getStationByKey(final String country, final String id) {
        try (final var cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? AND " + Constants.STATIONS.ID + "=?",
                new String[]{ country, id}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor);
            }
        }
        return null;
    }

    public Station getStationForUpload(final Upload upload) {
        try (final var cursor =  db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? AND " + Constants.STATIONS.ID + "=?",
                    new String[]{upload.getCountry(), upload.getStationId()}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor);
            }
        }
        return null;
    }

    public Set<Country> fetchCountriesWithProviderApps(final Set<String> countryCodes) {
        final var countryList = countryCodes.stream()
                .map(c -> "'" + c + "'")
                .collect(joining(","));
        final var countries = new HashSet<Country>();
        try (final var cursor = db.query(DATABASE_TABLE_COUNTRIES, null, Constants.COUNTRIES.COUNTRYSHORTCODE + " IN (" + countryList + ")",
                null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    final var country = createCountryFromCursor(cursor);
                    countries.add(country);
                    try (final var cursorPa = db.query(DATABASE_TABLE_PROVIDER_APPS, null, Constants.PROVIDER_APPS.COUNTRYSHORTCODE + " = ?",
                            new String[]{country.getCode()}, null, null, null)) {
                        if (cursorPa != null && cursorPa.moveToFirst()) {
                            do {
                                country.getProviderApps().add(createProviderAppFromCursor(cursorPa));
                            } while (cursorPa.moveToNext());
                        }
                    }
                } while (cursor.moveToNext());
            }
        }

        return countries;
    }

    public List<Station> getAllStations(final StationFilter stationFilter, final Set<String> countryCodes) {
        final var stationList = new ArrayList<Station>();
        var selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + whereCountryCodeIn(countryCodes);
        final var queryArgs = new ArrayList<String>();
        if (stationFilter.getNickname() != null) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
            queryArgs.add(stationFilter.getNickname());
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (stationFilter.hasPhoto() ? "NOT" : "") + " NULL";
        }
        if (stationFilter.isActive() != null) {
            selectQuery += " AND " + Constants.STATIONS.ACTIVE + " = ?";
            queryArgs.add(stationFilter.isActive() ? "1" : "0");
        }

        try (final var cursor = db.rawQuery(selectQuery, queryArgs.toArray(new String[]{}))) {
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return stationList;
    }

    public List<Station> getStationByLatLngRectangle(final double lat, final double lng, final StationFilter stationFilter) {
        final var stationList = new ArrayList<Station>();
        // Select All Query with rectangle - might be later change with it
        var selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.LAT + " < " + (lat + 0.5) + " AND " + Constants.STATIONS.LAT + " > " + (lat - 0.5)
                + " AND " + Constants.STATIONS.LON + " < " + (lng + 0.5) + " AND " + Constants.STATIONS.LON + " > " + (lng - 0.5);

        final var queryArgs = new ArrayList<String>();
        if (stationFilter.getNickname() != null) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
            queryArgs.add(stationFilter.getNickname());
        }
        if (stationFilter.hasPhoto() != null) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (stationFilter.hasPhoto() ? "NOT" : "") + " NULL";
        }
        if (stationFilter.isActive() != null) {
            selectQuery += " AND " + Constants.STATIONS.ACTIVE + " = ?";
            queryArgs.add(stationFilter.isActive() ? "1" : "0");
        }

        try (final var cursor = db.rawQuery(selectQuery, queryArgs.toArray(new String[]{}))) {
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return stationList;
    }

    public List<Country> getAllCountries() {
        final var countryList = new ArrayList<Country>();
        final var query = "SELECT * FROM " + DATABASE_TABLE_COUNTRIES;

        Log.d(TAG, query);
        try (final var cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    countryList.add(createCountryFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return countryList;
    }

}
