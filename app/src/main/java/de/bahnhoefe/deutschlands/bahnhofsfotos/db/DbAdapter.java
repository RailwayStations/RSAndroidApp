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
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStation;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations;
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
    private static final int DATABASE_VERSION = 21;

    private static final String CREATE_STATEMENT_STATIONS = "CREATE TABLE " + DATABASE_TABLE_STATIONS + " ("
            + Constants.STATIONS.ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Constants.STATIONS.COUNTRY + " TEXT, "
            + Constants.STATIONS.ID + " TEXT, "
            + Constants.STATIONS.TITLE + " TEXT, "
            + Constants.STATIONS.NORMALIZED_TITLE + " TEXT, "
            + Constants.STATIONS.LAT + " REAL, "
            + Constants.STATIONS.LON + " REAL, "
            + Constants.STATIONS.PHOTO_ID + " INTEGER, "
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

    public DbAdapter(Context context) {
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

    public void insertStations(PhotoStations photoStations, String countryCode) {
        db.beginTransaction();
        try {
            deleteStations(Set.of(countryCode));
            photoStations.getStations().forEach(station -> db.insert(DATABASE_TABLE_STATIONS, null, toContentValues(station, photoStations)));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(PhotoStation station, PhotoStations photoStations) {
        var values = new ContentValues();
        values.put(Constants.STATIONS.ID, station.getId());
        values.put(Constants.STATIONS.COUNTRY, station.getCountry());
        values.put(Constants.STATIONS.TITLE, station.getTitle());
        values.put(Constants.STATIONS.NORMALIZED_TITLE, StringUtils.replaceChars(StringUtils.deleteWhitespace(StringUtils.stripAccents(Normalizer.normalize(station.getTitle(), Normalizer.Form.NFC))), "-_()", null));
        values.put(Constants.STATIONS.LAT, station.getLat());
        values.put(Constants.STATIONS.LON, station.getLon());
        values.put(Constants.STATIONS.DS100, station.getShortCode());
        values.put(Constants.STATIONS.ACTIVE, !station.isInactive());
        if (station.getPhotos().size() > 0) {
            var photo = station.getPhotos().get(0);
            values.put(Constants.STATIONS.PHOTO_ID, photo.getId());
            values.put(Constants.STATIONS.PHOTO_URL, photoStations.getPhotoBaseUrl() + photo.getPath());
            values.put(Constants.STATIONS.PHOTOGRAPHER, photo.getPhotographer());
            values.put(Constants.STATIONS.OUTDATED, photo.isOutdated());
            values.put(Constants.STATIONS.PHOTOGRAPHER_URL, photoStations.getPhotographerUrl(photo.getPhotographer()));
            values.put(Constants.STATIONS.LICENSE, photoStations.getLicenseName(photo.getLicense()));
            values.put(Constants.STATIONS.LICENSE_URL, photoStations.getLicenseUrl(photo.getLicense()));
        }
        return values;
    }

    public void insertCountries(List<Country> countries) {
        if (countries.isEmpty()) {
            return;
        }
        db.beginTransaction();
        try {
            deleteCountries();
            countries.forEach(this::insertCountry);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertCountry(Country country) {
        db.insert(DATABASE_TABLE_COUNTRIES, null, toContentValues(country));

        country.getProviderApps().stream()
                .map(p -> toContentValues(country.getCode(), p))
                .forEach(values -> db.insert(DATABASE_TABLE_PROVIDER_APPS, null, values));
    }

    private ContentValues toContentValues(String countryCode, ProviderApp app) {
        var values = new ContentValues();
        values.put(Constants.PROVIDER_APPS.COUNTRYSHORTCODE, countryCode);
        values.put(Constants.PROVIDER_APPS.PA_TYPE, app.getType());
        values.put(Constants.PROVIDER_APPS.PA_NAME, app.getName());
        values.put(Constants.PROVIDER_APPS.PA_URL, app.getUrl());
        return values;
    }

    private ContentValues toContentValues(Country country) {
        var values = new ContentValues();
        values.put(Constants.COUNTRIES.COUNTRYSHORTCODE, country.getCode());
        values.put(Constants.COUNTRIES.COUNTRYNAME, country.getName());
        values.put(Constants.COUNTRIES.EMAIL, country.getEmail());
        values.put(Constants.COUNTRIES.TWITTERTAGS, country.getTwitterTags());
        values.put(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE, country.getTimetableUrlTemplate());
        values.put(Constants.COUNTRIES.OVERRIDE_LICENSE, country.getOverrideLicense());
        return values;
    }

    public Upload insertUpload(Upload upload) {
        upload.setId(db.insert(DATABASE_TABLE_UPLOADS, null, toContentValues(upload)));
        return upload;
    }

    public void deleteStations(Set<String> countryCodes) {
        db.delete(DATABASE_TABLE_STATIONS, whereCountryCodeIn(countryCodes), null);
    }

    public void deleteCountries() {
        db.delete(DATABASE_TABLE_PROVIDER_APPS, null, null);
        db.delete(DATABASE_TABLE_COUNTRIES, null, null);
    }

    private String getStationOrderBy(boolean sortByDistance, Location myPos) {
        var orderBy = Constants.STATIONS.TITLE + " ASC";

        if (sortByDistance) {
            var fudge = Math.pow(Math.cos(Math.toRadians(myPos.getLatitude())), 2);
            orderBy = "((" + myPos.getLatitude() + " - " + Constants.STATIONS.LAT + ") * (" + myPos.getLatitude() + " - " + Constants.STATIONS.LAT + ") + " +
                    "(" + myPos.getLongitude() + " - " + Constants.STATIONS.LON + ") * (" + myPos.getLongitude() + " - " + Constants.STATIONS.LON + ") * " + fudge + ")";
        }

        return orderBy;
    }

    public Cursor getCountryList() {
        var selectCountries = "SELECT " + Constants.COUNTRIES.ROWID_COUNTRIES + " AS " + Constants.CURSOR_ADAPTER_ID + ", " +
                Constants.COUNTRIES.COUNTRYSHORTCODE + ", " + Constants.COUNTRIES.COUNTRYNAME +
                " FROM " + DATABASE_TABLE_COUNTRIES + " ORDER BY " + Constants.COUNTRIES.COUNTRYNAME + " ASC";
        Log.d(TAG, selectCountries);

        var cursor = db.rawQuery(selectCountries, null);

        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
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
    public Cursor getStationsListByKeyword(String search, StationFilter stationFilter, Set<String> countryCodes, boolean sortByDistance, Location myPos) {
        var selectQuery = whereCountryCodeIn(countryCodes);
        var queryArgs = new ArrayList<String>();

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

        var cursor = db.query(DATABASE_TABLE_STATIONS,
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

    private String whereCountryCodeIn(Set<String> countryCodes) {
        return Constants.STATIONS.COUNTRY +
                " IN (" +
                countryCodes.stream().map(c -> "'" + c + "'").collect(joining(",")) +
                ")";
    }

    public Statistic getStatistic(String country) {
        try (var cursor = db.rawQuery("SELECT COUNT(*), COUNT(" + Constants.STATIONS.PHOTO_URL + "), COUNT(DISTINCT(" + Constants.STATIONS.PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.COUNTRY + " = ?", new String[]{country})) {
            if (cursor.moveToNext()) {
                return Statistic.builder()
                        .total(cursor.getInt(0))
                        .withPhoto(cursor.getInt(1))
                        .withoutPhoto(cursor.getInt(0) - cursor.getInt(1))
                        .photographers(cursor.getInt(2))
                        .build();
            }
        }
        return null;
    }

    public String[] getPhotographerNicknames() {
        var photographers = new ArrayList<String>();
        try (var cursor = db.rawQuery("SELECT distinct " + Constants.STATIONS.PHOTOGRAPHER + " FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " IS NOT NULL ORDER BY " + Constants.STATIONS.PHOTOGRAPHER, null)) {
            while (cursor.moveToNext()) {
                photographers.add(cursor.getString(0));
            }
        }
        return photographers.toArray(new String[0]);
    }

    public int countStations(Set<String> countryCodes) {
        try (var query = db.rawQuery("SELECT COUNT(*) FROM " + DATABASE_TABLE_STATIONS + " WHERE " + whereCountryCodeIn(countryCodes), null)) {
            if (query.moveToFirst()) {
                return query.getInt(0);
            }
        }
        return 0;
    }

    public void updateUpload(Upload upload) {
        db.beginTransaction();
        try {
            db.update(DATABASE_TABLE_UPLOADS, toContentValues(upload), Constants.UPLOADS.ID + " = ?", new String[]{String.valueOf(upload.getId())});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(Upload upload) {
        var values = new ContentValues();
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

    public Upload getPendingUploadForStation(Station station) {
        try (var cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.COUNTRY + " = ? AND " + Constants.UPLOADS.STATION_ID + " = ? AND " + getPendingUploadWhereClause(),
                new String[]{station.getCountry(), station.getId()}, null, null, Constants.UPLOADS.CREATED_AT + " DESC")) {
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor);
            }
        }

        return null;
    }

    public Upload getPendingUploadForCoordinates(double lat, double lon) {
        try (var cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.LAT + " = ? AND " + Constants.UPLOADS.LON + " = ? AND " + getPendingUploadWhereClause(),
                new String[]{String.valueOf(lat), String.valueOf(lon)}, null, null, Constants.UPLOADS.CREATED_AT + " DESC")) {
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor);
            }
        }

        return null;
    }

    private String getUploadWhereClause(Predicate<UploadState> predicate) {
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
        var queryBuilder = new SQLiteQueryBuilder();
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

    public Upload getUploadById(long id) {
        try (var cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createUploadFromCursor(cursor);
            }
        }

        return null;
    }

    public void deleteUpload(long id) {
        db.delete(DATABASE_TABLE_UPLOADS, Constants.UPLOADS.ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Upload> getPendingUploads(boolean withRemoteId) {
        var selection = getPendingUploadWhereClause();
        if (withRemoteId) {
            selection += " AND " + Constants.UPLOADS.REMOTE_ID + " IS NOT NULL";
        }
        var uploads = new ArrayList<Upload>();
        try (var cursor = db.query(DATABASE_TABLE_UPLOADS, null, selection,
                null, null, null, null)) {
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor));
            }
        }

        return uploads;
    }

    public void updateUploadStates(List<InboxStateQuery> stateQueries) {
        db.beginTransaction();
        try {
            stateQueries.forEach(state -> db.update(DATABASE_TABLE_UPLOADS, toUploadStatesContentValues(state), Constants.UPLOADS.REMOTE_ID + " = ?", new String[]{String.valueOf(state.getId())}));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toUploadStatesContentValues(InboxStateQuery state) {
        var values = new ContentValues();
        values.put(Constants.UPLOADS.UPLOAD_STATE, state.getState().name());
        values.put(Constants.UPLOADS.REJECTED_REASON, state.getRejectedReason());
        values.put(Constants.UPLOADS.CRC32, state.getCrc32());
        return values;
    }

    public List<Upload> getCompletedUploads() {
        var uploads = new ArrayList<Upload>();
        try (var cursor = db.query(DATABASE_TABLE_UPLOADS, null,
                Constants.UPLOADS.REMOTE_ID + " IS NOT NULL AND " + getCompletedUploadWhereClause(),
                null, null, null, null)) {
            while (cursor.moveToNext()) {
                uploads.add(createUploadFromCursor(cursor));
            }
        }

        return uploads;
    }

    static class DbOpenHelper extends SQLiteOpenHelper {

        DbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Creating database");
            db.execSQL(CREATE_STATEMENT_STATIONS);
            db.execSQL(CREATE_STATEMENT_STATIONS_IDX);
            db.execSQL(CREATE_STATEMENT_COUNTRIES);
            db.execSQL(CREATE_STATEMENT_PROVIDER_APPS);
            db.execSQL(CREATE_STATEMENT_UPLOADS);
            Log.i(TAG, "Database structure created.");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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

                if (oldVersion < 21) {
                    db.execSQL("ALTER TABLE " + DATABASE_TABLE_STATIONS + " ADD COLUMN " + Constants.STATIONS.PHOTO_ID + " INTEGER");
                }
            }

            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    @NonNull
    private Station createStationFromCursor(@NonNull Cursor cursor) {
        return Station.builder()
                .title(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.TITLE)))
                .country(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.COUNTRY)))
                .id(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.ID)))
                .lat(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.STATIONS.LAT)))
                .lon(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.STATIONS.LON)))
                .photoId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTO_ID)))
                .photoUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTO_URL)))
                .photographer(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTOGRAPHER)))
                .photographerUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTOGRAPHER_URL)))
                .license(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.LICENSE)))
                .licenseUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.LICENSE_URL)))
                .ds100(cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.DS100)))
                .active(Boolean.TRUE.equals(getBoolean(cursor, Constants.STATIONS.ACTIVE)))
                .outdated(Boolean.TRUE.equals(getBoolean(cursor, Constants.STATIONS.OUTDATED)))
                .build();
    }

    @NonNull
    private Country createCountryFromCursor(@NonNull Cursor cursor) {
        return Country.builder()
                .code(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE)))
                .name(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYNAME)))
                .email(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.EMAIL)))
                .twitterTags(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TWITTERTAGS)))
                .timetableUrlTemplate(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE)))
                .overrideLicense(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.OVERRIDE_LICENSE)))
                .build();
    }

    @NonNull
    private ProviderApp createProviderAppFromCursor(@NonNull Cursor cursor) {
        return ProviderApp.builder()
                .name(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_NAME)))
                .type(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_TYPE)))
                .url(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_URL)))
                .build();
    }

    @NonNull
    private Upload createUploadFromCursor(@NonNull Cursor cursor) {
        var uploadBuilder = Upload.builder()
                .comment(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COMMENT)))
                .country(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COUNTRY)))
                .createdAt(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.CREATED_AT)))
                .id(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.ID)))
                .inboxUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.INBOX_URL)))
                .lat(getDouble(cursor, Constants.UPLOADS.LAT))
                .lon(getDouble(cursor, Constants.UPLOADS.LON))
                .rejectReason(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REJECTED_REASON)))
                .remoteId(getLong(cursor, Constants.UPLOADS.REMOTE_ID))
                .stationId(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.STATION_ID)))
                .title(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.TITLE)))
                .active(getBoolean(cursor, Constants.UPLOADS.ACTIVE))
                .crc32(getLong(cursor, Constants.UPLOADS.CRC32));

        var problemType = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.PROBLEM_TYPE));
        if (problemType != null) {
            uploadBuilder.problemType(ProblemType.valueOf(problemType));
        }
        var uploadState = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.UPLOAD_STATE));
        if (uploadState != null) {
            uploadBuilder.uploadState(UploadState.valueOf(uploadState));
        }

        return uploadBuilder.build();
    }

    private Boolean getBoolean(Cursor cursor, String columnName) {
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(columnName)) == 1;
        }
        return null;
    }

    private Double getDouble(final Cursor cursor, final String columnName) {
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            return cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
        }
        return null;
    }

    private Long getLong(final Cursor cursor, final String columnName) {
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(columnName))) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
        }
        return null;
    }

    public Station fetchStationByRowId(long id) {
        try (var cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.ROWID + "=?", new String[]{
                id + ""}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor);
            }
        }
        return null;
    }

    public Station getStationByKey(String country, String id) {
        try (var cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? AND " + Constants.STATIONS.ID + "=?",
                new String[]{country, id}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor);
            }
        }
        return null;
    }

    public Station getStationForUpload(Upload upload) {
        try (var cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? AND " + Constants.STATIONS.ID + "=?",
                new String[]{upload.getCountry(), upload.getStationId()}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return createStationFromCursor(cursor);
            }
        }
        return null;
    }

    public Set<Country> fetchCountriesWithProviderApps(Set<String> countryCodes) {
        var countryList = countryCodes.stream()
                .map(c -> "'" + c + "'")
                .collect(joining(","));
        var countries = new HashSet<Country>();
        try (var cursor = db.query(DATABASE_TABLE_COUNTRIES, null, Constants.COUNTRIES.COUNTRYSHORTCODE + " IN (" + countryList + ")",
                null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    var country = createCountryFromCursor(cursor);
                    countries.add(country);
                    try (var cursorPa = db.query(DATABASE_TABLE_PROVIDER_APPS, null, Constants.PROVIDER_APPS.COUNTRYSHORTCODE + " = ?",
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

    public List<Station> getAllStations(StationFilter stationFilter, Set<String> countryCodes) {
        var stationList = new ArrayList<Station>();
        var selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + whereCountryCodeIn(countryCodes);
        var queryArgs = new ArrayList<String>();
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

        try (var cursor = db.rawQuery(selectQuery, queryArgs.toArray(new String[]{}))) {
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return stationList;
    }

    public List<Station> getStationByLatLngRectangle(double lat, double lng, StationFilter stationFilter) {
        var stationList = new ArrayList<Station>();
        // Select All Query with rectangle - might be later change with it
        var selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.LAT + " < " + (lat + 0.5) + " AND " + Constants.STATIONS.LAT + " > " + (lat - 0.5)
                + " AND " + Constants.STATIONS.LON + " < " + (lng + 0.5) + " AND " + Constants.STATIONS.LON + " > " + (lng - 0.5);

        var queryArgs = new ArrayList<String>();
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

        try (var cursor = db.rawQuery(selectQuery, queryArgs.toArray(new String[]{}))) {
            if (cursor.moveToFirst()) {
                do {
                    stationList.add(createStationFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return stationList;
    }

    public Set<Country> getAllCountries() {
        var countryList = new HashSet<Country>();
        var query = "SELECT * FROM " + DATABASE_TABLE_COUNTRIES;

        Log.d(TAG, query);
        try (var cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    countryList.add(createCountryFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return countryList;
    }

}
