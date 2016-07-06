package org.sil.lcroffline;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by toby on 4/07/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final String LOG_TAG = DatabaseHelper.class.getSimpleName();

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "LCRoffline.db";
    private static final String USER_TABLE_NAME = "users";
    public static final String USER_PHONE_FIELD = "phone";
    public static final String USER_NAME_FIELD = "name";
    public static final String USER_UPDATED_FIELD = "updated";
    private static final String USER_TABLE_CREATE =
            "CREATE TABLE " + USER_TABLE_NAME + " (" +
                    USER_PHONE_FIELD + " TEXT PRIMARY KEY, " +
                    USER_NAME_FIELD + " TEXT, " +
                    USER_UPDATED_FIELD + " INTEGER);";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(USER_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOG_TAG, "Upgrading Database. Dropping all data.");
        db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE_NAME);
        onCreate(db);
    }

    public Cursor getUser(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                USER_TABLE_NAME,
                new String[] {USER_NAME_FIELD, "datetime(" + USER_UPDATED_FIELD + ", 'unixepoch')"},
                USER_PHONE_FIELD + " = ?",
                new String[] {phone},
                null,
                null,
                null
        );
    }

     /**
     * Get all details from the sqlite_master table in Db.
     *
     * @return An ArrayList of the details.
     */
    private ArrayList<String[]> getDbDetails(SQLiteDatabase db) {
        Cursor c = db.rawQuery(
                "SELECT * FROM sqlite_master", null);
        ArrayList<String[]> result = new ArrayList<String[]>();
        int i = 0;
        result.add(c.getColumnNames());
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String[] temp = new String[c.getColumnCount()];
            for (i = 0; i < temp.length; i++) {
                temp[i] = c.getString(i);
            }
            result.add(temp);
        }

        return result;
    }
}
