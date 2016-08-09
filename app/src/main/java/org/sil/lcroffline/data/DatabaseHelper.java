package org.sil.lcroffline.data;

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
import org.sil.lcroffline.UserFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.sil.lcroffline.data.DatabaseContract.LanguageEntry;
import org.sil.lcroffline.data.DatabaseContract.ReportEntry;
import org.sil.lcroffline.data.DatabaseContract.StateUserJoinEntry;
import org.sil.lcroffline.data.DatabaseContract.UserEntry;

import static org.sil.lcroffline.data.DatabaseContract.*;

/**
 * Created by toby on 4/07/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final String LOG_TAG = DatabaseHelper.class.getSimpleName();

    private static final int DATABASE_VERSION = 12;
    private static final String DATABASE_NAME = "LCRoffline.db";

    private static final String USER_TABLE_CREATE =
            "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +
                    UserEntry._ID + " INTEGER PRIMARY KEY NOT NULL, " +
                    UserEntry.COLUMN_PHONE + " TEXT UNIQUE NOT NULL, " +
                    UserEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                    UserEntry.COLUMN_UPDATED + " DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);";

    private static final String STATE_TABLE_CREATE =
            "CREATE TABLE " + StateEntry.TABLE_NAME + " (" +
                    StateEntry._ID + " INTEGER PRIMARY KEY NOT NULL, " +
                    StateEntry.COLUMN_NAME + " TEXT UNIQUE NOT NULL);";

    private static final String STATE_USER_JOIN_TABLE_CREATE =
            "CREATE TABLE " + StateUserJoinEntry.TABLE_NAME + " (" +
                    StateUserJoinEntry.COLUMN_STATE_KEY + " INTEGER NOT NULL, " +
                    StateUserJoinEntry.COLUMN_USER_KEY + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + StateUserJoinEntry.COLUMN_STATE_KEY + ", " + StateUserJoinEntry.COLUMN_USER_KEY + "), " +
                    "FOREIGN KEY(" + StateUserJoinEntry.COLUMN_STATE_KEY + ") REFERENCES " + StateEntry.TABLE_NAME + "(" + StateEntry._ID + "), " +
                    "FOREIGN KEY(" + StateUserJoinEntry.COLUMN_USER_KEY + ") REFERENCES " + UserEntry.TABLE_NAME + "(" + UserEntry._ID + ")" +
                    ");";

    private static final String LANGUAGE_TABLE_CREATE =
            "CREATE TABLE " + LanguageEntry.TABLE_NAME + " (" +
                    LanguageEntry._ID + " INTEGER PRIMARY KEY NOT NULL, " +
                    StateUserJoinEntry.COLUMN_STATE_KEY + " INTEGER NOT NULL, " +
                    LanguageEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                    "FOREIGN KEY(" + LanguageEntry.COLUMN_STATE_KEY + ") REFERENCES " + StateEntry.TABLE_NAME + "(" + StateEntry._ID + ")" +
                    ");";

    private static final String REPORT_TABLE_CREATE =
            "CREATE TABLE " + ReportEntry.TABLE_NAME + " (" +
                    ReportEntry._ID + " INTEGER PRIMARY KEY NOT NULL, " +
                    ReportEntry.COLUMN_USER_KEY + " INTEGER NOT NULL, " +
                    ReportEntry.COLUMN_DATE + " DATE, " +
                    ReportEntry.COLUMN_CONTENT + " TEXT NOT NULL, " +
                    ReportEntry.COLUMN_LCR_ID + " INTEGER UNIQUE, " +
                    ReportEntry.COLUMN_FAIL_MSG + " TEXT, " +
                    "FOREIGN KEY(" + ReportEntry.COLUMN_USER_KEY + ") REFERENCES " + UserEntry.TABLE_NAME + "(" + UserEntry._ID + ")" +
                    ");";

    private static final String LANGUAGE_REPORT_JOIN_TABLE_CREATE =
            "CREATE TABLE " + LanguageReportJoinEntry.TABLE_NAME + " (" +
                    LanguageReportJoinEntry.COLUMN_LANGUAGE_KEY + " INTEGER NOT NULL, " +
                    LanguageReportJoinEntry.COLUMN_REPORT_KEY + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + LanguageReportJoinEntry.COLUMN_LANGUAGE_KEY + ", " + LanguageReportJoinEntry.COLUMN_REPORT_KEY + "), " +
                    "FOREIGN KEY(" + LanguageReportJoinEntry.COLUMN_LANGUAGE_KEY + ") REFERENCES " + LanguageEntry.TABLE_NAME + "(" + LanguageEntry._ID + "), " +
                    "FOREIGN KEY(" + LanguageReportJoinEntry.COLUMN_REPORT_KEY + ") REFERENCES " + ReportEntry.TABLE_NAME + "(" + ReportEntry._ID + ")" +
                    ");";

    public static final SimpleDateFormat SQLITE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public DatabaseHelper(Context context) {
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
        db.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StateEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StateUserJoinEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LanguageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ReportEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LanguageReportJoinEntry.TABLE_NAME);
        onCreate(db);
    }

    public Cursor getUser(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                UserEntry.TABLE_NAME,
                new String[] {UserEntry._ID, UserEntry.COLUMN_NAME, UserEntry.COLUMN_UPDATED},
                UserEntry.COLUMN_PHONE + " = ?",
                new String[] {phone},
                null, null, null
        );
    }

    public Cursor getUserStates(String phone){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                StateEntry.TABLE_NAME +
                        " JOIN " + StateUserJoinEntry.TABLE_NAME +
                        " ON " + StateEntry.TABLE_NAME + "." + StateEntry._ID +
                        " = " + StateUserJoinEntry.TABLE_NAME + "." + StateUserJoinEntry.COLUMN_STATE_KEY +
                        " JOIN " + UserEntry.TABLE_NAME +
                        " ON " + UserEntry.TABLE_NAME + "." + UserEntry._ID +
                        " = " + StateUserJoinEntry.TABLE_NAME + "." + StateUserJoinEntry.COLUMN_USER_KEY,
                new String[] {StateEntry.TABLE_NAME + "." + StateEntry._ID + " AS _id", StateEntry.TABLE_NAME + "." + StateEntry.COLUMN_NAME},
                UserEntry.COLUMN_PHONE + " = ?",
                new String[] {phone},
                null, null, null
        );
    }

    public String[] getLanguageNames(long stateID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query(
                LanguageEntry.TABLE_NAME,
                new String[] {LanguageEntry.COLUMN_NAME},
                LanguageEntry.COLUMN_STATE_KEY + " = ?",
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
                ReportEntry.TABLE_NAME,
                new String[] {ReportEntry._ID, ReportEntry.COLUMN_DATE, ReportEntry.COLUMN_CONTENT},
                ReportEntry.COLUMN_USER_KEY + " = ? AND " +
                        ReportEntry.COLUMN_LCR_ID + " IS NULL AND " +
                        ReportEntry.COLUMN_FAIL_MSG + " IS NULL",
                new String[] {Long.toString(userID)},
                null, null, null
        );
    }

    public Cursor getUploadedReports(long userID) {
        Log.d(LOG_TAG, "getting uploaded reports for user " + userID);
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                ReportEntry.TABLE_NAME,
                new String[] {ReportEntry.COLUMN_DATE, ReportEntry.COLUMN_CONTENT},
                ReportEntry.COLUMN_USER_KEY + " = ? AND " +
                        ReportEntry.COLUMN_LCR_ID + " IS NOT NULL",
                new String[] {Long.toString(userID)},
                null, null, null
        );
    }

    public long[] getReportLanguages(long reportID) {
        Log.d(LOG_TAG, "getting languages for report " + reportID);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query(
                LanguageReportJoinEntry.TABLE_NAME,
                new String[] {LanguageReportJoinEntry.COLUMN_LANGUAGE_KEY},
                LanguageReportJoinEntry.COLUMN_REPORT_KEY + " = ?",
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
                LanguageEntry.TABLE_NAME,
                new String[] {LanguageEntry.COLUMN_STATE_KEY},
                LanguageEntry._ID + " = ?",
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
                    UserEntry.TABLE_NAME,
                    new String[] {UserEntry._ID},
                    UserEntry._ID + " = ?",
                    new String[] {Integer.toString(userID)},
                    null, null, null
            );
            ContentValues values = new ContentValues();
            values.put(UserEntry.COLUMN_NAME, user.getString(UserFragment.LCR_USER_KEY_NAME));
            values.put(UserEntry.COLUMN_PHONE, user.getString(UserFragment.LCR_USER_KEY_PHONE));
            Date now = new Date();
            values.put(UserEntry.COLUMN_UPDATED, SQLITE_DATE_FORMAT.format(now));
            boolean success = true;
            if (existing.getCount() == 0) {
                // This user isn't in storage yet so use insert
                values.put(UserEntry._ID, userID);
                Log.d(LOG_TAG, "adding user: " + userID);
                db.insertOrThrow(UserEntry.TABLE_NAME, null, values);
            } else {
                // This user already exists in storage, so use update.
                Log.d(LOG_TAG, "updating user: " + userID);
                int changed = db.update(
                        UserEntry.TABLE_NAME,
                        values,
                        UserEntry._ID + " = ?",
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
                    StateEntry.TABLE_NAME,
                    new String[] {StateEntry._ID},
                    StateEntry._ID + " = ?",
                    new String[] {Integer.toString(stateID)},
                    null, null, null
            );
            ContentValues values = new ContentValues();
            values.put(StateEntry.COLUMN_NAME, state.getString(UserFragment.LCR_STATE_KEY_NAME));
            if (existing.getCount() == 0) {
                // This state isn't in storage yet so use insert
                values.put(StateEntry._ID, stateID);
                Log.v(LOG_TAG, "Adding state: " + stateID);
                db.insertOrThrow(StateEntry.TABLE_NAME, null, values);
            } else {
                // This state already exists in storage, so use update.
                Log.v(LOG_TAG, "Updating state: " + stateID);
                db.update(
                        StateEntry.TABLE_NAME,
                        values,
                        StateEntry._ID + " = ?",
                        new String[] {Integer.toString(stateID)}
                );
            }
            existing.close();
            // now relate the state to the user
            ContentValues joinValues = new ContentValues();
            joinValues.put(StateUserJoinEntry.COLUMN_STATE_KEY, stateID);
            joinValues.put(StateUserJoinEntry.COLUMN_USER_KEY, user_id);
            Log.v(LOG_TAG, "relating state " + stateID + " with user " + user_id);
            // if the state is already related to the user then nothing will happen (CONFLICT_IGNORE)
            db.insertWithOnConflict(
                    StateUserJoinEntry.TABLE_NAME,
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
                    LanguageEntry.TABLE_NAME,
                    new String[] {LanguageEntry._ID},
                    LanguageEntry._ID + " = ?",
                    new String[] {Integer.toString(languageID)},
                    null, null, null
            );
            ContentValues values = new ContentValues();
            values.put(LanguageEntry.COLUMN_STATE_KEY, state_id);
            values.put(LanguageEntry.COLUMN_NAME, language.getString(UserFragment.LCR_LANGUAGE_KEY_NAME));
            boolean success = true;
            if (existing.getCount() == 0) {
                // This state isn't in storage yet so use insert
                values.put(LanguageEntry._ID, languageID);
                Log.v(LOG_TAG, "Adding language: " + languageID + " for state: " + state_id);
                db.insertOrThrow(LanguageEntry.TABLE_NAME, null, values);
            } else {
                // This state already exists in storage, so use update.
                Log.v(LOG_TAG, "Updating language: " + languageID + " for state: " + state_id);
                int changed = db.update(
                        LanguageEntry.TABLE_NAME,
                        values,
                        LanguageEntry._ID + " = ?",
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
        values.put(ReportEntry.COLUMN_USER_KEY, userID);
        values.put(ReportEntry.COLUMN_DATE, reportDate.getTime());
        values.put(ReportEntry.COLUMN_CONTENT, reportContent.toString());
        long reportID = db.insert(ReportEntry.TABLE_NAME, null, values);
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
                LanguageEntry.TABLE_NAME,
                new String[] {LanguageEntry._ID},
                LanguageEntry.COLUMN_STATE_KEY + " = ? AND " + LanguageEntry.COLUMN_NAME + " = ?",
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
        values.put(LanguageReportJoinEntry.COLUMN_REPORT_KEY, reportID);
        values.put(LanguageReportJoinEntry.COLUMN_LANGUAGE_KEY, languageID);
        Log.v(LOG_TAG, "adding to report " + reportID + " language " + languageID + " (" + languageName + ")");
        long rowID = db.insertWithOnConflict(LanguageReportJoinEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return rowID != -1;
    }

    public void addLCRidToReport(long localID, int LCRid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ReportEntry.COLUMN_LCR_ID, LCRid);
        Log.d(LOG_TAG, "setting report " + localID + "to have LCR ID " + LCRid);
        db.update(
                ReportEntry.TABLE_NAME,
                values,
                ReportEntry._ID + " = ?",
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
