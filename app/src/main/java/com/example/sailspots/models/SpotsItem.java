package com.example.sailspots.models;

import java.io.Serializable;
import java.util.Locale;

/**
 * A data model class representing a single boating spot.
 * Implements Serializable to allow instances to be passed between activities via Intents.
 */
public class   SpotsItem implements Serializable {

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
    private String name;
    private String address;
    private String imageUrl;   // URL for a remote image.
    private Type type;         // The type of spot (RAMP, MARINA, or BEACH).
    private double latitude;   // GPS coordinate.
    private double longitude;  // GPS coordinate.
    private boolean favorite;  // User-bookmarked status.

    /**
     * Full constructor to create a new SpotsItem.
     */
    public SpotsItem(String name,
                     String address,
                     String imageUrl,
                     Type type,
                     double latitude,
                     double longitude,
                     boolean favorite) {
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.type = (type == null ? Type.RAMP : type); // Ensure type is never null.
        this.latitude = latitude;
        this.longitude = longitude;
        this.favorite = favorite;
    }

    /**
     * No-argument constructor required for some serialization frameworks like Firebase.
     */
    public SpotsItem() {}

    // --- Getters ---
    public String getName()       { return name; }
    public String getAddress()    { return address; }
    public String getImageUrl()   { return imageUrl; }
    public Type getType()         { return type; }
    public double getLatitude()   { return latitude; }
    public double getLongitude()  { return longitude; }
    public boolean isFavorite()   { return favorite; }

    // --- Setters ---
    public void setName(String name)               { this.name = name; }
    public void setAddress(String address)         { this.address = address; }
    public void setImageUrl(String imageUrl)       { this.imageUrl = imageUrl; }
    public void setType(Type type)                 { this.type = (type == null ? Type.RAMP : type); }
    public void setLatitude(double latitude)       { this.latitude = latitude; }
    public void setLongitude(double longitude)     { this.longitude = longitude; }
    public void setFavorite(boolean favorite)      { this.favorite = favorite; }

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
