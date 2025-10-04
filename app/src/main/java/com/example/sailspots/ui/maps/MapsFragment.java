package com.example.sailspots.ui.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sailspots.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG_MAP = "mapFrag";
    private GoogleMap mMap;
    private SearchView searchView;

    // background executor for geocoding (avoid blocking main thread)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> requestFineLocation =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) enableMyLocation();
                else Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            });

    public MapsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // Find SearchView in layout
        searchView = root.findViewById(R.id.idSearchView);
        createSearchViewListener();

        // Add/attach the map fragment
        if (savedInstanceState == null) {
            SupportMapFragment mapFragment = new SupportMapFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment, TAG_MAP)
                    .commit();
        }

        // Get map async
        getChildFragmentManager().executePendingTransactions();
        SupportMapFragment mapFrag =
                (SupportMapFragment) getChildFragmentManager().findFragmentByTag(TAG_MAP);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }
    }
    /**
     * Sets up a listener that captures submitted text from the SearchView.
     * On submit, it reads the query into locationName, geocodes it, and (if found)
     * moves/zooms the map and drops a marker.
     */

    // TODO: Add Polygons, Filters, and Autocomplete
    private void createSearchViewListener() {
        if (searchView == null) return;

        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search for a location:");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String locationName = searchView.getQuery().toString().trim();

                List<Address> addressList = null;
                if (locationName != null || locationName.equals("")) {
                    Geocoder geocoder = new Geocoder(requireContext());
                    try {
                        addressList = geocoder.getFromLocationName(locationName, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Address address = addressList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
                }
                return false;
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

        // Default camera in NYC
        LatLng nyc = new LatLng(40.7128, -74.0060);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nyc, 11f));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        enableMyLocation();
    }

    private void enableMyLocation() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {}
        } else {
            requestFineLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
}