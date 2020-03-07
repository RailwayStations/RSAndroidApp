package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;

public class BahnhofsDbAdapter {

    private static final String TAG = BahnhofsDbAdapter.class.getSimpleName();

    private static final String DATABASE_TABLE_STATIONS = "bahnhoefe";
    private static final String DATABASE_TABLE_COUNTRIES = "laender";
    private static final String DATABASE_TABLE_PROVIDER_APPS = "providerApps";
    private static final String DATABASE_TABLE_UPLOADS = "uploads";
    private static final String DATABASE_NAME = "bahnhoefe.db";
    private static final int DATABASE_VERSION = 14;

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
            + "ON " + DATABASE_TABLE_STATIONS + "(" + Constants.STATIONS.ID + ")";
    private static final String CREATE_STATEMENT_COUNTRIES = "CREATE TABLE " + DATABASE_TABLE_COUNTRIES + " ("
            + Constants.COUNTRIES.ROWID_COUNTRIES + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
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
            + Constants.UPLOADS.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
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
    private BahnhoefeDbOpenHelper dbHelper;
    private SQLiteDatabase db;

    public BahnhofsDbAdapter(Context context) {
        this.context = context;
    }

    public void open() {
        dbHelper = new BahnhoefeDbOpenHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        db.close();
        dbHelper.close();
    }

    public void insertBahnhoefe(List<Bahnhof> bahnhoefe) {
        if (bahnhoefe.isEmpty()) {
            return; // nothing todo
        }

        db.beginTransaction(); // soll die Performance verbessern, heißt's.
        try {
            for (Bahnhof bahnhof : bahnhoefe) {
                ContentValues values = new ContentValues();
                values.put(Constants.STATIONS.ID, bahnhof.getId());
                values.put(Constants.STATIONS.COUNTRY, bahnhof.getCountry());
                values.put(Constants.STATIONS.TITLE, bahnhof.getTitle());
                values.put(Constants.STATIONS.LAT, bahnhof.getLat());
                values.put(Constants.STATIONS.LON, bahnhof.getLon());
                values.put(Constants.STATIONS.PHOTO_URL, bahnhof.getPhotoUrl());
                values.put(Constants.STATIONS.PHOTOGRAPHER, bahnhof.getPhotographer());
                values.put(Constants.STATIONS.PHOTOGRAPHER_URL, bahnhof.getPhotographerUrl());
                values.put(Constants.STATIONS.LICENSE, bahnhof.getLicense());
                values.put(Constants.STATIONS.LICENSE_URL, bahnhof.getLicenseUrl());
                values.put(Constants.STATIONS.DS100, bahnhof.getDs100());
                values.put(Constants.STATIONS.ACTIVE, bahnhof.isActive());

                db.insert(DATABASE_TABLE_STATIONS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertCountries(List<Country> countries) {
        if (countries.isEmpty()) {
            return; // nothing todo
        }
        db.beginTransaction(); // soll die Performance verbessern, heißt's.
        try {
            deleteCountries();

            for (Country country : countries) {
                ContentValues values = new ContentValues();
                values.put(Constants.COUNTRIES.COUNTRYSHORTCODE, country.getCode());
                values.put(Constants.COUNTRIES.COUNTRYNAME, country.getName());
                values.put(Constants.COUNTRIES.EMAIL, country.getEmail());
                values.put(Constants.COUNTRIES.TWITTERTAGS, country.getTwitterTags());
                values.put(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE, country.getTimetableUrlTemplate());

                db.insert(DATABASE_TABLE_COUNTRIES, null, values);

                for (ProviderApp app : country.getProviderApps()) {
                    ContentValues paValues = new ContentValues();
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

    public void deleteBahnhoefe() {
        db.delete(DATABASE_TABLE_STATIONS, null, null);
    }

    public void deleteCountries() {
        db.delete(DATABASE_TABLE_PROVIDER_APPS, null, null);
        db.delete(DATABASE_TABLE_COUNTRIES, null, null);
    }

    public Cursor getStationsList(PhotoFilter photoFilter, String nickname) {
        String selectQuery = "SELECT rowid _id, " +
                Constants.STATIONS.ID + ", " +
                Constants.STATIONS.TITLE + ", " +
                Constants.STATIONS.PHOTO_URL + ", " +
                Constants.STATIONS.COUNTRY +
                " FROM " + DATABASE_TABLE_STATIONS;

        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " WHERE " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        selectQuery += " ORDER BY " + Constants.STATIONS.TITLE + " asc";
        Log.d(TAG, selectQuery.toString());

        Cursor cursor;
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
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQueryCountries = "SELECT rowidcountries _id, " +
                Constants.COUNTRIES.COUNTRYSHORTCODE + ", " +
                Constants.COUNTRIES.COUNTRYNAME +
                " FROM " + DATABASE_TABLE_COUNTRIES
                + " ORDER BY " +
                Constants.COUNTRIES.COUNTRYNAME + " asc";
        Log.d(TAG, selectQueryCountries.toString());


        Cursor cursor = db.rawQuery(selectQueryCountries, null);
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
     * @return a Cursor representing the matching results
     */
    public Cursor getBahnhofsListByKeyword(String search, PhotoFilter photoFilter, String nickname) {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = "1 = 1";
        List<String> queryArgs = new ArrayList<>();

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

        Cursor cursor = db.query(DATABASE_TABLE_STATIONS,
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

    public Statistic getStatistic(String country) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT count(*), count(" + Constants.STATIONS.PHOTO_URL + "), count(distinct(" + Constants.STATIONS.PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE_STATIONS + " WHERE country = ?", new String[]{country});
        if (!cursor.moveToNext()) {
            return null;
        }
        return new Statistic(cursor.getInt(0), cursor.getInt(1), cursor.getInt(0) - cursor.getInt(1), cursor.getInt(2));
    }

    public String[] getPhotographerNicknames() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<String> photographers = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT distinct " + Constants.STATIONS.PHOTOGRAPHER + " FROM " + DATABASE_TABLE_STATIONS + " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " IS NOT NULL ORDER BY " + Constants.STATIONS.PHOTOGRAPHER, null);
        while (cursor.moveToNext()) {
            photographers.add(cursor.getString(0));
        }
        cursor.close();
        return photographers.toArray(new String[0]);
    }

    public int countBahnhoefe() {
        Cursor query = db.rawQuery("SELECT count(*) FROM " + DATABASE_TABLE_STATIONS, null);
        if (query != null && query.moveToFirst()) {
            return query.getInt(0);
        }
        return 0;
    }

    public void updateUpload(InboxResponse inboxResponse, int uploadId) {
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.UPLOADS.REMOTE_ID, inboxResponse.getId());
            values.put(Constants.UPLOADS.INBOX_URL, inboxResponse.getInboxUrl());
            values.put(Constants.UPLOADS.UPLOAD_STATE, inboxResponse.getState().toString());
            db.update(DATABASE_TABLE_UPLOADS, values, Constants.UPLOADS.ID + " = ?", new String[]{String.valueOf(uploadId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    class BahnhoefeDbOpenHelper extends SQLiteOpenHelper {

        BahnhoefeDbOpenHelper(Context context) {
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
            }
            db.endTransaction();
        }
    }

    @NonNull
    private Bahnhof createBahnhofFromCursor(@NonNull Cursor cursor) {
        String title = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.TITLE));
        String country = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.COUNTRY));
        String bahnhofsnr = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.ID));
        Double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.STATIONS.LON));
        Double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.STATIONS.LAT));
        String photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTO_URL));
        String photographer = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTOGRAPHER));
        String photographerUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.PHOTOGRAPHER_URL));
        String license = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.LICENSE));
        String licenseUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.LICENSE_URL));
        String ds100 = cursor.getString(cursor.getColumnIndexOrThrow(Constants.STATIONS.DS100));
        boolean isActive = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.STATIONS.ACTIVE)) == 1;
        Bahnhof bahnhof = new Bahnhof();
        bahnhof.setTitle(title);
        bahnhof.setCountry(country);
        bahnhof.setId(bahnhofsnr);
        bahnhof.setLat(lat);
        bahnhof.setLon(lon);
        bahnhof.setPhotoUrl(photoUrl);
        bahnhof.setPhotographer(photographer);
        bahnhof.setPhotographerUrl(photographerUrl);
        bahnhof.setLicense(license);
        bahnhof.setLicenseUrl(licenseUrl);
        bahnhof.setDs100(ds100);
        bahnhof.setActive(isActive);
        return bahnhof;
    }

    @NonNull
    private Country createCountryFromCursor(@NonNull Cursor cursor) {
        String countryShortCode = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYSHORTCODE));
        String countryName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.COUNTRYNAME));
        String email = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.EMAIL));
        String twitterTags = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TWITTERTAGS));
        String timetableUrlTemplate = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COUNTRIES.TIMETABLE_URL_TEMPLATE));
        Country country = new Country();
        country.setCode(countryShortCode);
        country.setName(countryName);
        country.setEmail(email);
        country.setTwitterTags(twitterTags);
        country.setTimetableUrlTemplate(timetableUrlTemplate);
        return country;
    }

    @NonNull
    private ProviderApp createProviderAppFromCursor(@NonNull Cursor cursor) {
        String countryShortCode = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.COUNTRYSHORTCODE));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_NAME));
        String type = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_TYPE));
        String url = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PROVIDER_APPS.PA_URL));
        ProviderApp providerApp = new ProviderApp();
        providerApp.setCountryCode(countryShortCode);
        providerApp.setName(name);
        providerApp.setType(type);
        providerApp.setUrl(url);
        return providerApp;
    }

    public Bahnhof fetchBahnhofByRowId(long id) {
        Cursor cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.ROWID + "=?", new String[]{
                id + ""}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Bahnhof bahnhof = createBahnhofFromCursor(cursor);
            cursor.close();
            return bahnhof;
        } else
            return null;
    }

    public Bahnhof getBahnhofByKey(String country, String id) {
        Cursor cursor = db.query(DATABASE_TABLE_STATIONS, null, Constants.STATIONS.COUNTRY + "=? and " + Constants.STATIONS.ID + "=?",
                new String[]{ country, id}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Bahnhof bahnhof = createBahnhofFromCursor(cursor);
            cursor.close();
            return bahnhof;
        } else
            return null;
    }

    public Set<Country> fetchCountriesWithProviderApps(Set<String> countryCodes) {
        StringBuilder countryList = new StringBuilder();
        for (String countryCode : countryCodes) {
            if (countryList.length() > 0) {
                countryList.append(',');
            }
            countryList.append('\'').append(countryCode).append('\'');
        }
        Cursor cursor = db.query(DATABASE_TABLE_COUNTRIES, null, Constants.COUNTRIES.COUNTRYSHORTCODE + " in (" + countryList.toString() + ")",
                null, null, null, null);
        Set<Country> countries = new HashSet<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Country country = createCountryFromCursor(cursor);
                countries.add(country);

                Cursor cursorPa = db.query(DATABASE_TABLE_PROVIDER_APPS, null, Constants.PROVIDER_APPS.COUNTRYSHORTCODE + " = ?",
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

    public List<Bahnhof> getAllBahnhoefe(PhotoFilter photoFilter, String nickname) {
        List<Bahnhof> bahnhofList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS;
        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " WHERE " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " WHERE " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }
        Log.d(TAG, selectQuery.toString());

        Cursor cursor;
        if (photoFilter == PhotoFilter.NICKNAME) {
            cursor = db.rawQuery(selectQuery, new String[]{nickname});
        } else {
            cursor = db.rawQuery(selectQuery, null);
        }

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bahnhof bahnhof = createBahnhofFromCursor(cursor);
                // Adding bahnhof to list
                bahnhofList.add(bahnhof);
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "Bahnhof #" + bahnhofList.size() + " " + bahnhof);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return bahnhofList;
    }

    public List<Bahnhof> getBahnhoefeByLatLngRectangle(double lat, double lng, PhotoFilter photoFilter, String nickname) {
        List<Bahnhof> bahnhofList = new ArrayList<>();
        // Select All Query with rectangle - might be later change with it
        String selectQuery = "SELECT * FROM " + DATABASE_TABLE_STATIONS + " where " + Constants.STATIONS.LAT + " < " + (lat + 0.5) + " AND " + Constants.STATIONS.LAT + " > " + (lat - 0.5)
                + " AND " + Constants.STATIONS.LON + " < " + (lng + 0.5) + " AND " + Constants.STATIONS.LON + " > " + (lng - 0.5);
        if (photoFilter == PhotoFilter.NICKNAME) {
            selectQuery += " AND " + Constants.STATIONS.PHOTOGRAPHER + " = ?";
        } else if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " AND " + Constants.STATIONS.PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        Cursor cursor;
        if (photoFilter == PhotoFilter.NICKNAME) {
            cursor = db.rawQuery(selectQuery, new String[]{nickname});
        } else {
            cursor = db.rawQuery(selectQuery, null);
        }

        if (cursor.moveToFirst()) {
            do {
                Bahnhof bahnhof = createBahnhofFromCursor(cursor);
                bahnhofList.add(bahnhof);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return bahnhofList;
    }

    public List<Country> getAllCountries() {
        List<Country> countryList = new ArrayList<Country>();
        // Select All Query without any baseLocationValues (myCurrentLocation)
        String query = "SELECT * FROM " + DATABASE_TABLE_COUNTRIES;

        Log.d(TAG, query);
        Cursor cursor = db.rawQuery(query, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Country country = createCountryFromCursor(cursor);
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
