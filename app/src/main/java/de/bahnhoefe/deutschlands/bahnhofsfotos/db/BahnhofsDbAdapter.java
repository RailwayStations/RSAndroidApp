package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Statistic;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.DB_JSON_CONSTANTS.KEY_ID;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PhotoFilter;

public class BahnhofsDbAdapter {
    private static final String DATABASE_TABLE = "bahnhoefe";
    private static final String DATABASE_TABLE_LAENDER = "laender";
    private static final String DATABASE_NAME = "bahnhoefe.db";
    private static final int DATABASE_VERSION = 8;

    private static final String CREATE_STATEMENT_1 = "CREATE TABLE " + DATABASE_TABLE + " ("
            + Constants.DB_JSON_CONSTANTS.KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
            + KEY_ID + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_TITLE + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_LAT + " REAL, "
            + Constants.DB_JSON_CONSTANTS.KEY_LON + " REAL, "
            + Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER_URL + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_LICENSE + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_DS100 + " TEXT)";
    private static final String CREATE_STATEMENT_2 = "CREATE INDEX " + DATABASE_TABLE + "_IDX "
            + "ON " + DATABASE_TABLE + "(" + Constants.DB_JSON_CONSTANTS.KEY_ID + ")";
    private static final String CREATE_STATEMENT_COUNTRIES = "CREATE TABLE " + DATABASE_TABLE_LAENDER + " ("
            + Constants.DB_JSON_CONSTANTS.KEY_ROWID_COUNTRIES + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
            + Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_EMAIL + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_TIMETABLE_URL_TEMPLATE + " TEXT)";

    private static final String DROP_STATEMENT_1 = "DROP INDEX IF EXISTS " + DATABASE_TABLE + "_IDX";
    private static final String DROP_STATEMENT_2 = "DROP TABLE IF EXISTS " + DATABASE_TABLE;
    private static final String DROP_STATEMENT_COUNTRIES = "DROP TABLE IF EXISTS " + DATABASE_TABLE_LAENDER;
    private static final String TAG = BahnhoefeDbOpenHelper.class.getSimpleName();


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
            deleteBahnhoefe();

            for (Bahnhof bahnhof : bahnhoefe) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID, bahnhof.getId());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_TITLE, bahnhof.getTitle());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_LAT, bahnhof.getLat());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_LON, bahnhof.getLon());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL, bahnhof.getPhotoUrl());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER, bahnhof.getPhotographer());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER_URL, bahnhof.getPhotographerUrl());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_LICENSE, bahnhof.getLicense());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_DS100, bahnhof.getDS100());

                db.insert(DATABASE_TABLE, null, values);
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
                values.put(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE, country.getCountryShortCode());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME, country.getCountryName());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_EMAIL, country.getEmail());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS, country.getTwitterTags());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_TIMETABLE_URL_TEMPLATE, country.getTimetableUrlTemplate());

                db.insert(DATABASE_TABLE_LAENDER, null, values);

            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteBahnhoefe() {
        db.delete(DATABASE_TABLE, null, null);
    }

    public void deleteCountries() {
        db.delete(DATABASE_TABLE_LAENDER, null, null);
    }

    public Cursor getStationsList(PhotoFilter photoFilter) {
        String selectQuery = "SELECT rowid _id, " +
                KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL +
                " FROM " + DATABASE_TABLE;

        if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " WHERE " + Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        selectQuery += " ORDER BY " + Constants.DB_JSON_CONSTANTS.KEY_TITLE + " asc";
        Log.d(TAG, selectQuery.toString());


        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list

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
                Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME +
                " FROM " + DATABASE_TABLE_LAENDER
                + " ORDER BY " +
                Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME + " asc";
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
    public Cursor getBahnhofsListByKeyword(String search, PhotoFilter photoFilter) {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = String.format("%s LIKE ?", Constants.DB_JSON_CONSTANTS.KEY_TITLE);
        if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " AND " + Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        Cursor cursor = db.query(DATABASE_TABLE,
                new String[]{
                        "rowid _id", Constants.DB_JSON_CONSTANTS.KEY_ID, Constants.DB_JSON_CONSTANTS.KEY_TITLE, Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL
                },
                selectQuery,
                new String[]{"%" + search + "%"}, null, null, Constants.DB_JSON_CONSTANTS.KEY_TITLE + " asc");
        // looping through all rows and adding to list

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            Log.w(TAG, String.format("Query '%s' returned no result", search));
            cursor.close();
            return null;
        }
        return cursor;
    }

    public Statistic getStatistic() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT count(*), count(" + Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL + "), count(distinct(" + Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER + ")) FROM " + DATABASE_TABLE, null);
        if (!cursor.moveToNext()) {
            return null;
        }
        return new Statistic(cursor.getInt(0), cursor.getInt(1), cursor.getInt(0) - cursor.getInt(1), cursor.getInt(2));
    }

    class BahnhoefeDbOpenHelper extends SQLiteOpenHelper {

        BahnhoefeDbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, CREATE_STATEMENT_1);
            db.execSQL(CREATE_STATEMENT_1);
            Log.d(TAG, CREATE_STATEMENT_2);
            db.execSQL(CREATE_STATEMENT_2);
            Log.d(TAG, CREATE_STATEMENT_COUNTRIES);
            db.execSQL(CREATE_STATEMENT_COUNTRIES);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrade der Datenbank von Version" + oldVersion + " nach " + newVersion + " wird durchgeführt.");
            db.execSQL(DROP_STATEMENT_1);
            db.execSQL(DROP_STATEMENT_2);
            db.execSQL(DROP_STATEMENT_COUNTRIES);
            onCreate(db);
        }
    }

    @NonNull
    private Bahnhof createBahnhofFromCursor(@NonNull Cursor cursor) {
        String title = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_TITLE));
        String bahnhofsnr = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_ID));
        Double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LON));
        Double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LAT));
        String photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL));
        String photographer = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER));
        String photographerUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_PHOTOGRAPHER_URL));
        String license = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LICENSE));
        String ds100 = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_DS100));
        Bahnhof bahnhof = new Bahnhof();
        bahnhof.setTitle(title);
        bahnhof.setId(bahnhofsnr);
        bahnhof.setLat(lat);
        bahnhof.setLon(lon);
        bahnhof.setPhotoUrl(photoUrl);
        bahnhof.setPhotographer(photographer);
        bahnhof.setPhotographerUrl(photographerUrl);
        bahnhof.setLicense(license);
        bahnhof.setDS100(ds100);
        return bahnhof;
    }

    @NonNull
    private Country createCountryFromCursor(@NonNull Cursor cursor) {
        String countryShortCode = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE));
        String countryName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME));
        String email = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_EMAIL));
        String twitterTags = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS));
        String timetableUrlTemplate = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_TIMETABLE_URL_TEMPLATE));
        Country country = new Country();
        country.setCountryShortCode(countryShortCode);
        country.setCountryName(countryName);
        country.setEmail(email);
        country.setTwitterTags(twitterTags);
        country.setTimetableUrlTemplate(timetableUrlTemplate);
        return country;
    }

    public Bahnhof fetchBahnhofByRowId(long id) {
        Cursor cursor = db.query(DATABASE_TABLE, null, Constants.DB_JSON_CONSTANTS.KEY_ROWID + "=?", new String[]{
                id + ""}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Bahnhof bahnhof = createBahnhofFromCursor(cursor);
            cursor.close();
            return bahnhof;
        } else
            return null;
    }

    public Bahnhof fetchBahnhofByBahnhofId(String id) {
        Cursor cursor = db.query(DATABASE_TABLE, null, Constants.DB_JSON_CONSTANTS.KEY_ID + "=?", new String[]{
                id + ""}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Bahnhof bahnhof = createBahnhofFromCursor(cursor);
            cursor.close();
            return bahnhof;
        } else
            return null;
    }

    public Country fetchCountryByCountryShortCode(String countryShortCode) {
        Cursor cursor = db.query(DATABASE_TABLE_LAENDER, null, Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE + "=?", new String[]{
                countryShortCode + ""}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Country country = createCountryFromCursor(cursor);
            cursor.close();
            return country;
        } else
            return null;
    }

    public List<Bahnhof> getAllBahnhoefe(PhotoFilter photoFilter) {
        List<Bahnhof> bahnhofList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DATABASE_TABLE;
        if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " WHERE " + Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }
        Log.d(TAG, selectQuery.toString());
        Cursor cursor = db.rawQuery(selectQuery, null);

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

    public List<Bahnhof> getBahnhoefeByLatLngRectangle(LatLng position, PhotoFilter photoFilter) {
        double lat = position.latitude;
        double lng = position.longitude;
        List<Bahnhof> bahnhofList = new ArrayList<>();
        // Select All Query with rectangle - might be later change with it
        String selectQuery = "SELECT * FROM " + DATABASE_TABLE + " where " + Constants.DB_JSON_CONSTANTS.KEY_LAT + " < " + (lat + 0.5) + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LAT + " > " + (lat - 0.5)
                + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON + " < " + (lng + 0.5) + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON + " > " + (lng - 0.5);
        if (photoFilter != PhotoFilter.ALL_STATIONS) {
            selectQuery += " AND " + Constants.DB_JSON_CONSTANTS.KEY_PHOTO_URL + " IS " + (photoFilter == PhotoFilter.STATIONS_WITH_PHOTO ? "NOT" : "") + " NULL";
        }

        Cursor cursor = db.rawQuery(selectQuery, null);

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
        String selectQueryCountries = "SELECT * FROM " + DATABASE_TABLE_LAENDER;

        Log.d(TAG, selectQueryCountries.toString());
        Cursor cursor = db.rawQuery(selectQueryCountries, null);

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
