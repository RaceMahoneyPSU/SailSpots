package com.example.sailspots.models;

import com.google.android.gms.maps.model.LatLng;

/**
 * A data model class representing a single marina item in the UI. * This is a lightweight object used for display purposes in the RecyclerView and map.
 */
public class MarinaItem {

    // --- Member Variables ---
    public final String name;
    public final String address;
    public final String placeId;
    public final LatLng latLng;
    public final double distanceMiles;
    private boolean favorite;

    /**
     * Constructs a new MarinaItem.
     *
     * @param name          The name of the marina.
     * @param address       The address of the marina.
     * @param placeId       The Google Places API ID for the marina, used as a unique identifier.
     * @param latLng        The geographical coordinates (latitude and longitude) of the marina.
     * @param distanceMiles The distance from the user's current location to the marina in miles.
     * @param favorite      The initial favorite status of the marina.
     */
    public MarinaItem(String name, String address, String placeId, LatLng latLng, double distanceMiles, boolean favorite) {
        this.name = name;
        this.address = address;
        this.placeId = placeId;
        this.latLng = latLng;
        this.distanceMiles = distanceMiles;
        this.favorite = favorite;
    }

    // --- Getters and Setters ---

    /**
     * Sets the favorite status of this marina.
     * @param favorite The new favorite status (true or false).
     */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /**
     * Gets the unique Google Places API ID for this marina.
     * @return The place ID string.
     */
    public String getPlaceId() { return placeId; }

    /**
     * Gets the name of the marina.
     * @return The marina's name.
     */
    public String getName() { return name; }

    /**
     * Gets the address of the marina.
     * @return The marina's address.
     */
    public String getAddress() { return address; }

    /**
     * Gets the distance to the marina in miles.
     * @return The distance in miles.
     */
    public double getDistanceMiles() { return distanceMiles; }

    /**
     * Checks if this marina is currently marked as a favorite.
     * @return true if the item is a favorite, false otherwise.
     */
    public boolean isFavorite() {
        return favorite;
    }
}
