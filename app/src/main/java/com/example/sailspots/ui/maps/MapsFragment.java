package com.example.sailspots.ui.maps;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sailspots.R;
import com.example.sailspots.data.MarinaAdapter;
import com.example.sailspots.data.SpotsRepository;
import com.example.sailspots.models.SpotsItem;
import com.example.sailspots.ui.detail.MarinaDetailActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A fragment that displays a Google Map and a list of nearby marinas / docks / beaches.
 * Users can search for locations, view results, and mark them as favorites.
 *
 * Model:
 *  - Place  = raw search result (marina / dock / beach)
 *  - SpotsItem = a Place the user has favorited (saved in Firestore)
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {

    // Tag for identifying the map fragment in the fragment manager.
    private static final String TAG_MAP = "mapFrag";
    private static final String TAG = "MapsFragment";

    // Distance constants.
    private static final double METERS_IN_MILE = 1609.34;
    private static final double METERS_IN_KM = 1000.0;
    // Default radius (10 miles) for initial search (overridden by unit pref).
    private static final double DEFAULT_RADIUS_METERS = 10 * METERS_IN_MILE;

    // SharedPreferences keys (must match SettingsFragment).
    private static final String PREFS_NAME = "SailSpotsPrefs";
    private static final String KEY_USE_KM = "use_km";

    // --- UI and Data Components ---
    private GoogleMap mMap;
    private SearchView searchView;
    private RecyclerView recyclerMarinas;
    private Spinner spinnerType;
    private Spinner spinnerDistance;

    // All current search results (Places).
    private List<Place> allPlaces = new ArrayList<>();

    // Current filter state.
    private String currentTypeFilter = "marinas";
    private double currentRadiusMeters = DEFAULT_RADIUS_METERS;
    private boolean useKilometers = false;  // read from settings

    // Mapping from placeId -> marker so we can sync map <-> list.
    private final Map<String, Marker> markerByPlaceId = new HashMap<>();

    // --- Services and Clients ---
    private PlacesClient placesClient;

    private SpotsRepository spotsRepo;
    private ListenerRegistration favReg;
    private Set<String> favoriteIdsLive = new HashSet<>();

    // Adapter now expected to work with Place items.
    private MarinaAdapter marinaAdapter;

    /**
     * Handles the result of the location permission request.
     */
    private final ActivityResultLauncher<String> requestFineLocation =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // If permission is granted, enable the 'My Location' layer on the map.
                    enableMyLocation();
                } else {
                    // Inform the user that the feature is unavailable.
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    public MapsFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // --- Client and Service Initialization ---
        initializePlacesClient();

        // --- View Initialization ---
        searchView = root.findViewById(R.id.idSearchView);
        spinnerType = root.findViewById(R.id.spinnerType);
        spinnerDistance = root.findViewById(R.id.spinnerDistance);
        spotsRepo = new SpotsRepository();
        recyclerMarinas = root.findViewById(R.id.recyclerMarinas);
        recyclerMarinas.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMarinas.setHasFixedSize(true);

        // Load unit preference from settings
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        useKilometers = prefs.getBoolean(KEY_USE_KM, false);
        // Set initial radius according to current unit (10 km or 10 mi)
        currentRadiusMeters = useKilometers ? 10 * METERS_IN_KM : 10 * METERS_IN_MILE;

        // --- Adapter Setup ---
        // Initialize the adapter and define the favorite button click behavior.
        marinaAdapter = new MarinaAdapter((place, position) -> {
            if (place.getId() == null) return;
            String placeId = place.getId();
            String name = place.getName() != null ? place.getName() : "Spot";

            boolean isCurrentlyFavorite = favoriteIdsLive.contains(placeId);
            boolean newFavorite = !isCurrentlyFavorite;

            if (newFavorite) {
                // --- Optimistically ADD to favorites ---
                favoriteIdsLive.add(placeId);
                marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));

                SpotsItem spot = toSpot(place);   // updated helper below

                spotsRepo.upsertSpotById(placeId, spot,
                        () -> {
                            Toast.makeText(requireContext(),
                                    "Added to favorites: " + name,
                                    Toast.LENGTH_SHORT).show();
                            Log.d("Marinas", "Added to DB: " + name);
                        },
                        e -> {
                            // Revert on failure
                            favoriteIdsLive.remove(placeId);
                            marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));
                            Toast.makeText(requireContext(),
                                    "Failed to add favorite: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.e("Marinas", "Failed to add to DB: " + e.getMessage());
                        });

            } else {
                // --- Optimistically REMOVE from favorites ---
                favoriteIdsLive.remove(placeId);
                marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));

                spotsRepo.deleteSpotById(placeId,
                        () -> {
                            Toast.makeText(requireContext(),
                                    "Removed from favorites: " + name,
                                    Toast.LENGTH_SHORT).show();
                            Log.d("Marinas", "Removed from DB: " + name);
                        },
                        e -> {
                            // Revert on failure
                            favoriteIdsLive.add(placeId);
                            marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));
                            Toast.makeText(requireContext(),
                                    "Failed to remove favorite: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.e("Marinas", "Error removing from DB: " + name, e);
                        });
            }
        });

        // When a list item is clicked, open the detail screen.
        marinaAdapter.setOnMarinaClickListener((place, position) -> {
            if (place == null) return;

            Intent intent = new Intent(requireContext(), MarinaDetailActivity.class);
            intent.putExtra(MarinaDetailActivity.EXTRA_MARINA_NAME, place.getName());
            intent.putExtra(MarinaDetailActivity.EXTRA_MARINA_ADDRESS, place.getAddress());
            intent.putExtra(MarinaDetailActivity.EXTRA_PLACE_ID, place.getId());
            if (place.getLatLng() != null) {
                intent.putExtra(MarinaDetailActivity.EXTRA_LAT, place.getLatLng().latitude);
                intent.putExtra(MarinaDetailActivity.EXTRA_LNG, place.getLatLng().longitude);
            }
            startActivity(intent);
        });

        recyclerMarinas.setAdapter(marinaAdapter);

        // --- Filter Spinners Setup ---
        setupFilterSpinners();

        // --- Final Setup ---
        setupMapFragment(savedInstanceState);
        setupSearchView();
    }

    @Override
    public void onStart() {
        super.onStart();
        favReg = spotsRepo.listenFavoriteIds(ids -> {
            // Keep a mutable copy locally
            favoriteIdsLive = (ids != null) ? new HashSet<>(ids) : new HashSet<>();

            // Let the adapter know which IDs are currently favorites
            if (marinaAdapter != null) {
                marinaAdapter.setFavoritePlaceIds(favoriteIdsLive);
            }
        }, e -> {
            Log.e("Spots", "favorites listen failed", e);
            Toast.makeText(requireContext(), "Failed to listen to favorites", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (favReg != null) {
            favReg.remove();
            favReg = null;
        }
    }

    /**
     * Public method to be called by MainActivity before signing out.
     * This ensures the listener is detached before auth state changes.
     */
    public void onSignOut() {
        Log.d(TAG, "onSignOut called, removing listener.");
        if (favReg != null) {
            favReg.remove();
            favReg = null;
        }
    }

    /**
     * Re-submit the current Place list to the adapter whenever favorites or data change.
     * The adapter is responsible for checking favoriteIdsLive to show favorite state.
     */
    private void recomputeMergedAndSubmit() {
        if (allPlaces == null) return;
        marinaAdapter.submitList(new ArrayList<>(allPlaces));
    }

    private void setPlacesAndRefresh(List<Place> loadedPlaces) {
        this.allPlaces = new ArrayList<>(loadedPlaces);
        recomputeMergedAndSubmit();
        updateMapMarkers();
    }

    /**
     * Refreshes map markers based on the currently loaded Places.
     */
    private void updateMapMarkers() {
        if (mMap == null) return;

        mMap.clear();
        markerByPlaceId.clear();

        for (int i = 0; i < allPlaces.size(); i++) {
            Place place = allPlaces.get(i);
            if (place == null || place.getLatLng() == null) continue;

            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName())
                            .snippet(place.getAddress())
            );
            if (marker != null) {
                marker.setTag(i);
                markerByPlaceId.put(place.getId(), marker);
            }
        }

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Integer) {
                int position = (Integer) tag;
                if (position >= 0 && position < marinaAdapter.getItemCount()) {
                    recyclerMarinas.smoothScrollToPosition(position);
                }
            }
            return false;
        });
    }

    /**
     * Converts a Place result into a SpotsItem for database storage.
     */
    private SpotsItem toSpot(@NonNull Place place) {
        SpotsItem spot = new SpotsItem();
        spot.setName(place.getName());
        spot.setPlaceId(place.getId());
        spot.setAddress(place.getAddress());
        spot.setType(SpotsItem.Type.MARINA);

        if (place.getLatLng() != null) {
            spot.setLatitude(place.getLatLng().latitude);
            spot.setLongitude(place.getLatLng().longitude);
        }
        spot.setFavorite(true);
        return spot;
    }

    /**
     * Initializes the Google Places API client.
     */
    private void initializePlacesClient() {
        if (!Places.isInitialized()) {
            String apiKey = getString(R.string.google_maps_key);
            Places.initialize(requireContext().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());
    }

    /**
     * Sets up the type and distance filter spinners.
     */
    private void setupFilterSpinners() {
        if (spinnerType != null) {
            List<String> types = Arrays.asList("Marinas", "Docks", "Beaches");
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    types
            );
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
            spinnerType.setSelection(0); // default "Marinas"

            spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentTypeFilter = (String) parent.getItemAtPosition(position);
                    if (mMap != null) {
                        LatLng center = mMap.getCameraPosition().target;
                        searchForMarinas(center, currentRadiusMeters, currentTypeFilter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        }

        if (spinnerDistance != null) {
            List<String> distances;
            if (useKilometers) {
                distances = Arrays.asList("10 km", "20 km", "30 km");
            } else {
                distances = Arrays.asList("10 mi", "20 mi", "30 mi");
            }

            ArrayAdapter<String> distanceAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    distances
            );
            distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDistance.setAdapter(distanceAdapter);
            spinnerDistance.setSelection(0);

            spinnerDistance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    int valueKmOrMi;
                    try {
                        valueKmOrMi = Integer.parseInt(label.split(" ")[0]);
                    } catch (Exception e) {
                        valueKmOrMi = 10;
                    }

                    if (useKilometers) {
                        currentRadiusMeters = valueKmOrMi * METERS_IN_KM;
                    } else {
                        currentRadiusMeters = valueKmOrMi * METERS_IN_MILE;
                    }

                    if (mMap != null) {
                        LatLng center = mMap.getCameraPosition().target;
                        searchForMarinas(center, currentRadiusMeters, currentTypeFilter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        }
    }

    /**
     * Initializes and adds the Google Map fragment to the layout.
     */
    private void setupMapFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            SupportMapFragment mapFragment = new SupportMapFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment, TAG_MAP)
                    .commit();
        }

        getChildFragmentManager().executePendingTransactions();
        SupportMapFragment mapFrag =
                (SupportMapFragment) getChildFragmentManager().findFragmentByTag(TAG_MAP);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }
    }

    /**
     * Sets up the SearchView to handle user search queries.
     */
    private void setupSearchView() {
        if (searchView == null) return;
        searchView.setQueryHint("Search for a location…");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String locationName = query.trim();
                if (locationName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a location.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                try {
                    Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        if (mMap != null) {
                            mMap.clear();
                            markerByPlaceId.clear();
                            mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                            animateCamera(latLng, 12f);
                            searchForMarinas(latLng, currentRadiusMeters, currentTypeFilter);
                        }
                    } else {
                        Toast.makeText(requireContext(),
                                "No results for \"" + locationName + "\"",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(requireContext(),
                            "Geocoder error. Check network.",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Initial camera position (e.g., Miami).
        LatLng miami = new LatLng(25.7617, -80.1918);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miami, 11f));

        // Initial search with current filters.
        searchForMarinas(miami, currentRadiusMeters, currentTypeFilter);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        enableMyLocation();
    }

    private void enableMyLocation() {
        if (mMap == null || getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) { }
        } else {
            requestFineLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void animateCamera(LatLng latLng, float zoom) {
        if (mMap == null) return;
        CameraPosition pos = new CameraPosition.Builder()
                .target(latLng).zoom(zoom).tilt(0f).bearing(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    @NonNull
    private List<String> getPrimaryTypesForFilter(@NonNull String filterType) {
        String lower = filterType.toLowerCase(Locale.US);
        if ("beaches".equals(lower)) {
            return Arrays.asList("beach");
        }
        // "Marinas" and "Docks" both start from primary type "marina".
        return Arrays.asList("marina");
    }

    private boolean matchesTypeFilter(@NonNull Place place, @NonNull String filterType) {
        String lowerFilter = filterType.toLowerCase(Locale.US);
        String name = place.getName() != null ? place.getName().toLowerCase(Locale.US) : "";
        String addr = place.getAddress() != null ? place.getAddress().toLowerCase(Locale.US) : "";

        if ("marinas".equals(lowerFilter)) {
            return name.contains("marina")
                    || name.contains("yacht")
                    || name.contains("harbor")
                    || name.contains("harbour")
                    || addr.contains("marina")
                    || addr.contains("yacht")
                    || addr.contains("harbor")
                    || addr.contains("harbour");
        } else if ("docks".equals(lowerFilter)) {
            return name.contains("dock")
                    || name.contains("pier")
                    || name.contains("landing")
                    || name.contains("boat ramp")
                    || addr.contains("dock")
                    || addr.contains("pier")
                    || addr.contains("landing")
                    || addr.contains("boat ramp");
        } else if ("beaches".equals(lowerFilter)) {
            return name.contains("beach") || addr.contains("beach");
        }
        return true;
    }

    /**
     * Searches for marinas/docks/beaches near a given map location within a specified radius.
     * Returns Places, and favorites are tracked separately via favoriteIdsLive.
     */
    private void searchForMarinas(@NonNull LatLng centerLatLng, double radiusInMeters, @NonNull String filterType) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
        );

        CircularBounds locationRestriction = CircularBounds.newInstance(centerLatLng, radiusInMeters);

        List<String> primaryTypes = getPrimaryTypesForFilter(filterType);

        SearchNearbyRequest request = SearchNearbyRequest.builder(locationRestriction, placeFields)
                .setIncludedPrimaryTypes(primaryTypes)
                .build();

        placesClient.searchNearby(request).addOnSuccessListener(response -> {
            List<Place> foundPlaces = new ArrayList<>();

            for (Place place : response.getPlaces()) {
                if (place.getLatLng() == null || place.getName() == null || place.getId() == null)
                    continue;

                if (!matchesTypeFilter(place, filterType)) continue;

                // Distance is computed if needed by UI; not stored in model here.
                float[] results = new float[1];
                Location.distanceBetween(
                        centerLatLng.latitude, centerLatLng.longitude,
                        place.getLatLng().latitude, place.getLatLng().longitude,
                        results
                );

                foundPlaces.add(place);
            }
            Log.d(TAG, "Found " + foundPlaces.size() + " places for filter=" + filterType);
            setPlacesAndRefresh(foundPlaces);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error searching for marinas", e);
            Toast.makeText(getContext(), "Failed to find nearby places.", Toast.LENGTH_SHORT).show();
        });
    }
}
