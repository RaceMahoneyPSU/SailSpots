package com.example.sailspots.data;

import android.provider.BaseColumns;

/**
 * Defines the database schema constants, such as table and column names.
 * This class acts as a contract for the database structure, ensuring consistency.
 */
public final class SpotsContract {

    // Private constructor to prevent instantiation of the contract class.
    private SpotsContract() {}

    // Database name and version constants.
    public static final String DB_NAME = "sailspots.db";
    public static final int DB_VERSION = 1;

    /**
     * Defines the contents of the 'spots' table.
     * Implements BaseColumns to include the standard _ID and _COUNT columns.
     */
    public static final class Spots implements BaseColumns {
        // Table name
        public static final String TABLE = "spots";

        // Column names
        public static final String COL_NAME       = "name";
        public static final String COL_TYPE       = "type";       // TEXT: RAMP | MARINA | BEACH
        public static final String COL_LAT        = "latitude";   // REAL
        public static final String COL_LNG        = "longitude";  // REAL
        public static final String COL_IMAGE_URL  = "image_url";  // TEXT (nullable)
        public static final String COL_FAVORITE   = "favorite";   // INTEGER (0 for false, 1 for true)

        // SQL statement to create the 'spots' table.
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_NAME + " TEXT NOT NULL, " +
                        COL_TYPE + " TEXT NOT NULL, " +
                        COL_LAT + " REAL NOT NULL, " +
                        COL_LNG + " REAL NOT NULL, " +
                        COL_IMAGE_URL + " TEXT, " +
                        COL_FAVORITE + " INTEGER NOT NULL DEFAULT 0, " +
                        // Defines a unique constraint to prevent duplicate entries.
                        "UNIQUE(" + COL_NAME + ", " + COL_LAT + ", " + COL_LNG + ") ON CONFLICT REPLACE" +
                        ");";

        // SQL statement to create an index on the 'favorite' column for faster queries.
        public static final String SQL_INDEX_FAVORITE =
                "CREATE INDEX idx_spots_favorite ON " + TABLE + " (" + COL_FAVORITE + ");";

        // SQL statement to create an index on the 'type' column for faster queries.
        public static final String SQL_INDEX_TYPE =
                "CREATE INDEX idx_spots_type ON " + TABLE + " (" + COL_TYPE + ");";

        // SQL statement to drop the 'spots' table.
        public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE;
    }
}
