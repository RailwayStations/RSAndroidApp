package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;

import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Upload;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;

public class DbAdapter {

    private static final String TAG = DbAdapter.class.getSimpleName();

    public static final String DATABASE_TABLE_STATIONS = "bahnhoefe";
    private static final String DATABASE_TABLE_COUNTRIES = "laender";
    private static final String DATABASE_TABLE_PROVIDER_APPS = "providerApps";
    public static final String DATABASE_TABLE_UPLOADS = "uploads";
    private static final String DATABASE_NAME = "bahnhoefe.db";
    private static final int DATABASE_VERSION = 15;

    private static final String CREATE_STATEMENT_STATIONS = "CREATE TABLE " + DATABASE_TABLE_STATIONS + " ("
            + Constants.STATIONS.ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Constants.STATIONS.COUNTRY + " TEXT, "
            + Constants.STATIONS.ID + " TEXT, "
            + Constants.STATIONS.TITLE + " TEXT, "
            + Constants.STATIONS.LAT + " REAL, "
            + Constants.STATIONS.LON + " REAL, "
            + Constants.STATIONS.PHOTO_URL + " TEXT, "
            + Constants.STATIONS.PHOTOGRAPHER + " TEXT, "
            + Constants.STATIONS.PHOTOGRAPHER_URL + " TEXT, "
            + Constants.STATIONS.LICENSE + " TEXT, "
            + Constants.STATIONS.LICENSE_URL + " TEXT, "
            + Constants.STATIONS.DS100 + " TEXT, "
            + Constants.STATIONS.ACTIVE + " INTEGER)";
    private static final String CREATE_STATEMENT_STATIONS_IDX = "CREATE INDEX " + DATABASE_TABLE_STATIONS + "_IDX "
            + "ON " + DATABASE_TABLE_STATIONS + "(" + Constants.STATIONS.COUNTRY + ", " + Constants.STATIONS.ID + ")";
    private static final String CREATE_STATEMENT_COUNTRIES = "CREATE TABLE " + DATABASE_TABLE_COUNTRIES + " ("
            + Constants.COUNTRIES.ROWID_COUNTRIES + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Constants.COUNTRIES.COUNTRYSHORTCODE + " TEXT, "
            + Constants.COUNTRIES.COUNTRYNAME + " TEXT, "
            + Constants.COUNTRIES.EMAIL + " TEXT, "
            + Constants.COUNTRIES.TWITTERTAGS + " TEXT, "
            + Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE + " TEXT)";
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
            + Constants.UPLOADS.CREATED_AT + " INTEGER)";

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

    public void insertBahnhoefe(final List<Station> bahnhoefe, final Set<String> countryCodes) {
        if (bahnhoefe.isEmpty()) {
            return; // nothing todo
        }

        db.beginTransaction(); // soll die Performance verbessern, heißt's.
        try {
            deleteBahnhoefe(countryCodes);
            for (final Station station : bahnhoefe) {
                final ContentValues values = new ContentValues();
                values.put(Constants.STATIONS.ID, station.getId());
                values.put(Constants.STATIONS.COUNTRY, station.getCountry());
                values.put(Constants.STATIONS.TITLE, station.getTitle());
                values.put(Constants.STATIONS.LAT, station.getLat());
                values.put(Constants.STATIONS.LON, station.getLon());
                values.put(Constants.STATIONS.PHOTO_URL, station.getPhotoUrl());
                values.put(Constants.STATIONS.PHOTOGRAPHER, station.getPhotographer());
                values.put(Constants.STATIONS.PHOTOGRAPHER_URL, station.getPhotographerUrl());
                values.put(Constants.STATIONS.LICENSE, station.getLicense());
                values.put(Constants.STATIONS.LICENSE_URL, station.getLicenseUrl());
                values.put(Constants.STATIONS.DS100, station.getDs100());
                values.put(Constants.STATIONS.ACTIVE, station.isActive());

                db.insert(DATABASE_TABLE_STATIONS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertCountries(final List<Country> countries) {
        if (countries.isEmpty()) {
            return; // nothing todo
        }
        db.beginTransaction(); // soll die Performance verbessern, heißt's.
        try {
            deleteCountries();

            for (final Country country : countries) {
                final ContentValues values = new ContentValues();
                values.put(Constants.COUNTRIES.COUNTRYSHORTCODE, country.getCode());
                values.put(Constants.COUNTRIES.COUNTRYNAME, country.getName());
                values.put(Constants.COUNTRIES.EMAIL, country.getEmail());
                values.put(Constants.COUNTRIES.TWITTERTAGS, country.getTwitterTags());
                values.put(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE, country.getTimetableUrlTemplate());

                db.insert(DATABASE_TABLE_COUNTRIES, null, values);

                for (final ProviderApp app : country.getProviderApps()) {
                    final ContentValues paValues = new ContentValues();
                    paValues.put(Constants.PROVIDER_APPS.COUNTRYSHORTCODE, country.getCode());
                    paValues.put(Constants.PROVIDER_APPS.PA_TYPE, app.getType());
                    paValues.put(Constants.PROVIDER_APPS.PA_NAME, app.getName());
                    paValues.put(Constants.PROVIDER_APPS.PA_URL, app.getUrl());
                    db.insert(DATABASE_TABLE_PROVIDER_APPS, null, paValues);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Upload insertUpload(final Upload upload) {
        final ContentValues values = new ContentValues();
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
        values.put(Constants.UPLOADS.UPLOAD_STATE, upload.getUploadState().name());

        upload.setId(db.insert(DATABASE_TABLE_UPLOADS, null, values));
        return upload;
    }

    public void deleteBahnhoefe(final Set<String> countryCodes) {
        db.delete(DATABASE_TABLE_STATIONS, whereCountryCodeIn(countryCodes), null);
    }

    public void deleteCountries() {
        db.delete(DATABASE_TABLE_PROVIDER_APPS, null, null);
        db.delete(DATABASE_TABLE_COUNTRIES, null, null);
    }

    public Cursor getStationsList(final PhotoFilter photoFilter, final String nickname, final Set<String> countryCodes) {
        String selectQuery = "SELECT rowid _id, " +
                Constants.STATIONS.ID + ", " +
                Constants.STATIONS.TITLE + ", " +
                Constants.STATIONS.PHOTO_URL + ", " +
                Constants.STATIONS.COUNTRY +
                " FROM " + DATABASE_TABLE_STATIONS + " WHERE " +
                whereCountryCodeIn(countryCodes);

        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        selectQuery += " ORDER BY " + Constants.STATIONS.TITLE + " ASC";
        Log.d(TAG, selectQuery.toString());

        final Cursor cursor;
        if (photoFilter == PhotoFilter.NICKNAME) {
            cursor = db.rawQuery(selectQuery, new String[]{nickname});
        } else {
            cursor = db.rawQuery(selectQuery, null);
        }

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    public Cursor getCountryList() {
        //Open connection to read only
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String selectQueryCountries = "SELECT rowidcountries _id, " +
                Constants.COUNTRIES.COUNTRYSHORTCODE + ", " +
                Constants.COUNTRIES.COUNTRYNAME +
                " FROM " + DATABASE_TABLE_COUNTRIES
                + " ORDER BY " +
                Constants.COUNTRIES.COUNTRYNAME + " asc";
        Log.d(TAG, selectQueryCountries.toString());


        final Cursor cursor = db.rawQuery(selectQueryCountries, null);
        // looping through all rows and adding to list

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }


    /**
     * Return a cursor on station ids where the station's title matches the given string
     *
     * @param search
     * @param photoFilter if stations need to be filtered by photo available or not
     * @param countryCodes
     * @return a Cursor representing the matching results
     */
    public Cursor getStationsListByKeyword(final String search, final PhotoFilter photoFilter, final String nickname, final Set<String> countryCodes) {
        String selectQuery = whereCountryCodeIn(countryCodes);
        final List<String> queryArgs = new ArrayList<>();

        if (StringUtils.isNotBlank(search)) {
            selectQuery += String.format(" AND %s LIKE ?", Constants.STATIONS.TITLE);
            queryArgs.add("%" + StringUtils.trimToEmpty(search) + "%");
        }

        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        if (photoFilter == PhotoFilter.NICKNAME) {
            queryArgs.add(nickname);
        }

        final Cursor cursor = db.query(DATABASE_TABLE_STATIONS,
                new String[]{
                        "rowid _id", Constants.STATIONS.ID, Constants.STATIONS.TITLE, Constants.STATIONS.PHOTO_URL, Constants.STATIONS.COUNTRY
                },
                selectQuery,
                queryArgs.toArray(new String[0]), null, null, Constants.STATIONS.TITLE + " asc");

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            Log.w(TAG, String.format("Query '%s' returned no result", search));
            cursor.close();
            return null;
        }
        return cursor;
    }

    private String whereCountryCodeIn(final Set<String> countryCodes) {
        String selectQuery = Constants.STATIONS.COUNTRY + " IN (";
        for (final String country : countryCodes) {
            selectQuery += "'" + country + "',";
        }
        selectQuery = selectQuery.substring(0, selectQuery.length() - 1) + ')';
        return selectQuery;
    }

    public Statistic getStatistic(final String country) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        final Cursor cursor = db.rawQuery("SELECT count(*), count(" + Constants.STATIONS.PHOTO_URL + "), count(distinct(" + Constants.STATIONS.PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE_STATIONS + " WHERE country = ?", new String[]{country});
        if (!cursor.moveToNext()) {
            return null;
        }
        return new Statistic(cursor.getInt(0), cursor.getInt(1), cursor.getInt(0) - cursor.getInt(1), cursor.getInt(2));
    }

    public String[] getPhotographerNicknames() {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();

        final List<String> photographers = new ArrayList<>();
        final Cursor cursor = db.rawQuery("SELECT distinct " + Constants.STATIONS.PHOTOGRAPHER + " FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " IS NOT NULL ORDER BY " + Constants.STATIONS.PHOTOGRAPHER, null);
        while (cursor.moveToNext()) {
            photographers.add(cursor.getString(0));
        }
        cursor.close();
        return photographers.toArray(new String[0]);
    }

    public int countBahnhoefe() {
        final Cursor query = db.rawQuery("SELECT count(*) FROM " + DATABASE_TABLE_STATIONS, null);
        if (query != null && query.moveToFirst()) {
            return query.getInt(0);
        }
        return 0;
    }

    public void updateUpload(final Upload upload) {
        db.beginTransaction();
        try {
            final ContentValues values = new ContentValues();
            values.put(Constants.UPLOADS.REMOTE_ID, upload.getRemoteId());
            values.put(Constants.UPLOADS.INBOX_URL, upload.getInboxUrl());
            values.put(Constants.UPLOADS.UPLOAD_STATE, upload.getUploadState().name());
            db.update(DATABASE_TABLE_UPLOADS, values, Constants.UPLOADS.ID + " = ?", new String[]{String.valueOf(upload.getId())});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Upload getPendingUploadForStation(final Station station) {
        final Cursor cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.COUNTRY + "=? and " + Constants.UPLOADS.STATION_ID + "=? and " + getPendingUploadWhereClause(),
                new String[]{ station.getCountry(), station.getId()}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final Upload upload = createUploadFromCursor(cursor);
            cursor.close();
            return upload;
        }

        return null;
    }

    public Upload getPendingUploadForCoordinates(final double lat, final double lon) {
        final Cursor cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.LAT + "=? and " + Constants.UPLOADS.LON + "=? and " + getPendingUploadWhereClause(),
                new String[]{ String.valueOf(lat), String.valueOf(lon)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final Upload upload = createUploadFromCursor(cursor);
            cursor.close();
            return upload;
        }

        return null;
    }

    private String getPendingUploadWhereClause() {
        String where = Constants.UPLOADS.UPLOAD_STATE + " IN (";
        for (final UploadState state : UploadState.values()) {
            if (state.isPending()) {
                where += "'" + state.name() + "',";
            }
        }
        return where.substring(0, where.length() - 1) + ')';
    }

    public Cursor getUploadsWithStations() {
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DATABASE_TABLE_UPLOADS
                        + " LEFT JOIN "
                        + DATABASE_TABLE_STATIONS
                        + " ON "
                        + DATABASE_TABLE_STATIONS + "." + Constants.STATIONS.COUNTRY
                        + " = "
                        + DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.COUNTRY
                        + " and "
                        + DATABASE_TABLE_STATIONS + "." + Constants.STATIONS.ID
                        + " = "
                        + DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.STATION_ID);

        return queryBuilder.query(db, new String[]{
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.ID + " AS _id",
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.REMOTE_ID,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.COUNTRY,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.STATION_ID,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.TITLE,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.UPLOAD_STATE,
                DATABASE_TABLE_UPLOADS + "." + Constants.UPLOADS.PROBLEM_TYPE,
                DATABASE_TABLE_STATIONS + "." + Constants.UPLOADS.TITLE + " AS stationTitle"
        }, null, null, null, null, Constants.UPLOADS.CREATED_AT + " DESC");
    }

    public Upload getUploadById(final long id) {
        final Cursor cursor = db.query(DATABASE_TABLE_UPLOADS, null, Constants.UPLOADS.ID + "=?",
                new String[]{ String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final Upload upload = createUploadFromCursor(cursor);
            cursor.close();
            return upload;
        }

        return null;
    }

    public void deleteUpload(final long id) {
        db.delete(DATABASE_TABLE_UPLOADS, Constants.UPLOADS.ID + "=?", new String[]{ String.valueOf(id)});
    }

    class DbOpenHelper extends SQLiteOpenHelper {

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

            // from now on we need to preserve user data and perform schema changes selectively
            } else if (oldVersion < 14) {
                db.execSQL(CREATE_STATEMENT_UPLOADS);
            } else if (oldVersion < 15) {
                db.execSQL(DROP_STATEMENT_STATIONS_IDX);
                db.execSQL(CREATE_STATEMENT_STATIONS_IDX);
            }

            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    @NonNull
    private Station createStationFromCursor(@NonNull final Cursor cursor) {
        final Station station = new Station();
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
        return station;
    }

    @NonNull
    private Country createCountryFromCursor(@NonNull final Cursor cursor) {
        final Country country = new Country();
        country.setCode(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE)));
        country.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYNAME)));
        country.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.EMAIL)));
        country.setTwitterTags(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TWITTERTAGS)));
        country.setTimetableUrlTemplate(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE)));
        return country;
    }

    @NonNull
    private ProviderApp createProviderAppFromCursor(@NonNull final Cursor cursor) {
        final ProviderApp providerApp = new ProviderApp();
        providerApp.setCountryCode(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.COUNTRYSHORTCODE)));
        providerApp.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_NAME)));
        providerApp.setType(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_TYPE)));
        providerApp.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_URL)));
        return providerApp;
    }

    @NonNull
    private Upload createUploadFromCursor(@NonNull final Cursor cursor) {
        final Upload upload = new Upload();
        upload.setComment(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COMMENT)));
        upload.setCountry(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COUNTRY)));
        upload.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.CREATED_AT)));
        upload.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.ID)));
        upload.setInboxUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.INBOX_URL)));
        upload.setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.UPLOADS.LAT)));
        upload.setLon(cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.UPLOADS.LON)));
        final String problemType = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.PROBLEM_TYPE));
        if (problemType != null) {
            upload.setProblemType(ProblemType.valueOf(problemType));
        }
        upload.setRejectReason(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REJECTED_REASON)));
        upload.setRemoteId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REMOTE_ID)));
        upload.setStationId(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.STATION_ID)));
        upload.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.TITLE)));
        final String uploadState = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.UPLOAD_STATE));
        if (uploadState != null) {
            upload.setUploadState(UploadState.valueOf(uploadState));
        }
        return upload;
    }

    public Station fetchStationByRowId(final long id) {
        final Cursor cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.ROWID + "=?", new String[]{
                id + ""}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final Station station = createStationFromCursor(cursor);
            cursor.close();
            return station;
        } else
            return null;
    }

    public Station getStationByKey(final String country, final String id) {
        final Cursor cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? and " + Constants.STATIONS.ID + "=?",
                new String[]{ country, id}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final Station station = createStationFromCursor(cursor);
            cursor.close();
            return station;
        }

        return null;
    }

    public Station getStationForUpload(final Upload upload) {
        Cursor cursor = null;
        if (upload.isUploadForExistingStation()) {
            cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? and " + Constants.STATIONS.ID + "=?",
                    new String[]{upload.getCountry(), upload.getStationId()}, null, null, null);
        }
        if (cursor != null && cursor.moveToFirst()) {
            final Station station = createStationFromCursor(cursor);
            cursor.close();
            return station;
        }

        return null;
    }

    public Set<Country> fetchCountriesWithProviderApps(final Set<String> countryCodes) {
        final StringBuilder countryList = new StringBuilder();
        for (final String countryCode : countryCodes) {
            if (countryList.length() > 0) {
                countryList.append(',');
            }
            countryList.append('\'').append(countryCode).append('\'');
        }
        final Cursor cursor = db.query(DATABASE_TABLE_COUNTRIES, null, Constants.COUNTRIES.COUNTRYSHORTCODE + " IN (" + countryList.toString() + ")",
                null, null, null, null);
        final Set<Country> countries = new HashSet<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                final Country country = createCountryFromCursor(cursor);
                countries.add(country);

                final Cursor cursorPa = db.query(DATABASE_TABLE_PROVIDER_APPS, null, Constants.PROVIDER_APPS.COUNTRYSHORTCODE + " = ?",
                        new String[]{country.getCode()}, null, null, null);
                if (cursorPa != null && cursorPa.moveToFirst()) {
                    do {
                        country.getProviderApps().add(createProviderAppFromCursor(cursorPa));
                    } while (cursorPa.moveToNext());
                }

            } while (cursor.moveToNext());
        }

        return countries;
    }

    public List<Station> getAllStations(final PhotoFilter photoFilter, final String nickname) {
        final List<Station> stationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS;
        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " WHERE " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }
        Log.d(TAG, selectQuery.toString());

        final Cursor cursor;
        if (photoFilter == PhotoFilter.NICKNAME) {
            cursor = db.rawQuery(selectQuery, new String[]{nickname});
        } else {
            cursor = db.rawQuery(selectQuery, null);
        }

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                final Station station = createStationFromCursor(cursor);
                stationList.add(station);
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "Station #" + stationList.size() + " " + station);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return stationList;
    }

    public List<Station> getStationByLatLngRectangle(final double lat, final double lng, final PhotoFilter photoFilter, final String nickname) {
        final List<Station> stationList = new ArrayList<>();
        // Select All Query with rectangle - might be later change with it
        String selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS + " where " + Constants.STATIONS.LAT + " < " + (lat + 0.5) + " AND " + Constants.STATIONS.LAT + " > " + (lat - 0.5)
                + " AND " + Constants.STATIONS.LON + " < " + (lng + 0.5) + " AND " + Constants.STATIONS.LON + " > " + (lng - 0.5);
        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        final Cursor cursor;
        if (photoFilter == PhotoFilter.NICKNAME) {
            cursor = db.rawQuery(selectQuery, new String[]{nickname});
        } else {
            cursor = db.rawQuery(selectQuery, null);
        }

        if (cursor.moveToFirst()) {
            do {
                final Station station = createStationFromCursor(cursor);
                stationList.add(station);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return stationList;
    }

    public List<Country> getAllCountries() {
        final List<Country> countryList = new ArrayList<Country>();
        // Select All Query without any baseLocationValues (myCurrentLocation)
        final String query = "SELECT * FROM " + DATABASE_TABLE_COUNTRIES;

        Log.d(TAG, query);
        final Cursor cursor = db.rawQuery(query, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                final Country country = createCountryFromCursor(cursor);
                // Adding country to list
                countryList.add(country);
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "Country #" + countryList.size() + " " + country);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return countryList;
    }


}
