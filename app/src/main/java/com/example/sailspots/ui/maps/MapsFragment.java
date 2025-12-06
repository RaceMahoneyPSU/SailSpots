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
import com.example.sailspots.models.MarinaItem;
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
 * A fragment that displays a Google Map and a list of nearby marinas.
 * Users can search for locations, view marinas, and mark them as favorites.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {

    // Tag for identifying the map fragment in the fragment manager.
    private static final String TAG_MAP = "mapFrag";
    private static final String TAG = "MapsFragment";

    // Distance constants.
    private static final double METERS_IN_MILE = 1609.34;
    private static final double METERS_IN_KM = 1000.0;
    // Default radius (10 miles) for initial
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
    private List<MarinaItem> allMarinas = new ArrayList<>();

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

    /**
     * Default constructor for the fragment. Required for fragment instantiation.
     */
    public MapsFragment() { }

    /**
     * Inflates the fragment's layout.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    /**
     * Called after the view has been created. Used to initialize UI components and listeners.
     */
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

        // Adapter Setup
        // Initialize the adapter and define the favorite button click behavior
        marinaAdapter = new MarinaAdapter((item, position) -> {
            // Create a mutable copy of the current list from the adapter.
            List<MarinaItem> current = new ArrayList<>(marinaAdapter.getCurrentList());
            MarinaItem old = current.get(position);
            boolean newFavorite = !old.isFavorite();

            // Create an updated item with the new favorite state.
            MarinaItem updated = new MarinaItem(
                    old.name, old.address, old.placeId, old.latLng, old.distanceMeters, newFavorite
            );
            // Replace the old item with the updated one and submit the new list.
            current.set(position, updated);
            marinaAdapter.submitList(current);

            // --- Database Sync ---
            if (newFavorite) {
                // If it's a new favorite, convert to a SpotsItem and save to the database.
                SpotsItem spot = toSpot(updated);
                String docId = spot.getPlaceId();
                spotsRepo.upsertSpotById(docId, spot,
                        () -> {
                            Toast.makeText(requireContext(),
                                    "Added to favorites: " + updated.name,
                                    Toast.LENGTH_SHORT).show();
                            Log.d("Marinas", "Added to DB: " + updated.name);
                        },
                        e -> {
                            Toast.makeText(requireContext(),
                                    "Failed to add favorite: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.e("Marinas", "Failed to add to DB: " + e.getMessage());
                            updated.setFavorite(false);
                            marinaAdapter.notifyDataSetChanged();
                        });

            } else {
                // If it's no longer a favorite, remove it from the database using its unique placeId.
                spotsRepo.deleteSpotById(updated.placeId,
                        () -> {
                            Toast.makeText(requireContext(),
                                    "Removed from favorites: " + updated.name,
                                    Toast.LENGTH_SHORT).show();
                            Log.d("Marinas", "Removed from DB: " + updated.name);
                        },
                        e -> {
                            Toast.makeText(requireContext(),
                                    "Failed to remove favorite: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.e("Marinas", "Error removing from DB: " + updated.name, e);
                            updated.setFavorite(true);
                            marinaAdapter.notifyDataSetChanged();
                        });
            }
        });

        // When a list item is clicked, open the detail screen.
        marinaAdapter.setOnMarinaClickListener((item, position) -> {
            Intent intent = new Intent(requireContext(), MarinaDetailActivity.class);
            intent.putExtra(MarinaDetailActivity.EXTRA_MARINA_NAME, item.name);
            intent.putExtra(MarinaDetailActivity.EXTRA_MARINA_ADDRESS, item.address);
            intent.putExtra(MarinaDetailActivity.EXTRA_PLACE_ID, item.placeId);
            if (item.latLng != null) {
                intent.putExtra(MarinaDetailActivity.EXTRA_LAT, item.latLng.latitude);
                intent.putExtra(MarinaDetailActivity.EXTRA_LNG, item.latLng.longitude);
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
            favoriteIdsLive = ids;
            recomputeMergedAndSubmit();  // recompute using latest allMarinas + live IDs
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

    private void recomputeMergedAndSubmit() {
        if (allMarinas == null) return;

        List<MarinaItem> merged = new ArrayList<>(allMarinas.size());
        for (MarinaItem m : allMarinas) {
            boolean isFavorite = favoriteIdsLive.contains(m.placeId);
            merged.add(new MarinaItem(
                    m.name, m.address, m.placeId, m.latLng, m.distanceMeters, isFavorite
            ));
        }
        marinaAdapter.submitList(merged);
    }

    private void setMarinasAndRefresh(List<MarinaItem> loadedMarinas) {
        // Keep an immutable copy for ListAdapter/DiffUtil correctness
        this.allMarinas = new ArrayList<>(loadedMarinas);
        recomputeMergedAndSubmit();  // merges with favoriteIdsLive and updates the adapter
        updateMapMarkers();          // refresh markers for the new list
    }

    /**
     * Refreshes map markers based on the currently loaded marinas.
     */
    private void updateMapMarkers() {
        if (mMap == null) return;

        // Clear old markers and mapping.
        mMap.clear();
        markerByPlaceId.clear();

        for (int i = 0; i < allMarinas.size(); i++) {
            MarinaItem m = allMarinas.get(i);
            if (m.latLng == null) continue;

            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(m.latLng)
                            .title(m.name)
                            .snippet(m.address)
            );
            if (marker != null) {
                // Tag the marker with the list position so we can sync on click.
                marker.setTag(i);
                markerByPlaceId.put(m.placeId, marker);
            }
        }

        // When a marker is clicked, scroll/select corresponding item in the list.
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Integer) {
                int position = (Integer) tag;
                if (position >= 0 && position < marinaAdapter.getItemCount()) {
                    recyclerMarinas.smoothScrollToPosition(position);
                }
            }
            // Return false to also show the default info window.
            return false;
        });
    }

    /**
     * Converts a MarinaItem to a SpotsItem for database storage.
     *
     * @param m The MarinaItem to convert.
     * @return A corresponding SpotsItem.
     */
    private SpotsItem toSpot(@NonNull MarinaItem m) {
        SpotsItem spot = new SpotsItem();
        spot.setName(m.name);
        spot.setPlaceId(m.placeId);
        spot.setAddress(m.address);
        spot.setType(SpotsItem.Type.MARINA);
        if (m.latLng != null) {
            spot.setLatitude(m.latLng.latitude);
            spot.setLongitude(m.latLng.longitude);
        }
        spot.setFavorite(true); // Assumes this is called when an item becomes a favorite.
        return spot;
    }

    /**
     * Initializes the Google Places API client.
     */
    private void initializePlacesClient() {
        if (!Places.isInitialized()) {
            // Use API key from resources.
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
            // Simple inline list for types
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
                    // Re-run the search at the current map center.
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
            // Distance options depending on unit preference.
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
            spinnerDistance.setSelection(0); // default "10"

            spinnerDistance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    // Extract numeric part (e.g., "10" from "10 km" / "10 mi").
                    int valueKmOrMi;
                    try {
                        valueKmOrMi = Integer.parseInt(label.split(" ")[0]);
                    } catch (Exception e) {
                        valueKmOrMi = 10;
                    }

                    // Update radius in meters based on unit.
                    if (useKilometers) {
                        currentRadiusMeters = valueKmOrMi * METERS_IN_KM;
                    } else {
                        currentRadiusMeters = valueKmOrMi * METERS_IN_MILE;
                    }

                    // Re-run the search at the current map center.
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
        // Only add the fragment if the activity is not being recreated.
        if (savedInstanceState == null) {
            SupportMapFragment mapFragment = new SupportMapFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment, TAG_MAP)
                    .commit();
        }

        // Wait for transactions to complete and then get the map asynchronously.
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
            /**
             * Called when the user submits a search query.
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String locationName = query.trim();
                if (locationName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a location.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                // Use Geocoder to find coordinates for the entered location name.
                try {
                    Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    // Get the first result from the geocoder.
                    List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        // Move the map camera to the found location.
                        if (mMap != null) {
                            mMap.clear();
                            markerByPlaceId.clear();
                            mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                            animateCamera(latLng, 12f);
                            // Also search for places around the new location using current filters.
                            searchForMarinas(latLng, currentRadiusMeters, currentTypeFilter);
                        }
                    } else {
                        Toast.makeText(requireContext(), "No results for \"" + locationName + "\"", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(requireContext(), "Geocoder error. Check network.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            /**
             * Called when the text in the search view changes. Not used here.
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    /**
     * Callback for when the Google Map is ready to be used.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Set initial camera position (e.g., Miami, Florida).
        LatLng miami = new LatLng(25.7617, -80.1918);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miami, 11f));

        // Perform initial search with current filters.
        searchForMarinas(miami, currentRadiusMeters, currentTypeFilter);

        // Configure map UI settings.
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Attempt to enable the 'My Location' blue dot and button.
        enableMyLocation();
    }

    /**
     * Checks for location permission and enables the 'My Location' layer if granted.
     * If not granted, it launches the permission request.
     */
    private void enableMyLocation() {
        if (mMap == null || getContext() == null) return;
        // Check if the app has the required location permission.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                // Permission is granted, so enable the location layer.
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
                // This should not happen if the permission check passes.
            }
        } else {
            // Permission is not granted, so request it.
            requestFineLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Animates the map camera to a new position with a specified zoom level.
     *
     * @param latLng The target coordinates.
     * @param zoom   The target zoom level.
     */
    private void animateCamera(LatLng latLng, float zoom) {
        if (mMap == null) return;
        CameraPosition pos = new CameraPosition.Builder()
                .target(latLng).zoom(zoom).tilt(0f).bearing(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    /**
     * Returns the list of primary place types for the current filter.
     * Uses Places primary types, then we further refine by name/address.
     *
     * @param filterType The user-friendly filter string (e.g., "Marinas").
     */
    @NonNull
    private List<String> getPrimaryTypesForFilter(@NonNull String filterType) {
        String lower = filterType.toLowerCase(Locale.US);
        if ("beaches".equals(lower)) {
            return Arrays.asList("beach");
        }
        // "Marinas" and "Docks" both start from primary type "marina".
        return Arrays.asList("marina");
    }

    /**
     * Applies simple name/address heuristics to ensure the place matches the
     * selected filter type (marinas, docks, beaches).
     */
    private boolean matchesTypeFilter(@NonNull Place place, @NonNull String filterType) {
        String lowerFilter = filterType.toLowerCase(Locale.US);
        String name = place.getName() != null ? place.getName().toLowerCase(Locale.US) : "";
        String addr = place.getAddress() != null ? place.getAddress().toLowerCase(Locale.US) : "";

        if ("marinas".equals(lowerFilter)) {
            // Marinas / yacht clubs.
            return name.contains("marina")
                    || name.contains("yacht")
                    || name.contains("harbor")
                    || name.contains("harbour")
                    || addr.contains("marina")
                    || addr.contains("yacht")
                    || addr.contains("harbor")
                    || addr.contains("harbour");
        } else if ("docks".equals(lowerFilter)) {
            // Boating docks / landings
            return name.contains("dock")
                    || name.contains("pier")
                    || name.contains("landing")
                    || name.contains("boat ramp")
                    || addr.contains("dock")
                    || addr.contains("pier")
                    || addr.contains("landing")
                    || addr.contains("boat ramp");
        } else if ("beaches".equals(lowerFilter)) {
            // Beaches
            return name.contains("beach") || addr.contains("beach");
        }
        return true;
    }

    /**
     * Searches for marinas/docks/beaches near a given map location within a specified radius
     * using the Places SDK Search Nearby API.
     *
     * @param centerLatLng   The latitude and longitude of the search center.
     * @param radiusInMeters The search radius in meters.
     * @param filterType     The current type filter (e.g., "Marinas", "Docks", "Beaches").
     */
    private void searchForMarinas(@NonNull LatLng centerLatLng, double radiusInMeters, @NonNull String filterType) {
        // Define the fields you want the API to return for each place.
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
        );

        // Build a circular location restriction with the given radius.
        CircularBounds locationRestriction = CircularBounds.newInstance(centerLatLng, radiusInMeters);

        // Resolve which primary types to include based on the filter.
        List<String> primaryTypes = getPrimaryTypesForFilter(filterType);

        // Build the SearchNearbyRequest to include the selected primary types.
        SearchNearbyRequest request = SearchNearbyRequest.builder(locationRestriction, placeFields)
                .setIncludedPrimaryTypes(primaryTypes)
                .build();

        // Execute the request with the Places client.
        placesClient.searchNearby(request).addOnSuccessListener(response -> {
            List<MarinaItem> foundMarinas = new ArrayList<>();

            for (Place place : response.getPlaces()) {
                if (place.getLatLng() == null || place.getName() == null || place.getId() == null)
                    continue;

                // Extra filtering by type based on name/address.
                if (!matchesTypeFilter(place, filterType)) continue;

                // Compute distance from the search center in either miles or kilometers.
                float[] results = new float[1];
                Location.distanceBetween(centerLatLng.latitude, centerLatLng.longitude,
                        place.getLatLng().latitude, place.getLatLng().longitude, results);

                double distance;
                if (useKilometers) {
                    distance = results[0] / METERS_IN_KM;
                } else {
                    distance = results[0] / METERS_IN_MILE;
                }

                boolean isFavorite = favoriteIdsLive.contains(place.getId());

                // NOTE: distanceMiles now stores distance in *current units* (mi or km).
                foundMarinas.add(new MarinaItem(
                        place.getName(),
                        place.getAddress(),
                        place.getId(),
                        place.getLatLng(),
                        distance,
                        isFavorite
                ));
            }
            Log.d(TAG, "Found " + foundMarinas.size() + " places for filter=" + filterType);
            setMarinasAndRefresh(foundMarinas);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error searching for marinas", e);
            Toast.makeText(getContext(), "Failed to find nearby places.", Toast.LENGTH_SHORT).show();
        });
    }
}
