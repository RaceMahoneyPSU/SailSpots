package com.example.sailspots.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.sailspots.models.SpotsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for SpotsItem.
 * This class handles all database operations (CRUD) for the 'spots' table.
 */
public class SpotsDao {
    // Database helper for creating and managing the database connection.
    private final SpotsDbHelper helper;

    /**
     * Constructor for the SpotsDao.
     * @param context The application context.
     */
    public SpotsDao(Context context) {
        this.helper = new SpotsDbHelper(context.getApplicationContext());
    }

    // --- UPSERT Operations ---
    /**
     * Inserts a new SpotsItem or updates an existing one if it has the same primary key.
     * @param item The SpotsItem to insert or update.
     * @return The row ID of the new or updated record.
     */
    public long upsert(SpotsItem item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = toValues(item);
        // CONFLICT_REPLACE will delete the old row and insert the new one.
        return db.insertWithOnConflict(
                SpotsContract.Spots.TABLE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    /**
     * Inserts or updates a list of SpotsItems in a single transaction for efficiency.
     * @param items The list of SpotsItems to upsert.
     */
    public void upsertAll(List<SpotsItem> items) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (SpotsItem item : items) {
                db.insertWithOnConflict(
                        SpotsContract.Spots.TABLE,
                        null,
                        toValues(item),
                        SQLiteDatabase.CONFLICT_REPLACE
                );
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // --- READ Operations ---

    /**
     * Retrieves all SpotsItems from the database, sorted alphabetically by name.
     * @return A list of all SpotsItems.
     */
    public List<SpotsItem> getAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<SpotsItem> results = new ArrayList<>();
        // The try-with-resources statement ensures the Cursor is closed automatically.
        try (Cursor cursor = db.query(
                SpotsContract.Spots.TABLE,
                null, // All columns
                null, // No WHERE clause
                null, // No WHERE args
                null, // No GROUP BY
                null, // No HAVING
                SpotsContract.Spots.COL_NAME + " COLLATE NOCASE ASC")) { // ORDER BY
            while (cursor.moveToNext()) {
                results.add(fromCursor(cursor));
            }
        }
        return results;
    }

    /**
     * Retrieves all favorited SpotsItems, sorted alphabetically by name.
     * @return A list of all favorite SpotsItems.
     */
    public List<SpotsItem> getFavorites() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<SpotsItem> results = new ArrayList<>();

        try (Cursor c = db.query(
                SpotsContract.Spots.TABLE,
                null,
                SpotsContract.Spots.COL_FAVORITE + "=?",
                new String[]{"1"}, // Query for favorite = true (1)
                null,
                null,
                SpotsContract.Spots.COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                results.add(fromCursor(c));
            }
        }
        return results;
    }

    /**
     * Retrieves a single SpotsItem from the database by its ID.
     * @param id The ID of the item to retrieve.
     * @return The found SpotsItem, or null if not found.
     */
    public SpotsItem getById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(
                SpotsContract.Spots.TABLE,
                null,
                SpotsContract.Spots._ID + "=?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null)) {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    // --- UPDATE Operations ---

    /**
     * Updates an existing SpotsItem in the database.
     * @param id The ID of the item to update.
     * @param item The SpotsItem object containing the new data.
     * @return The number of rows affected.
     */
    public int update(long id, SpotsItem item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = toValues(item);
        return db.update(
                SpotsContract.Spots.TABLE,
                cv,
                SpotsContract.Spots._ID + "=?",
                new String[]{String.valueOf(id)}
        );
    }

    /**
     * Updates only the 'favorite' status of a specific item.
     * @param id The ID of the item to update.
     * @param favorite The new favorite status (true or false).
     * @return The number of rows affected.
     */
    public int setFavorite(long id, boolean favorite) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SpotsContract.Spots.COL_FAVORITE, favorite ? 1 : 0);
        return db.update(
                SpotsContract.Spots.TABLE,
                cv,
                SpotsContract.Spots._ID + "=?",
                new String[]{String.valueOf(id)}
        );
    }

    // --- DELETE Operations ---

    /**
     * Deletes a single SpotsItem from the database by its ID.
     * @param id The ID of the item to delete.
     * @return The number of rows deleted.
     */
    public int delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(
                SpotsContract.Spots.TABLE,
                SpotsContract.Spots._ID + "=?",
                new String[]{String.valueOf(id)}
        );
    }

    /**
     * Deletes all records from the spots table.
     * @return The number of rows deleted.
     */
    public int deleteAll(){
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(
                SpotsContract.Spots.TABLE,
                null,
                null
        );
    }

    // --- Mapper Functions ---

    /**
     * Converts a SpotsItem object into a ContentValues object for database insertion.
     * @param s The SpotsItem object.
     * @return A ContentValues object.
     */
    private static ContentValues toValues(SpotsItem s) {
        ContentValues cv = new ContentValues();
        // The SpotsItem ID is not put here as it's handled by the primary key column.
        cv.put(SpotsContract.Spots.COL_NAME, s.getName());
        cv.put(SpotsContract.Spots.COL_TYPE, s.getType().name());
        cv.put(SpotsContract.Spots.COL_TYPE, s.getType().toString());
        cv.put(SpotsContract.Spots.COL_LAT, s.getLatitude());
        cv.put(SpotsContract.Spots.COL_LNG, s.getLongitude());
        cv.put(SpotsContract.Spots.COL_IMAGE_URL, s.getImageUrl());
        cv.put(SpotsContract.Spots.COL_FAVORITE, s.isFavorite() ? 1 : 0); // Store boolean as 1 or 0
        return cv;
    }

    /**
     * Creates a SpotsItem object from a database Cursor.
     * @param c The database Cursor, positioned at the correct row.
     * @return A populated SpotsItem object.
     */
    private static SpotsItem fromCursor(Cursor c) {
        SpotsItem item = new SpotsItem();
        // Read data from the cursor by column name and set it on the SpotsItem object.
        item.setName(c.getString(c.getColumnIndexOrThrow(SpotsContract.Spots.COL_NAME)));

        String typeStr = c.getString(c.getColumnIndexOrThrow(SpotsContract.Spots.COL_TYPE));
        item.setType(SpotsItem.Type.fromString(typeStr));

        item.setLatitude(c.getDouble(c.getColumnIndexOrThrow(SpotsContract.Spots.COL_LAT)));
        item.setLongitude(c.getDouble(c.getColumnIndexOrThrow(SpotsContract.Spots.COL_LNG)));
        item.setImageUrl(c.getString(c.getColumnIndexOrThrow(SpotsContract.Spots.COL_IMAGE_URL)));
        item.setFavorite(c.getInt(c.getColumnIndexOrThrow(SpotsContract.Spots.COL_FAVORITE)) == 1); // Convert 1 or 0 back to boolean
        return item;
    }
}
