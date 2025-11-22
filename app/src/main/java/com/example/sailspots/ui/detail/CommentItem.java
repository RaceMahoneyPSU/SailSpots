package com.example.sailspots.ui.detail;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A data model class (POJO) representing a single comment or review.
 * It holds all the relevant information for one review item.
 */
public class CommentItem {
    // --- Final properties ensure the object is immutable after creation ---
    public final String id;         // The unique ID of the Firestore document.
    public final String author;     // The name of the person who wrote the comment.
    public final int rating;       // The star rating (e.g., 1 to 5).
    public final String text;       // The content of the comment.
    public final String dateLabel;  // A formatted, human-readable date string (e.g., "Jan 8").
    public final Timestamp createdAt;  // The exact time the comment was created, for sorting.

    /**
     * Constructor to create a new CommentItem.
     * @param id The document ID from Firestore.
     * @param author The author's name.
     * @param rating The star rating.
     * @param text The review text.
     * @param dateLabel A pre-formatted date string (though it's overwritten by formatDate).
     * @param createdAt The raw timestamp from Firestore.
     */
    public CommentItem(String id, String author, int rating, String text, String dateLabel, Timestamp createdAt) {
        this.id = id;
        this.author = author;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
        // The dateLabel is generated here, ensuring it's always correctly formatted from the Timestamp.
        this.dateLabel = formatDate(createdAt);
    }

    /**
     * A private helper method to convert a Firestore Timestamp into a simple, readable date string.
     * @param ts The Timestamp object from Firestore.
     * @return A formatted date string like "MMM d" (e.g., "Jan 8"), or an empty string if the timestamp is null.
     */
    private static String formatDate(Timestamp ts) {
        // Return an empty string if the timestamp is null to avoid crashes.
        if (ts == null) return "";
        // Convert the Firestore Timestamp to a standard Java Date object.
        Date date = ts.toDate();
        // Define the desired date format (e.g., "Jan 8").
        DateFormat df = new SimpleDateFormat("MMM d", Locale.getDefault());
        // Format the date and return the string.
        return df.format(date);
    }

    /**
     * A static factory method to create a CommentItem directly from a Firestore DocumentSnapshot.
     * This encapsulates the logic for parsing the Firestore document.
     * @param doc The DocumentSnapshot retrieved from Firestore.
     * @return A new, fully populated CommentItem object.
     */
    public static CommentItem fromSnapshot(DocumentSnapshot doc) {
        // Get the unique ID of the document.
        String id = doc.getId();
        // Safely get the fields from the document, providing default values if they are missing.
        String author = doc.getString("authorName");
        String text = doc.getString("text");
        // Firestore stores numbers as Long, so we need to get it as a Long and then convert to int.
        Long ratingLong = doc.getLong("rating");
        int rating = ratingLong != null ? ratingLong.intValue() : 0;
        Timestamp createdAt = doc.getTimestamp("createdAt");

        // --- Data Sanitization ---
        // Ensure that we don't have null values for author or text.
        if (author == null) author = "Unknown";
        if (text == null) text = "";

        // Create and return a new CommentItem using the parsed data.
        return new CommentItem(id, author, rating, text, "", createdAt);
    }
}
