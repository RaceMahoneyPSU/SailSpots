package com.example.sailspots.ui.maps;

import com.google.android.gms.maps.model.LatLng;/**
 * A data model class representing a single marina item in the UI.
 */
public class MarinaItem {

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
     * @param placeId       The Google Places API ID for the marina.
     * @param latLng        The geographical coordinates of the marina.
     * @param distanceMiles The distance to the marina in miles.
     */
    public MarinaItem(String name, String address, String placeId, LatLng latLng, double distanceMiles) {
        this.name = name;
        this.address = address;
        this.placeId = placeId;
        this.latLng = latLng;
        this.distanceMiles = distanceMiles;
        this.favorite = false;
    }

    /**
     * Checks if this marina is currently marked as a favorite.
     *
     * @return true if the item is a favorite, false otherwise.
     */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * Sets the favorite status of this marina.
     *
     * @param favorite The new favorite status.
     */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
