package com.example.sailspots.ui.maps;

import com.google.android.gms.maps.model.LatLng;

public class MarinaItem {
    public final String name;
    public final String address;
    public final String placeId;
    public final LatLng latLng;
    public final double distanceMiles;

    public MarinaItem(String name, String address, String placeId, LatLng latLng, double distanceMiles) {
        this.name = name;
        this.address = address;
        this.placeId = placeId;
        this.latLng = latLng;
        this.distanceMiles = distanceMiles;
    }
}
