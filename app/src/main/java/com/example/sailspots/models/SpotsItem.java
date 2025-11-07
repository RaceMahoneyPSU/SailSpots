package com.example.sailspots.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.Locale;

/**
 * A data model class representing a single boating spot for database storage. * Implements Serializable to allow instances to be passed between activities via Intents.
 */
public class SpotsItem implements Serializable {

    /**
     * Defines the type of the boating spot.
     */
    public enum Type {
        RAMP, MARINA, BEACH;

        /**
         * Converts a string to a corresponding Type enum, defaulting to RAMP.
         * @param s The input string (e.g., "MARINA").
         * @return The matching Type enum.
         */
        public static Type fromString(String s) {
            if (s == null) return RAMP;
            switch (s.trim().toUpperCase(Locale.US)) {
                case "MARINA": return MARINA;
                case "BEACH":  return BEACH;
                default:       return RAMP;
            }
        }

        /**
         * Provides a user-friendly string representation (e.g., "Ramp").
         */
        @Override
        public String toString() {
            String name = name().toLowerCase(Locale.US);
            return name.substring(0, 1).toUpperCase(Locale.US) + name.substring(1);
        }
    }

    // --- Member Variables ---
    @DocumentId
    private String id;           // The unique database ID (primary key).
    private String placeId;    // The Google Places API ID for unique identification.
    private String name;
    private String address;
    private Type type;         // The type of spot (RAMP, MARINA, or BEACH).
    private double latitude;   // GPS coordinate.
    private double longitude;  // GPS coordinate.
    private boolean favorite;  // User-bookmarked status.

    /**
     * Full constructor to create a new SpotsItem.
     */
    public SpotsItem(String id,
                     String placeId,
                     String name,
                     String address,
                     Type type,
                     double latitude,
                     double longitude,
                     boolean favorite) {
        this.id = id;
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.type = (type == null ? Type.RAMP : type); // Ensure type is never null.
        this.latitude = latitude;
        this.longitude = longitude;
        this.favorite = favorite;
    }

    /**
     * No-argument constructor required for Firestore.
     */
    public SpotsItem() {}

    // --- Getters ---
    public String getId()           { return id; }
    public String getPlaceId()    { return placeId; }
    public String getName()       { return name; }
    public String getAddress()    { return address; }
    public Type getType()         { return type; }
    public double getLatitude()   { return latitude; }
    public double getLongitude()  { return longitude; }
    public boolean isFavorite()   { return favorite; }

    // --- Setters ---
    public void setId(String id)                    { this.id = id; }
    public void setPlaceId(String placeId)        { this.placeId = placeId; }
    public void setName(String name)              { this.name = name; }
    public void setAddress(String address)        { this.address = address; }
    public void setType(Type type)                { this.type = (type == null ? Type.RAMP : type); }
    public void setLatitude(double latitude)      { this.latitude = latitude; }
    public void setLongitude(double longitude)    { this.longitude = longitude; }
    public void setFavorite(boolean favorite)     { this.favorite = favorite; }

    /**
     * Convenience method to flip the favorite status.
     */
    public void toggleFavorite() { this.favorite = !this.favorite; }

    /**
     * Provides a string representation of the object, useful for logging and debugging.
     */
    @Override
    public String toString() {
        return "SpotsItem{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
