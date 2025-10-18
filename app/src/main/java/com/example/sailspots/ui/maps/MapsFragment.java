package com.example.sailspots.ui.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.sailspots.data.SpotsDao;
import com.example.sailspots.models.MarinaItem;
import com.example.sailspots.models.SpotsItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A fragment that displays a Google Map and a list of nearby marinas.
 * Users can search for locations, view marinas, and mark them as favorites.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {

    // Tag for identifying the map fragment in the fragment manager.
    private static final String TAG_MAP = "mapFrag";

    // --- UI and Data Components ---
    private GoogleMap mMap;
    private SearchView searchView;
    private RecyclerView recyclerMarinas;
    private List<MarinaItem> allMarinas = new ArrayList<>();
    private SpotsDao spotsDao;
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

        // --- View and DAO Initialization ---
        searchView = root.findViewById(R.id.idSearchView);
        spotsDao = new SpotsDao(requireContext());
        recyclerMarinas = root.findViewById(R.id.recyclerMarinas);
        recyclerMarinas.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMarinas.setHasFixedSize(true);

        // --- Adapter Setup ---
        // Initialize the adapter and define the favorite button click behavior.
        marinaAdapter = new MarinaAdapter((item, position) -> {
            // Create a mutable copy of the current list from the adapter.
            List<MarinaItem> current = new ArrayList<>(marinaAdapter.getCurrentList());
            MarinaItem old = current.get(position);
            boolean newFavorite = !old.isFavorite();

            // Create an updated item with the new favorite state.
            MarinaItem updated = new MarinaItem(
                    old.name, old.address, old.placeId, old.latLng, old.distanceMiles, newFavorite
            );
            // Replace the old item with the updated one and submit the new list.
            current.set(position, updated);
            marinaAdapter.submitList(current);

            // --- Database Sync ---
            if (newFavorite) {
                // If it's a new favorite, convert to a SpotsItem and save to the database.
                spotsDao.upsert(toSpot(updated));
                Log.d("Marinas", "Added to DB: " + updated.name);
            } else {
                // If it's no longer a favorite, remove it from the database using its unique placeId.
                spotsDao.deleteByPlaceId(updated.placeId);
                Log.d("Marinas", "Removed from DB: " + updated.name);
            }
        });
        recyclerMarinas.setAdapter(marinaAdapter);

        // --- Initial Data Load ---
        seedDummyMarinas(); // TODO: Replace with actual location-based map query.
        syncFavoritesFromDatabase();

        // --- Final Setup ---
        setupMapFragment(savedInstanceState);
        setupSearchView();
    }

    /**
     * Populates the marina list with hardcoded data for development and testing.
     */
    private void seedDummyMarinas() {
        List<MarinaItem> dummyMarinas = new ArrayList<>();
        dummyMarinas.add(new MarinaItem("Hudson Marina", "123 River Rd", "id1", new LatLng(40.70, -74.01), 1.2, false));
        dummyMarinas.add(new MarinaItem("East Bay Harbor", "45 Dock St", "id2", new LatLng(40.72, -74.00), 2.5, false));
        dummyMarinas.add(new MarinaItem("Lakeside Yacht Club", "789 Lake Ave", "id3", new LatLng(40.74, -74.02), 4.8, false));
        dummyMarinas.add(new MarinaItem("North Cove Marina", "385 South End Ave", "id4", new LatLng(40.709, -74.016), 0.5, false));
        dummyMarinas.add(new MarinaItem("ONE°15 Brooklyn Marina", "159 Bridge Park Dr", "id5", new LatLng(40.697, -73.999), 1.8, false));
        dummyMarinas.add(new MarinaItem("Newport Yacht Club & Marina", "76 Washington Blvd", "id6", new LatLng(40.726, -74.035), 3.2, false));
        dummyMarinas.add(new MarinaItem("Liberty Landing Marina", "80 Audrey Zapp Dr", "id7", new LatLng(40.71, -74.04), 2.1, false));
        dummyMarinas.add(new MarinaItem("Pier 40", "353 West St", "id8", new LatLng(40.729, -74.011), 1.5, false));
        dummyMarinas.add(new MarinaItem("Hoboken Cove Boathouse", "Frank Sinatra Dr", "id9", new LatLng(40.748, -74.025), 5.5, false));
        dummyMarinas.add(new MarinaItem("Weehawken-Port Imperial", "4800 Ave at Port Imperial", "id10", new LatLng(40.78, -74.01), 7.0, false));
        allMarinas = new ArrayList<>(dummyMarinas);
        marinaAdapter.submitList(allMarinas);
    }

    /**
     * Converts a MarinaItem to a SpotsItem for database storage.
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
     * Fetches favorite items from the database and updates the UI list to reflect their status.
     */
    private void syncFavoritesFromDatabase() {
        // Get all saved spots and store their placeIds in a Set for fast lookup.
        List<SpotsItem> favorites = spotsDao.getAll();
        Set<String> favoriteIds = new HashSet<>();
        for (SpotsItem s : favorites) {
            favoriteIds.add(s.getPlaceId());
        }

        // Create a new list, updating the favorite state of each marina.
        List<MarinaItem> merged = new ArrayList<>(allMarinas.size());
        for (MarinaItem m : allMarinas) {
            boolean isFavorite = favoriteIds.contains(m.placeId);
            merged.add(new MarinaItem(
                    m.name, m.address, m.placeId, m.latLng, m.distanceMiles, isFavorite
            ));
        }
        // Update the adapter with the synchronized list.
        marinaAdapter.submitList(merged);
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
                            mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                            animateCamera(latLng, 12f);
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
        // Set initial camera position (e.g., New York City).
        LatLng nyc = new LatLng(40.7128, -74.0060);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nyc, 11f));

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
     * @param latLng The target coordinates.
     * @param zoom   The target zoom level.
     */
    private void animateCamera(LatLng latLng, float zoom) {
        if (mMap == null) return;
        CameraPosition pos = new CameraPosition.Builder()
                .target(latLng).zoom(zoom).tilt(0f).bearing(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
    }
}
