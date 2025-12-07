package com.example.sailspots.ui.maps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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
 * Fragment that displays a Google Map and a list of nearby marinas, docks, and beaches.
 * Users can search for locations, view results, and mark them as favorites.
 *
 * Model:
 *  - Place: Raw search result from Google Places API.
 *  - SpotsItem: A Place the user has favorited, saved in Firestore.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG_MAP = "mapFrag";
    private static final String TAG = "MapsFragment";

    // Distance constants.
    private static final double METERS_IN_MILE = 1609.34;
    private static final double METERS_IN_KM = 1000.0;

    // SharedPreferences keys.
    private static final String PREFS_NAME = "SailSpotsPrefs";
    private static final String KEY_USE_KM = "use_km";

    // --- UI and Data Components ---
    private GoogleMap mMap;
    private SearchView searchView;
    private RecyclerView recyclerMarinas;
    private Spinner spinnerType;
    private Spinner spinnerDistance;

    // All results within the max radius (30 mi / 50 km)
    private List<Place> allPlaces = new ArrayList<>();

    // Current state.
    private String currentTypeFilter = "marinas";
    private double currentRadiusMeters;
    private double maxSearchRadiusMeters;
    private boolean useKilometers = false;
    private LatLng currentCenter = null;

    // Mapping from placeId to marker for map synchronization.
    private final Map<String, Marker> markerByPlaceId = new HashMap<>();

    // --- Services and Clients ---
    private PlacesClient placesClient;
    private SpotsRepository spotsRepo;
    private ListenerRegistration favReg;
    private Set<String> favoriteIdsLive = new HashSet<>();

    private MarinaAdapter marinaAdapter;

    private final ActivityResultLauncher<String> requestFineLocation =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Default constructor required for fragment instantiation.
     */
    public MapsFragment() { }

    /**
     * Inflates the fragment's layout containing the map, filters, and results list.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    /**
     * Called after the view hierarchy has been created.
     * Initializes UI elements, shared prefs, adapters, and map/search setup.
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

        // Default radius & max radius based on unit.
        if (useKilometers) {
            // Spinner: 15, 30, 50 km
            currentRadiusMeters = 15 * METERS_IN_KM;
            maxSearchRadiusMeters = 50 * METERS_IN_KM;
        } else {
            // Spinner: 10, 20, 30 mi
            currentRadiusMeters = 10 * METERS_IN_MILE;
            maxSearchRadiusMeters = 30 * METERS_IN_MILE;
        }

        // --- Adapter Setup ---
        marinaAdapter = new MarinaAdapter(this::toggleFavorite);

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

    /**
     * Starts listening for favorite ID changes when the fragment becomes visible.
     */
    @Override
    public void onStart() {
        super.onStart();
        favReg = spotsRepo.listenFavoriteIds(ids -> {
            favoriteIdsLive = (ids != null) ? new HashSet<>(ids) : new HashSet<>();
            if (marinaAdapter != null) {
                marinaAdapter.setFavoritePlaceIds(favoriteIdsLive);
            }
        }, e -> {
            Log.e("Spots", "favorites listen failed", e);
            Toast.makeText(requireContext(), "Failed to listen to favorites", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Stops listening for favorite ID changes when the fragment is no longer visible.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (favReg != null) {
            favReg.remove();
            favReg = null;
        }
    }

    /**
     * Public hook that can be called before sign-out to ensure Firestore listeners are cleaned up.
     */
    public void onSignOut() {
        Log.d(TAG, "onSignOut called, removing listener.");
        if (favReg != null) {
            favReg.remove();
            favReg = null;
        }
    }

    /**
     * Handles toggling a Place as a favorite when the heart icon is clicked.
     * Performs optimistic UI updates and syncs the change with Firestore.
     *
     * @param place    The Place associated with the clicked item.
     * @param position The adapter position in the list.
     */
    private void toggleFavorite(@NonNull Place place, int position) {
        if (place.getId() == null) return;
        String placeId = place.getId();
        String name = place.getName() != null ? place.getName() : "Spot";

        boolean isCurrentlyFavorite = favoriteIdsLive.contains(placeId);
        boolean newFavorite = !isCurrentlyFavorite;

        if (newFavorite) {
            favoriteIdsLive.add(placeId);
            marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));

            SpotsItem spot = toSpot(place);
            spotsRepo.upsertSpotById(placeId, spot,
                    () -> {
                        Toast.makeText(requireContext(), "Added to favorites: " + name, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Added to DB: " + name);
                    },
                    e -> {
                        favoriteIdsLive.remove(placeId);
                        marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));
                        Toast.makeText(requireContext(), "Failed to add favorite", Toast.LENGTH_SHORT).show();
                    });
        } else {
            favoriteIdsLive.remove(placeId);
            marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));

            spotsRepo.deleteSpotById(placeId,
                    () -> {
                        Toast.makeText(requireContext(), "Removed from favorites: " + name, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Removed from DB: " + name);
                    },
                    e -> {
                        favoriteIdsLive.add(placeId);
                        marinaAdapter.setFavoritePlaceIds(new HashSet<>(favoriteIdsLive));
                        Toast.makeText(requireContext(), "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Submit a list of Places to the adapter and refresh markers.
     */
    private void setPlacesAndRefresh(List<Place> displayPlaces) {
        marinaAdapter.submitList(new ArrayList<>(displayPlaces));
        updateMapMarkers(displayPlaces);
    }

    /**
     * Filters allPlaces by the currently selected radius around currentCenter,
     * then updates the list and map markers.
     */
    private void applyRadiusFilterAndRefresh() {
        if (currentCenter == null || allPlaces == null) {
            setPlacesAndRefresh(new ArrayList<>());
            return;
        }

        List<Place> filtered = new ArrayList<>();
        for (Place place : allPlaces) {
            if (place == null || place.getLatLng() == null) continue;

            float[] results = new float[1];
            Location.distanceBetween(
                    currentCenter.latitude, currentCenter.longitude,
                    place.getLatLng().latitude, place.getLatLng().longitude,
                    results
            );
            double distanceMeters = results[0];
            if (distanceMeters <= currentRadiusMeters) {
                filtered.add(place);
            }
        }

        setPlacesAndRefresh(filtered);
    }

    /**
     * Clears existing markers and adds new ones for the supplied Places list.
     * Also configures marker click behavior to scroll the list.
     *
     * @param placesToShow The list of Places to visualize as markers.
     */
    private void updateMapMarkers(List<Place> placesToShow) {
        if (mMap == null) return;

        mMap.clear();
        markerByPlaceId.clear();

        for (int i = 0; i < placesToShow.size(); i++) {
            Place place = placesToShow.get(i);
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
     * Converts a Places API Place into a SpotsItem suitable for Firestore storage.
     *
     * @param place The Place to convert.
     * @return A corresponding SpotsItem with location, type, and favorite flag set.
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
     * Initializes the Google Places API client if needed and creates a PlacesClient instance.
     */
    private void initializePlacesClient() {
        if (!Places.isInitialized()) {
            String apiKey = getString(R.string.google_maps_key);
            Places.initialize(requireContext().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());
    }

    /**
     * Sets up the type and distance spinners, including their adapters and selection listeners.
     * Type changes trigger a new Places fetch; distance changes re-filter the in-memory results.
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
            spinnerType.setSelection(0);

            spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentTypeFilter = (String) parent.getItemAtPosition(position);
                    if (mMap != null && currentCenter != null) {
                        searchForMarinas(currentCenter, maxSearchRadiusMeters, currentTypeFilter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        }

        if (spinnerDistance != null) {
            List<String> distances;
            if (useKilometers) {
                distances = Arrays.asList("15 km", "30 km", "50 km");
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
                        // Fallback to smallest radius if parsing fails
                        valueKmOrMi = useKilometers ? 15 : 10;
                    }

                    if (useKilometers) {
                        currentRadiusMeters = valueKmOrMi * METERS_IN_KM;
                    } else {
                        currentRadiusMeters = valueKmOrMi * METERS_IN_MILE;
                    }

                    // Just re-filter locally; don't hit the API again.
                    applyRadiusFilterAndRefresh();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        }
    }

    /**
     * Creates and attaches the child SupportMapFragment and registers for the map async callback.
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
     * Configures the SearchView so users can search arbitrary locations by name
     * and run a Places search centered on the result.
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
                            currentCenter = latLng;
                            mMap.clear();
                            markerByPlaceId.clear();
                            mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                            animateCamera(latLng, 12f);
                            // Fetch with max radius; UI filters by currentRadiusMeters.
                            searchForMarinas(latLng, maxSearchRadiusMeters, currentTypeFilter);
                        }
                    } else {
                        Toast.makeText(requireContext(), "No results for \"" + locationName + "\"", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(requireContext(), "Geocoder error. Check network.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    /**
     * Called when the GoogleMap instance is ready.
     * Sets an initial camera position and kicks off the first Places search.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Initial camera position (Miami).
        LatLng miami = new LatLng(25.7617, -80.1918);
        currentCenter = miami;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miami, 11f));

        // Initial fetch with max radius; UI filters down to currentRadiusMeters.
        searchForMarinas(miami, maxSearchRadiusMeters, currentTypeFilter);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        enableMyLocation();
    }

    /**
     * Enables the "My Location" blue dot and button if the location permission is granted.
     * Otherwise, it requests the necessary permission.
     */
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

    /**
     * Animates the camera to the specified LatLng with the given zoom level.
     *
     * @param latLng The target location on the map.
     * @param zoom   The desired zoom level.
     */
    private void animateCamera(LatLng latLng, float zoom) {
        if (mMap == null) return;
        CameraPosition pos = new CameraPosition.Builder()
                .target(latLng).zoom(zoom).tilt(0f).bearing(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    /**
     * Returns the primary Place types to use for a given user-facing filter label.
     *
     * @param filterType The selected filter ("Marinas", "Docks", "Beaches").
     * @return A list of Places API primary type strings.
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
     * Applies additional name/address-based filtering for a Place to match the selected type.
     *
     * @param place      The Place to check.
     * @param filterType The user-facing filter string.
     * @return true if the Place matches the filter; false otherwise.
     */
    private boolean matchesTypeFilter(@NonNull Place place, @NonNull String filterType) {
        String lowerFilter = filterType.toLowerCase(Locale.US);
        String name = place.getName() != null ? place.getName().toLowerCase(Locale.US) : "";
        String addr = place.getAddress() != null ? place.getAddress().toLowerCase(Locale.US) : "";

        if ("marinas".equals(lowerFilter)) {
            return name.contains("marina") || name.contains("yacht") || name.contains("harbor")
                    || addr.contains("marina") || addr.contains("yacht");
        } else if ("docks".equals(lowerFilter)) {
            return name.contains("dock") || name.contains("pier") || name.contains("landing")
                    || addr.contains("dock") || addr.contains("pier");
        } else if ("beaches".equals(lowerFilter)) {
            return name.contains("beach") || addr.contains("beach");
        }
        return true;
    }

    /**
     * Fetches places within {@code maxSearchRadiusMeters} from the Places API,
     * stores them in {@code allPlaces}, and then applies the current radius filter for display.
     *
     * @param centerLatLng        The center of the search area.
     * @param radiusInMetersIgnored Ignored; the method always uses maxSearchRadiusMeters.
     * @param filterType          The current type filter ("Marinas", "Docks", "Beaches").
     */
    private void searchForMarinas(@NonNull LatLng centerLatLng,
                                  double radiusInMetersIgnored,
                                  @NonNull String filterType) {

        double searchRadiusMeters = maxSearchRadiusMeters;

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
        );

        CircularBounds locationRestriction = CircularBounds.newInstance(centerLatLng, searchRadiusMeters);
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

                foundPlaces.add(place);
            }

            Log.d(TAG, "Found " + foundPlaces.size() + " places for filter=" + filterType);

            // Store full set (within max radius) and then filter based on currentRadiusMeters.
            allPlaces = new ArrayList<>(foundPlaces);
            applyRadiusFilterAndRefresh();

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error searching for marinas", e);
            Toast.makeText(getContext(), "Failed to find nearby places.", Toast.LENGTH_SHORT).show();
        });
    }
}
