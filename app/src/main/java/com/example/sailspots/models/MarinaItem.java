package com.example.sailspots.models;

import com.google.android.gms.maps.model.LatLng;

/**
 * A data model class representing a single marina item in the UI.
 * This is a lightweight object used for display purposes in the RecyclerView and map.
 */
public class MarinaItem {

    // --- Member Variables ---
    public final String name;
    public final String address;
    public final String placeId;
    public final LatLng latLng;

    // CHANGED: We now store raw meters so we can convert to MI or KM dynamically in the adapter
    public final double distanceMeters;

    private boolean favorite;

    /**
     * Constructs a new MarinaItem.
     *
     * @param name           The name of the marina.
     * @param address        The address of the marina.
     * @param placeId        The Google Places API ID for the marina.
     * @param latLng         The geographical coordinates.
     * @param distanceMeters The distance from user's location in METERS.
     * @param favorite       The initial favorite status.
     */
    public MarinaItem(String name, String address, String placeId, LatLng latLng, double distanceMeters, boolean favorite) {
        this.name = name;
        this.address = address;
        this.placeId = placeId;
        this.latLng = latLng;
        this.distanceMeters = distanceMeters;
        this.favorite = favorite;
    }

    // --- Getters and Setters ---

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getPlaceId() { return placeId; }

    public String getName() { return name; }

    public String getAddress() { return address; }

    public double getDistanceMeters() { return distanceMeters; }

    public boolean isFavorite() {
        return favorite;
    }
}
