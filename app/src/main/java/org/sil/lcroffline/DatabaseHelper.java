package org.sil.lcroffline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by toby on 4/07/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final String LOG_TAG = DatabaseHelper.class.getSimpleName();

    private static final int DATABASE_VERSION = 11;
    private static final String DATABASE_NAME = "LCRoffline.db";

    public static final String PRIMARY_KEY = "id";

    private static final String USER_TABLE_NAME = "users";
    public static final String USER_PHONE_FIELD = "phone";
    public static final String USER_NAME_FIELD = "name";
    public static final String USER_UPDATED_FIELD = "updated";
    private static final String USER_TABLE_CREATE =
            "CREATE TABLE " + USER_TABLE_NAME + " (" +
                    PRIMARY_KEY + " INTEGER PRIMARY KEY NOT NULL, " +
                    USER_PHONE_FIELD + " TEXT UNIQUE NOT NULL, " +
                    USER_NAME_FIELD + " TEXT NOT NULL, " +
                    USER_UPDATED_FIELD + " DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);";

    private static final String STATE_TABLE_NAME = "states";
    public static final String STATE_NAME_FIELD = "name";
    private static final String STATE_TABLE_CREATE =
            "CREATE TABLE " + STATE_TABLE_NAME + " (" +
                    PRIMARY_KEY + " INTEGER PRIMARY KEY NOT NULL, " +
                    STATE_NAME_FIELD + " TEXT UNIQUE NOT NULL);";

    private static final String STATE_USER_JOIN_TABLE_NAME = "states_users";
    private static final String USER_FOREIGN_KEY = "user_id";
    private static final String STATE_FOREIGN_KEY = "state_id";
    private static final String STATE_USER_JOIN_TABLE_CREATE =
            "CREATE TABLE " + STATE_USER_JOIN_TABLE_NAME + " (" +
                    STATE_FOREIGN_KEY + " INTEGER NOT NULL, " +
                    USER_FOREIGN_KEY + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + STATE_FOREIGN_KEY + ", " + USER_FOREIGN_KEY + "), " +
                    "FOREIGN KEY(" + STATE_FOREIGN_KEY + ") REFERENCES " + STATE_TABLE_NAME + "(" + PRIMARY_KEY + "), " +
                    "FOREIGN KEY(" + USER_FOREIGN_KEY + ") REFERENCES " + USER_TABLE_NAME + "(" + PRIMARY_KEY + ")" +
                    ");";

    private static final String LANGUAGE_TABLE_NAME = "languages";
    public static final String LANGUAGE_NAME_FIELD = "name";
    private static final String LANGUAGE_TABLE_CREATE =
            "CREATE TABLE " + LANGUAGE_TABLE_NAME + " (" +
                    PRIMARY_KEY + " INTEGER PRIMARY KEY NOT NULL, " +
                    STATE_FOREIGN_KEY + " INTEGER NOT NULL, " +
                    LANGUAGE_NAME_FIELD + " TEXT NOT NULL, " +
                    "FOREIGN KEY(" + STATE_FOREIGN_KEY + ") REFERENCES " + STATE_TABLE_NAME + "(" + PRIMARY_KEY + ")" +
                    ");";

    private static final String REPORT_TABLE_NAME = "reports";
    public static final String REPORT_DATE_FIELD = "date";
    public static final String REPORT_CONTENT_FIELD = "content";
    public static final String REPORT_LCR_ID_FIELD = "id_lcr";
    public static final String REPORT_FAIL_MSG_FIELD = "fail_message";
    private static final String REPORT_TABLE_CREATE =
            "CREATE TABLE " + REPORT_TABLE_NAME + " (" +
                    PRIMARY_KEY + " INTEGER PRIMARY KEY NOT NULL, " +
                    USER_FOREIGN_KEY + " INTEGER NOT NULL, " +
                    REPORT_DATE_FIELD + " DATE, " +
                    REPORT_CONTENT_FIELD + " TEXT NOT NULL, " +
                    REPORT_LCR_ID_FIELD + " INTEGER UNIQUE, " +
                    REPORT_FAIL_MSG_FIELD + " TEXT, " +
                    "FOREIGN KEY(" + USER_FOREIGN_KEY + ") REFERENCES " + USER_TABLE_NAME + "(" + PRIMARY_KEY + ")" +
                    ");";

    private static final String LANGUAGE_REPORT_JOIN_TABLE_NAME = "languages_reports";
    private static final String REPORT_FOREIGN_KEY = "Report_id";
    private static final String LANGUAGE_FOREIGN_KEY = "Language_id";
    private static final String LANGUAGE_REPORT_JOIN_TABLE_CREATE =
            "CREATE TABLE " + LANGUAGE_REPORT_JOIN_TABLE_NAME + " (" +
                    LANGUAGE_FOREIGN_KEY + " INTEGER NOT NULL, " +
                    REPORT_FOREIGN_KEY + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + LANGUAGE_FOREIGN_KEY + ", " + REPORT_FOREIGN_KEY + "), " +
                    "FOREIGN KEY(" + LANGUAGE_FOREIGN_KEY + ") REFERENCES " + LANGUAGE_TABLE_NAME + "(" + PRIMARY_KEY + "), " +
                    "FOREIGN KEY(" + REPORT_FOREIGN_KEY + ") REFERENCES " + REPORT_TABLE_NAME + "(" + PRIMARY_KEY + ")" +
                    ");";

    public static final SimpleDateFormat SQLITE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(USER_TABLE_CREATE);
        db.execSQL(STATE_TABLE_CREATE);
        db.execSQL(STATE_USER_JOIN_TABLE_CREATE);
        db.execSQL(LANGUAGE_TABLE_CREATE);
        db.execSQL(REPORT_TABLE_CREATE);
        db.execSQL(LANGUAGE_REPORT_JOIN_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOG_TAG, "Upgrading Database. Dropping all data.");
        db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + STATE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + STATE_USER_JOIN_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LANGUAGE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + REPORT_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LANGUAGE_REPORT_JOIN_TABLE_NAME);
        onCreate(db);
    }

    public Cursor getUser(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                USER_TABLE_NAME,
                new String[] {PRIMARY_KEY, USER_NAME_FIELD, USER_UPDATED_FIELD},
                USER_PHONE_FIELD + " = ?",
                new String[] {phone},
                null, null, null
        );
    }

    public Cursor getUserStates(String phone){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                STATE_TABLE_NAME +
                        " JOIN " + STATE_USER_JOIN_TABLE_NAME +
                        " ON " + STATE_TABLE_NAME + "." + PRIMARY_KEY +
                        " = " + STATE_USER_JOIN_TABLE_NAME + "." + STATE_FOREIGN_KEY +
                        " JOIN " + USER_TABLE_NAME +
                        " ON " + USER_TABLE_NAME + "." + PRIMARY_KEY +
                        " = " + STATE_USER_JOIN_TABLE_NAME + "." + USER_FOREIGN_KEY,
                new String[] {STATE_TABLE_NAME + "." + PRIMARY_KEY + " AS _id", STATE_TABLE_NAME + "." + STATE_NAME_FIELD},
                USER_PHONE_FIELD + " = ?",
                new String[] {phone},
                null, null, null
        );
    }

    public String[] getLanguageNames(long stateID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query(
                LANGUAGE_TABLE_NAME,
                new String[] {LANGUAGE_NAME_FIELD},
                STATE_FOREIGN_KEY + " = ?",
                new String[] {Long.toString(stateID)},
                null, null, null
        );
        String[] languageNames = new String[result.getCount()];
        for (int i = 0; i < languageNames.length; ++i) {
            result.moveToPosition(i);
            languageNames[i] = result.getString(0);
        }
        result.close();
        return languageNames;
    }

    public Cursor getQueuedReports(long userID) {
        Log.d(LOG_TAG, "getting queued reports for user " + userID);
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                REPORT_TABLE_NAME,
                new String[] {PRIMARY_KEY, REPORT_DATE_FIELD, REPORT_CONTENT_FIELD},
                USER_FOREIGN_KEY + " = ? AND " +
                        REPORT_LCR_ID_FIELD + " IS NULL AND " +
                        REPORT_FAIL_MSG_FIELD + " IS NULL",
                new String[] {Long.toString(userID)},
                null, null, null
        );
    }

    public Cursor getUploadedReports(long userID) {
        Log.d(LOG_TAG, "getting uploaded reports for user " + userID);
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                REPORT_TABLE_NAME,
                new String[] {REPORT_DATE_FIELD, REPORT_CONTENT_FIELD},
                USER_FOREIGN_KEY + " = ? AND " +
                        REPORT_LCR_ID_FIELD + " IS NOT NULL",
                new String[] {Long.toString(userID)},
                null, null, null
        );
    }

    public long[] getReportLanguages(long reportID) {
        Log.d(LOG_TAG, "getting languages for report " + reportID);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query(
                LANGUAGE_REPORT_JOIN_TABLE_NAME,
                new String[] {LANGUAGE_FOREIGN_KEY},
                REPORT_FOREIGN_KEY + " = ?",
                new String[] {Long.toString(reportID)},
                null, null, null
        );
        long[] languages = new long[result.getCount()];
        if (result.moveToFirst()) {
            int i = 0;
            while (!result.isAfterLast()) {
                languages[i] = result.getLong(0);
                result.moveToNext();
            }
        }
        return languages;
    }

    public long getLanguageState(long languageID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query(
                LANGUAGE_TABLE_NAME,
                new String[] {STATE_FOREIGN_KEY},
                PRIMARY_KEY + " = ?",
                new String[] {Long.toString(languageID)},
                null, null, null
        );
        if (result.moveToFirst()) {
            return result.getLong(0);
        } else {
            Log.e(LOG_TAG, "no corresponding state for language with id " + languageID);
            return -1;
        }
    }

    public boolean setUser(JSONObject user) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int userID = user.getInt(UserFragment.LCR_USER_KEY_ID);
            Cursor existing = db.query(
                    USER_TABLE_NAME,
                    new String[] {PRIMARY_KEY},
                    PRIMARY_KEY + " = ?",
                    new String[] {Integer.toString(userID)},
                    null, null, null
            );
            ContentValues values = new ContentValues();
            values.put(USER_NAME_FIELD, user.getString(UserFragment.LCR_USER_KEY_NAME));
            values.put(USER_PHONE_FIELD, user.getString(UserFragment.LCR_USER_KEY_PHONE));
            Date now = new Date();
            values.put(USER_UPDATED_FIELD, SQLITE_DATE_FORMAT.format(now));
            boolean success = true;
            if (existing.getCount() == 0) {
                // This user isn't in storage yet so use insert
                values.put(PRIMARY_KEY, userID);
                Log.d(LOG_TAG, "adding user: " + userID);
                db.insertOrThrow(USER_TABLE_NAME, null, values);
            } else {
                // This user already exists in storage, so use update.
                Log.d(LOG_TAG, "updating user: " + userID);
                int changed = db.update(
                        USER_TABLE_NAME,
                        values,
                        PRIMARY_KEY + " = ?",
                        new String[] {Integer.toString(userID)}
                );
                success = changed == 1;
            }
            existing.close();
            JSONArray states = user.getJSONArray(UserFragment.LCR_USER_KEY_STATES);
            for (int i = 0; i < states.length(); ++i) {
                success &= setState(states.getJSONObject(i), userID);
            }
            return success;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "could not set user in storage: Unable to decode JSON", e);
            return false;
        } catch (SQLException e) {
            Log.e(LOG_TAG, "could not set user in storage: Unable to perform SQL query", e);
            return false;
        }
    }

    private boolean setState(JSONObject state, int user_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int stateID = state.getInt(UserFragment.LCR_STATE_KEY_ID);
            Cursor existing = db.query(
                    STATE_TABLE_NAME,
                    new String[] {PRIMARY_KEY},
                    PRIMARY_KEY + " = ?",
                    new String[] {Integer.toString(stateID)},
                    null, null, null
            );
            ContentValues values = new ContentValues();
            values.put(STATE_NAME_FIELD, state.getString(UserFragment.LCR_STATE_KEY_NAME));
            if (existing.getCount() == 0) {
                // This state isn't in storage yet so use insert
                values.put(PRIMARY_KEY, stateID);
                Log.v(LOG_TAG, "Adding state: " + stateID);
                db.insertOrThrow(STATE_TABLE_NAME, null, values);
            } else {
                // This state already exists in storage, so use update.
                Log.v(LOG_TAG, "Updating state: " + stateID);
                db.update(
                        STATE_TABLE_NAME,
                        values,
                        PRIMARY_KEY + " = ?",
                        new String[] {Integer.toString(stateID)}
                );
            }
            existing.close();
            // now relate the state to the user
            ContentValues joinValues = new ContentValues();
            joinValues.put(STATE_FOREIGN_KEY, stateID);
            joinValues.put(USER_FOREIGN_KEY, user_id);
            Log.v(LOG_TAG, "relating state " + stateID + " with user " + user_id);
            // if the state is already related to the user then nothing will happen (CONFLICT_IGNORE)
            db.insertWithOnConflict(
                    STATE_USER_JOIN_TABLE_NAME,
                    null,
                    joinValues,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            JSONArray languages = state.getJSONArray(UserFragment.LCR_STATE_KEY_LANGUAGES);
            boolean success = true;
            for (int i = 0; i < languages.length(); ++i) {
                success &= setLanguage(languages.getJSONObject(i), stateID);
            }
            return success;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "could not set state in storage: Unable to decode JSON", e);
            return false;
        } catch (SQLException e) {
            Log.e(LOG_TAG, "could not set state in storage: Unable to perform SQL query", e);
            return false;
        }
    }

    private boolean setLanguage(JSONObject language, int state_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int languageID = language.getInt(UserFragment.LCR_LANGUAGE_KEY_ID);
            Cursor existing = db.query(
                    LANGUAGE_TABLE_NAME,
                    new String[] {PRIMARY_KEY},
                    PRIMARY_KEY + " = ?",
                    new String[] {Integer.toString(languageID)},
                    null, null, null
            );
            ContentValues values = new ContentValues();
            values.put(STATE_FOREIGN_KEY, state_id);
            values.put(LANGUAGE_NAME_FIELD, language.getString(UserFragment.LCR_LANGUAGE_KEY_NAME));
            boolean success = true;
            if (existing.getCount() == 0) {
                // This state isn't in storage yet so use insert
                values.put(PRIMARY_KEY, languageID);
                Log.v(LOG_TAG, "Adding language: " + languageID + " for state: " + state_id);
                db.insertOrThrow(LANGUAGE_TABLE_NAME, null, values);
            } else {
                // This state already exists in storage, so use update.
                Log.v(LOG_TAG, "Updating language: " + languageID + " for state: " + state_id);
                int changed = db.update(
                        LANGUAGE_TABLE_NAME,
                        values,
                        PRIMARY_KEY + " = ?",
                        new String[] {Integer.toString(languageID)}
                );
                success = changed == 1;
            }
            existing.close();
            return success;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "could not set state in storage: Unable to decode JSON", e);
            return false;
        } catch (SQLException e) {
            Log.e(LOG_TAG, "could not set state in storage: Unable to perform SQL query", e);
            return false;
        }
    }

    public boolean createReport(
            long userID,
            long stateID,
            CharSequence[] languageNames,
            Date reportDate,
            CharSequence reportContent) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_FOREIGN_KEY, userID);
        values.put(REPORT_DATE_FIELD, reportDate.getTime());
        values.put(REPORT_CONTENT_FIELD, reportContent.toString());
        long reportID = db.insert(REPORT_TABLE_NAME, null, values);
        if (reportID == -1) {
            Log.e(LOG_TAG, "Failed to save report");
            return false;
        } else {
            Log.d(LOG_TAG, "report saved with id " + reportID + " for user " + userID);
        }

        boolean success = true;
        for (CharSequence languageName : languageNames) {
            success &= addLanguageToReport(db, reportID, stateID, languageName);
        }
        return success;
    }

    private boolean addLanguageToReport(SQLiteDatabase db, long reportID, long stateID, CharSequence languageName) {
        Cursor result = db.query(
                LANGUAGE_TABLE_NAME,
                new String[] {PRIMARY_KEY},
                STATE_FOREIGN_KEY + " = ? AND " + LANGUAGE_NAME_FIELD + " = ?",
                new String[] {Long.toString(stateID), languageName.toString()},
                null, null, null
        );
        if (result.getCount() != 1) {
            Log.e(LOG_TAG, "Looking for language " + languageName + " in state " + stateID + " returned " + result.getCount() + " results.");
            return false;
        }
        result.moveToFirst();
        long languageID = result.getLong(0);
        result.close();
        ContentValues values = new ContentValues();
        values.put(REPORT_FOREIGN_KEY, reportID);
        values.put(LANGUAGE_FOREIGN_KEY, languageID);
        Log.v(LOG_TAG, "adding to report " + reportID + " language " + languageID + " (" + languageName + ")");
        long rowID = db.insertWithOnConflict(LANGUAGE_REPORT_JOIN_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return rowID != -1;
    }

    public void addLCRidToReport(long localID, int LCRid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(REPORT_LCR_ID_FIELD, LCRid);
        Log.d(LOG_TAG, "setting report " + localID + "to have LCR ID " + LCRid);
        db.update(
                REPORT_TABLE_NAME,
                values,
                PRIMARY_KEY + " = ?",
                new String[] {String.valueOf(localID)}
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
        result.add(c.getColumnNames());
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String[] temp = new String[c.getColumnCount()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = c.getString(i);
            }
            result.add(temp);
        }
        c.close();

        return result;
    }
}
