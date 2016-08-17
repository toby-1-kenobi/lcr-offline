package org.sil.lcroffline.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by toby on 17/08/16.
 */
public class DataProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    DatabaseHelper mDbHelper;

    static final int USER = 100;
    static final int USER_BY_ACC_NAME = 101;
    static final int REPORT = 300;

    static UriMatcher buildUriMatcher() {

        // The code passed into the UriMatcher constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Use the addURI function to match each of the types.
        String authority = DatabaseContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, DatabaseContract.PATH_USERS, USER);
        matcher.addURI(authority, DatabaseContract.PATH_USERS + DatabaseContract.UserEntry.PATH_BY_PHONE + "/*", USER_BY_ACC_NAME);
        matcher.addURI(authority, DatabaseContract.PATH_REPORTS, REPORT);

        return matcher;

    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case USER:
                return DatabaseContract.UserEntry.CONTENT_TYPE;
            case USER_BY_ACC_NAME:
                return DatabaseContract.UserEntry.CONTENT_ITEM_TYPE;
            case REPORT:
                return DatabaseContract.ReportEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case USER: {
                retCursor = mDbHelper.getReadableDatabase().query(
                        DatabaseContract.UserEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case USER_BY_ACC_NAME: {
                retCursor = mDbHelper.getUser(DatabaseContract.UserEntry.getPhoneFromUri(uri));
                break;
            }
            case REPORT: {
                retCursor = mDbHelper.getReadableDatabase().query(
                        DatabaseContract.ReportEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case USER: {
                long _id = db.insert(DatabaseContract.UserEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = DatabaseContract.UserEntry.buildUserUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case REPORT: {
                long _id = db.insert(DatabaseContract.ReportEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = DatabaseContract.ReportEntry.buildReportUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);

        // A null value deletes all rows.
        int deletedCount;
        if (selection == null) selection = "1";
        switch (match) {
            case USER: {
                deletedCount = db.delete(
                        DatabaseContract.UserEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case REPORT: {
                deletedCount = db.delete(
                        DatabaseContract.ReportEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (deletedCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deletedCount;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);

        int updatedCount;
        switch (match) {
            case USER: {
                updatedCount = db.update(
                        DatabaseContract.UserEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            }
            case REPORT: {
                updatedCount = db.update(
                        DatabaseContract.ReportEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (updatedCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updatedCount;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case REPORT:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(DatabaseContract.ReportEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
