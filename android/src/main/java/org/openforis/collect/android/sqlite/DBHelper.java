package org.openforis.collect.android.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.openforis.collect.android.sqlite.DataModel.UserData;

import java.util.ArrayList;

public class DBHelper {

    public static final String ID = "id";
    public static final String EMAIL = "email";
    public static final String FULL_NAME = "full_name";

    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "userDetails.db";
    private static final int DATABASE_VERSION = 1;

    private static final String USER_TABLE = "user_table";

    private final Context mContext;
    private DatabaseHelper mDbHelper;

    private static final String CREATE_USER_TABLE = "create table "
            + USER_TABLE + " (" + ID
            + " integer primary key autoincrement, "
            + EMAIL + " text not null unique, "
            + FULL_NAME + " text not null );";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_USER_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i1) {
            db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE);
            onCreate(db);
        }
    }

    public DBHelper(SQLiteDatabase mDd, Context mContext) {
        this.mDb = mDd;
        this.mContext = mContext;
    }

    public DBHelper(Context ctx) {
        mContext = ctx;
        mDbHelper = new DatabaseHelper(mContext);
    }

    public DBHelper open() throws SQLException {
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public boolean insertUser(String email, String fullName) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EMAIL, email);
        cv.put(FULL_NAME, fullName);

        long result = db.insert(USER_TABLE, null, cv);
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public ArrayList<String> retriverUserFullName() throws SQLException {
        mDb = mDbHelper.getReadableDatabase();
        String sqlQuery = "SELECT * FROM " + USER_TABLE;
        Cursor cursor = mDb.rawQuery(sqlQuery, null);
        ArrayList<String> userDataList = new ArrayList<String>();

        if (cursor.moveToFirst()){
            do {
                String fullName = cursor.getString(cursor.getColumnIndex(FULL_NAME));
                userDataList.add(fullName);
            } while (cursor.moveToNext());
        }

        return userDataList;
    }

    public ArrayList<UserData> retrieveAllUser() throws SQLException {
        mDb = mDbHelper.getReadableDatabase();
        String sqlQuery = "SELECT * FROM " + USER_TABLE;
        Cursor cursor = mDb.rawQuery(sqlQuery, null);
        ArrayList<UserData> userDataList = new ArrayList<UserData>();

        if (cursor.moveToFirst()){
            do {
                String id = cursor.getString(cursor.getColumnIndex(ID));
                String email = cursor.getString(cursor.getColumnIndex(EMAIL));
                String fullName = cursor.getString(cursor.getColumnIndex(FULL_NAME));
                userDataList.add(new UserData(id, email, fullName));
            } while (cursor.moveToNext());
        }

        return userDataList;
    }
}
