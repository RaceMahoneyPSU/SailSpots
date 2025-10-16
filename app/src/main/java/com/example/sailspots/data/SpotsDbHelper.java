package com.example.sailspots.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages database creation and version management for the application's SQLite database.
 */
public class SpotsDbHelper extends SQLiteOpenHelper {

    /**
     * Constructor for the database helper.
     * @param context The application context.
     */
    public SpotsDbHelper(Context context) {
        // Calls the superclass constructor, passing the database name and version from the contract.
        super(context, SpotsContract.DB_NAME, null, SpotsContract.DB_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * This is where the creation of tables and initial population of data should happen.
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Executes the SQL statements from the contract to create tables and indexes.
        db.execSQL(SpotsContract.Spots.SQL_CREATE);
        db.execSQL(SpotsContract.Spots.SQL_INDEX_FAVORITE);
        db.execSQL(SpotsContract.Spots.SQL_INDEX_TYPE);
    }

    /**
     * Called when the database needs to be upgraded.
     * This method will only be called if the database version number is increased.
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // A simple upgrade policy: drop the existing table and recreate it.
        // WARNING: This will delete all existing user data.
        db.execSQL(SpotsContract.Spots.SQL_DROP);
        onCreate(db);
    }
}
