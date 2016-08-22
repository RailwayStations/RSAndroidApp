package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.JSONTask;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

import static com.google.android.gms.analytics.internal.zzy.l;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.R.id.search;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.DB_JSON_CONSTANTS.KEY_ID;

/**
 * Created by android_oma on 29.05.16.
 */

public class BahnhofsDbAdapter {
    private static final String DATABASE_TABLE = "bahnhoefe";
    private static final String DATABASE_NAME = "bahnhoefe.db";
    private static final int DATABASE_VERSION = 2;

    private static final String CREATE_STATEMENT = "CREATE TABLE " + DATABASE_TABLE + " ("
            + Constants.DB_JSON_CONSTANTS.KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
            + KEY_ID + " INTEGER, "
            + Constants.DB_JSON_CONSTANTS.KEY_TITLE + " TEXT, "
            + Constants.DB_JSON_CONSTANTS.KEY_LAT + " REAL, "
            + Constants.DB_JSON_CONSTANTS.KEY_LON + " REAL, "
            + Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG + " TEXT)";




    private static final String DROP_STATEMENT = "DROP TABLE IF EXISTS " + DATABASE_TABLE;


    private Context context;
    private BahnhoefeDbOpenHelper dbHelper;
    private SQLiteDatabase db;

    public BahnhofsDbAdapter(Context context){
        this.context = context;
    }

    public void open(){
        dbHelper = new BahnhoefeDbOpenHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void close(){
        db.close();
        dbHelper.close();
    }

    public void insertBahnhoefe(List<Bahnhof> bahnhoefe, JSONTask jsonTask){
        deleteBahnhoefe();
        for (Bahnhof bahnhof : bahnhoefe){
            ContentValues values = new ContentValues();
            values.put(KEY_ID, bahnhof.getId());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_TITLE,bahnhof.getTitle());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_LAT, bahnhof.getLat());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_LON, bahnhof.getLon());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG, "N");

            db.insert(DATABASE_TABLE, null,values);
            jsonTask.pub("added: " +  bahnhof.getId());

        }
    }

    public void insertBahnhoefe(List<Bahnhof> bahnhoefe){
        deleteBahnhoefe();
        for (Bahnhof bahnhof : bahnhoefe){
            ContentValues values = new ContentValues();
            values.put(KEY_ID, bahnhof.getId());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_TITLE,bahnhof.getTitle());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_LAT, bahnhof.getLat());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_LON, bahnhof.getLon());
            values.put(Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG, "N");

            db.insert(DATABASE_TABLE, null,values);

        }
    }


    private void deleteBahnhoefe(){
        db.delete(DATABASE_TABLE,null,null);
    }

    public Cursor  getStationsList() {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery =  "SELECT rowid _id, " +
                KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE +
                " FROM " + DATABASE_TABLE + " ORDER BY " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + " asc";
        Log.d("Bahnhoefe",selectQuery.toString());


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


    private Cursor readBahnhoefe(){
        return db.query(DATABASE_TABLE,null, null, null, null, null, Constants.DB_JSON_CONSTANTS.KEY_TITLE + " asc");
    }


    public Cursor getBahnhofsListByKeyword(String search) {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT rowid _id, " +
                Constants.DB_JSON_CONSTANTS.KEY_ID + "," +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE +
                " FROM " + DATABASE_TABLE +
                " WHERE " + Constants.DB_JSON_CONSTANTS.KEY_TITLE + "  LIKE  '%" + search + "%' " +
                " ORDER BY " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + " asc";


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


    class BahnhoefeDbOpenHelper extends SQLiteOpenHelper {

        public BahnhoefeDbOpenHelper(Context context){
            super(context,DATABASE_NAME,null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            Log.d("dbCreation", CREATE_STATEMENT);
            db.execSQL(CREATE_STATEMENT);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            Log.w("BahnhofsDbAdapter", "Upgrade der Datenbank von Version" + oldVersion + " nach " + newVersion + " wird durchgeführt.");
            db.execSQL(DROP_STATEMENT);
            onCreate(db);
        }



        }

    public Bahnhof fetchBahnhof(long id){
        Cursor cursor = db.query(DATABASE_TABLE, null, Constants.DB_JSON_CONSTANTS.KEY_ROWID + "=?", new String[] {
                id + ""},null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
            String title = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_TITLE));
            int bahnhofsnr = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_ID));
            Float lon = cursor.getFloat(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LON));
            Float lat = cursor.getFloat(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_LAT));
            String photoflag = cursor.getString(cursor.getColumnIndexOrThrow(Constants.DB_JSON_CONSTANTS.KEY_PHOTOFLAG));
            Bahnhof bahnhof = new Bahnhof();
            bahnhof.setTitle(title);
            bahnhof.setId(bahnhofsnr);
            bahnhof.setLat(lat);
            bahnhof.setLon(lon);
            bahnhof.setPhotoflag(photoflag);
            cursor.close();
            return bahnhof;
        } else {
            throw new RuntimeException("Bahnhof wurde nicht gefunden");

        }

    }
    // Getting All Bahnhoefe
    public List<Bahnhof> getAllBahnhoefe() {
        List<Bahnhof> bahnhofList = new ArrayList<Bahnhof>();
        // Select All Query without any baseLocationValues (myCurrentLocation)
        String selectQuery = "SELECT " +
                KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LAT + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LON +
                " FROM " + DATABASE_TABLE;
        Log.d("Bahnhoefe",selectQuery.toString());
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bahnhof bahnhof = new Bahnhof();
                bahnhof.setId(Integer.parseInt(cursor.getString(0)));
                bahnhof.setTitle(cursor.getString(1));
                bahnhof.setLat(Float.parseFloat(cursor.getString(2)));
                bahnhof.setLon(Float.parseFloat(cursor.getString(3)));
                // Adding bahnhof to list
                bahnhofList.add(bahnhof);

            } while (cursor.moveToNext());
        }else {
            throw new RuntimeException("keine Bahnhoefe geladen");
        }
        cursor.close();

        // return bahnhof list
        return bahnhofList;
    }

    // Getting All Bahnhoefe
    public List<Bahnhof> getBahnhoefeByLatLngRectangle(double lat, double lng) {
        List<Bahnhof> bahnhofList = new ArrayList<Bahnhof>();
        // Select All Query with rectangle - might be later change with it
        /*String selectQuery = "SELECT " + KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LAT + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LON +
                " FROM " + DATABASE_TABLE + " where "+ Constants.DB_JSON_CONSTANTS.KEY_LAT +" < "+ (lat +0.5) + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LAT +" > "+ (lat -0.5)
                + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON +" < "+ (lng +0.5) + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON +" > "+ (lng -   0.5) ;*/

        String selectQuery = "SELECT " + KEY_ID + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_TITLE + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LAT + ", " +
                Constants.DB_JSON_CONSTANTS.KEY_LON +
                " FROM " + DATABASE_TABLE + " where "+ Constants.DB_JSON_CONSTANTS.KEY_LAT + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LAT +
                 " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON + " AND " + Constants.DB_JSON_CONSTANTS.KEY_LON;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bahnhof bahnhof = new Bahnhof();
                bahnhof.setId(Integer.parseInt(cursor.getString(0)));
                bahnhof.setTitle(cursor.getString(1));
                bahnhof.setLat(Float.parseFloat(cursor.getString(2)));
                bahnhof.setLon(Float.parseFloat(cursor.getString(3)));
                // Adding bahnhof to list
                bahnhofList.add(bahnhof);

            } while (cursor.moveToNext());
        }else {
            throw new RuntimeException("keine Bahnhoefe geladen");
        }
        cursor.close();

        // returns bahnhof list
        return bahnhofList;
    }

}
