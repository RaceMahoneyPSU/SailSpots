package com.example.sailspots.ui.maps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sailspots.R;
import com.example.sailspots.data.SpotsDao;
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
import java.util.List;
import java.util.Locale;

/**
 * A fragment that displays a Google Map and a list of nearby marinas.
 * Users can search for locations, view marinas, and mark them as favorites.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG_MAP = "mapFrag";

    private GoogleMap mMap;
    private SearchView searchView;
    private ListView listView;

    private final ArrayList<MarinaItem> marinaData = new ArrayList<>();
    private SpotsDao spotsDao;
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
     * Default constructor for the fragment.
     */
    public MapsFragment() { }

    /**
     * Inflates the layout for this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    /**
     * Called when the fragment's view has been created.
     */
    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        searchView = root.findViewById(R.id.idSearchView);
        listView = root.findViewById(R.id.listMarinas);

        spotsDao = new SpotsDao(requireContext());

        marinaAdapter = new MarinaAdapter(requireContext(), marinaData);
        listView.setAdapter(marinaAdapter);

        seedDummyMarinas();
        syncFavoritesFromDatabase();
        marinaAdapter.notifyDataSetChanged();

        setupListItemClickListener();
        setupFavoriteToggleListener();
        setupMapFragment(savedInstanceState);
        setupSearchView();
    }

    /**
     * Populates the list with hardcoded marina data for testing.
     */
    private void seedDummyMarinas() {
        marinaData.clear();
        marinaData.add(new MarinaItem("Hudson Marina", "123 River Rd", "id1", new LatLng(40.70, -74.01), 1.2));
        marinaData.add(new MarinaItem("East Bay Harbor", "45 Dock St", "id2", new LatLng(40.72, -74.00), 2.5));
        marinaData.add(new MarinaItem("Lakeside Yacht Club", "789 Lake Ave", "id3", new LatLng(40.74, -74.02), 4.8));
        marinaData.add(new MarinaItem("Sunset Marina", "12 Ocean Blvd", "id4", new LatLng(40.76, -74.05), 6.3));
        marinaData.add(new MarinaItem("Harborview Dock", "555 Pier Ln", "id5", new LatLng(40.68, -74.08), 3.9));
        marinaData.add(new MarinaItem("Bluewater Harbor", "890 Bay St", "id6", new LatLng(40.71, -74.03), 2.1));
        marinaData.add(new MarinaItem("Riverbend Marina", "222 Canal Rd", "id7", new LatLng(40.73, -74.07), 5.7));
        marinaData.add(new MarinaItem("Seaside Boat Club", "78 Shoreline Dr", "id8", new LatLng(40.75, -74.09), 7.4));
        marinaData.add(new MarinaItem("North Cove Marina", "101 North St", "id9", new LatLng(40.77, -74.04), 8.2));
        marinaData.add(new MarinaItem("Harbor Lights Marina", "202 Harbor Way", "id10", new LatLng(40.69, -74.06), 4.5));
    }

    /**
     * Fetches items from the database and updates the favorite state of items in the list.
     */
    private void syncFavoritesFromDatabase() {
        List<SpotsItem> favorites = spotsDao.getAll();
        for (MarinaItem marina : marinaData) {
            marina.setFavorite(false);
            for (SpotsItem fav : favorites) {
                if (fav.getName() != null
                        && fav.getName().equals(marina.name)
                        && fav.getLatitude() == marina.latLng.latitude
                        && fav.getLongitude() == marina.latLng.longitude) {
                    marina.setFavorite(true);
                    break;
                }
            }
        }
    }

    /**
     * Sets up the click listener for list items to open a detail view.
     */
    private void setupListItemClickListener() {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            MarinaItem item = marinaData.get(position);
            Intent intent = new Intent(requireContext(), SpotDetailActivity.class);
            intent.putExtra("name", item.name);
            intent.putExtra("address", item.address);
            intent.putExtra("lat", item.latLng.latitude);
            intent.putExtra("lng", item.latLng.longitude);
            intent.putExtra("miles", item.distanceMiles);
            startActivity(intent);
        });
    }

    /**
     * Sets up the listener for the favorite button to handle database operations.
     */
    private void setupFavoriteToggleListener() {
        marinaAdapter.setOnFavoriteClickListener(item -> {
            SpotsItem spot = new SpotsItem(
                    item.name, item.address, "", SpotsItem.Type.MARINA,
                    item.latLng.latitude, item.latLng.longitude, item.isFavorite()
            );

            if (item.isFavorite()) {
                spotsDao.upsert(spot);
            } else {
                spotsDao.deleteByNameAndLatLng(item.name, item.latLng.latitude, item.latLng.longitude);
            }
        });
    }

    /**
     * Adds the map fragment to the layout and requests the map asynchronously.
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
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search for a location…");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String locationName = searchView.getQuery() != null
                        ? searchView.getQuery().toString().trim() : "";
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
                            mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                            animateCamera(latLng, 12f);
                        }
                    } else {
                        Toast.makeText(requireContext(),
                                "No results for \"" + locationName + "\"", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(requireContext(),
                            "Geocoder error. Check network/GMS.", Toast.LENGTH_SHORT).show();
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
     * Called when the map is ready to be used.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng nyc = new LatLng(40.7128, -74.0060);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nyc, 11f));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        enableMyLocation();
    }

    /**
     * Checks for location permission and enables the 'My Location' layer.
     */
    private void enableMyLocation() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }
        } else {
            requestFineLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Animates the map camera to a new position.
     */
    private void animateCamera(LatLng latLng, float zoom) {
        if (mMap == null) return;
        CameraPosition pos = new CameraPosition.Builder()
                .target(latLng).zoom(zoom).tilt(0f).bearing(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
    }
}
