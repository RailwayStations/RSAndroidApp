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
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.DB_JSON_CONSTANTS.KEY_ID;

public class BahnhofsDbAdapter {
    private static final String DATABASE_TABLE = "bahnhoefe";
    private static final String DATABASE_TABLE_LAENDER = "laender";
    private static final String DATABASE_NAME = "bahnhoefe.db";
    private static final int DATABASE_VERSION = 5;

    private static final String CREATE_STATEMENT_1 = "CREATE TABLE " + DATABASE_TABLE + " ("
            + Constants.DB_JSON_CONSTANTS.KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
            + KEY_ID + " INTEGER, "
            + Constants.DB_JSON_CONSTANTS.KEY_TITLE + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_LAT + " REAL, "
            + Constants.DB_JSON_CONSTANTS.KEY_LON + " REAL, "
            + Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG + " TEXT)";
    private static final String CREATE_STATEMENT_2 = "CREATE INDEX " + DATABASE_TABLE + "_IDX "
            + "ON " + DATABASE_TABLE + "(" + Constants.DB_JSON_CONSTANTS.KEY_ID + ")";
    private static final String CREATE_STATEMENT_COUNTRIES = "CREATE TABLE " + DATABASE_TABLE_LAENDER + " ("
            + Constants.DB_JSON_CONSTANTS.KEY_ROWID_COUNTRIES + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
            + Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_EMAIL + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS + " TEXT)";

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
        db.beginTransaction(); // soll die Performance verbessern, heißt's.
        try {
            for (Bahnhof bahnhof : bahnhoefe) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID, bahnhof.getId());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_TITLE, bahnhof.getTitle());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_LAT, bahnhof.getLat());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_LON, bahnhof.getLon());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG, bahnhof.getPhotoflag());

                db.insert(DATABASE_TABLE, null, values);

            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertCountries(List<Country> countries) {
        db.beginTransaction(); // soll die Performance verbessern, heißt's.
        try {
            for (Country country : countries) {
                ContentValues values = new ContentValues();
                values.put(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE, country.getCountryShortCode());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME, country.getCountryName());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_EMAIL, country.getEmail());
                values.put(Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS, country.getTwitterTags());

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

    public Cursor getStationsList(boolean withPhoto) {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT rowid _id, " +
                KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE +
                " FROM " + DATABASE_TABLE
                + " WHERE " + Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG + " IS " + (withPhoto ? "NOT" : "") + " NULL"
                + " ORDER BY " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + " asc";
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
     * @param withPhoto true if stations with photo are to be queried, false if stations w/o photo
     * @return a Cursor representing the matching results
     */
    public Cursor getBahnhofsListByKeyword(String search, boolean withPhoto) {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = String.format(
                "%s  LIKE ? AND %s IS %s NULL",
                Constants.DB_JSON_CONSTANTS.KEY_TITLE,
                Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG,
                (withPhoto ? "NOT" : "")
        );

        Cursor cursor = db.query(DATABASE_TABLE,
                new String[]{
                        "rowid _id", Constants.DB_JSON_CONSTANTS.KEY_ID, Constants.DB_JSON_CONSTANTS.KEY_TITLE
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


    class BahnhoefeDbOpenHelper extends SQLiteOpenHelper {

        public BahnhoefeDbOpenHelper(Context context) {
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
        int bahnhofsnr = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_ID));
        Double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LON));
        Double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LAT));
        String photoflag = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG));
        Bahnhof bahnhof = new Bahnhof();
        bahnhof.setTitle(title);
        bahnhof.setId(bahnhofsnr);
        bahnhof.setLat(lat);
        bahnhof.setLon(lon);
        bahnhof.setPhotoflag(photoflag);
        return bahnhof;
    }

    @NonNull
    private Country createCountryFromCursor(@NonNull Cursor cursor) {
        String countryShortCode = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE));
        String countryName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME));
        String email = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_EMAIL));
        String twitterTags = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS));
        Country country = new Country();
        country.setCountryShortCode(countryShortCode);
        country.setCountryName(countryName);
        country.setEmail(email);
        country.setTwitterTags(twitterTags);
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

    public Bahnhof fetchBahnhofByBahnhofId(long id) {
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

    // Getting All Bahnhoefe
    public List<Bahnhof> getAllBahnhoefe(boolean withPhoto) {
        List<Bahnhof> bahnhofList = new ArrayList<Bahnhof>();
        // Select All Query without any baseLocationValues (myCurrentLocation)
        String selectQuery = "SELECT " +
                KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LAT + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LON + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG +
                " FROM " + DATABASE_TABLE
                + " WHERE " + Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG + " IS " + (withPhoto ? "NOT" : "") + " NULL";
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

        // return bahnhof list
        return bahnhofList;
    }

    // Getting All Bahnhoefe
    public List<Bahnhof> getBahnhoefeByLatLngRectangle(LatLng position, boolean withPhoto) {
        double lat = position.latitude;
        double lng = position.longitude;
        List<Bahnhof> bahnhofList = new ArrayList<Bahnhof>();
        // Select All Query with rectangle - might be later change with it
        String selectQuery = "SELECT " + KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LAT + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LON + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG +
                " FROM " + DATABASE_TABLE + " where " + Constants.DB_JSON_CONSTANTS.KEY_LAT + " < " + (lat + 0.5) + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LAT + " > " + (lat - 0.5)
                + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON + " < " + (lng + 0.5) + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON + " > " + (lng - 0.5)
                + " AND " + Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG + " IS " + (withPhoto ? "NOT" : "") + " NULL";

        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bahnhof bahnhof = createBahnhofFromCursor(cursor);
                // Adding bahnhof to list
                bahnhofList.add(bahnhof);

            } while (cursor.moveToNext());
        }
        cursor.close();

        // returns bahnhof list
        return bahnhofList;
    }

    // Getting All Countries
    public List<Country> getAllCountries() {
        List<Country> countryList = new ArrayList<Country>();
        // Select All Query without any baseLocationValues (myCurrentLocation)
        String selectQueryCountries = "SELECT " +
                Constants.DB_JSON_CONSTANTS.KEY_COUNTRYSHORTCODE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_COUNTRYNAME + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_EMAIL + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TWITTERTAGS +
                " FROM " + DATABASE_TABLE_LAENDER;

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

        // return country list
        return countryList;
    }


}
