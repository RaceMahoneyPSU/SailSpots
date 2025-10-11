package com.example.sailspots.ui.maps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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
import com.example.sailspots.models.SpotsItem; // <-- your existing model
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG_MAP = "mapFrag";
    private static final String TAG_FAV = "SpotsItem";
    private static final double MAX_MILES = 50.0;

    private GoogleMap mMap;
    private SearchView searchView;
    private ListView listView;

    private final ArrayList<MarinaItem> marinaData = new ArrayList<>();
    private MarinaAdapter marinaAdapter;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private final ActivityResultLauncher<String> requestFineLocation =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) enableMyLocation();
                else Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
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

        searchView = root.findViewById(R.id.idSearchView);
        listView = root.findViewById(R.id.listMarinas);

        marinaAdapter = new MarinaAdapter(requireContext(), marinaData);
        listView.setAdapter(marinaAdapter);

        // Open details on row tap (explicit intent)
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

        // ♥ Favorite click -> convert to SpotsItem (your model) and log it
        marinaAdapter.setOnFavoriteClickListener(item -> {
            SpotsItem spot = new SpotsItem(
                    item.name,                   // name
                    item.address,                // address
                    "",                          // imageUrl (none for now)
                    SpotsItem.Type.MARINA,       // type
                    item.latLng.latitude,        // latitude
                    item.latLng.longitude,       // longitude
                    true                         // favorite
            );
            Log.d(TAG_FAV, spot.toString());
            Toast.makeText(requireContext(), "Favorited (logged)", Toast.LENGTH_SHORT).show();
        });

        // Add & attach the map
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

        createSearchViewListener();
    }

    private void createSearchViewListener() {
        if (searchView == null) return;
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search for a location…");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                final String locationName = searchView.getQuery() != null
                        ? searchView.getQuery().toString().trim() : "";
                if (locationName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a location.", Toast.LENGTH_SHORT).show();
                    return true;
                }

                executor.execute(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                        if (addressList != null && !addressList.isEmpty()) {
                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            runOnUiThreadSafe(() -> {
                                if (mMap == null) return;
                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                                animateCamera(latLng, 12f);
                                fetchMarinasNear(latLng);
                            });
                        } else {
                            runOnUiThreadSafe(() ->
                                    Toast.makeText(requireContext(),
                                            "No results for \"" + locationName + "\"", Toast.LENGTH_SHORT).show());
                        }
                    } catch (IOException e) {
                        runOnUiThreadSafe(() ->
                                Toast.makeText(requireContext(),
                                        "Geocoder error. Check network/GMS.", Toast.LENGTH_SHORT).show());
                    }
                });
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng nyc = new LatLng(40.7128, -74.0060);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nyc, 11f));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        enableMyLocation();

        // Initial load
        fetchMarinasNear(nyc);

        // Refresh when camera stops moving
        mMap.setOnCameraIdleListener(() -> {
            LatLng center = mMap.getCameraPosition().target;
            fetchMarinasNear(center);
        });
    }

    private void enableMyLocation() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try { mMap.setMyLocationEnabled(true); }
            catch (SecurityException ignored) { }
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

    // ----------------- Marina Fetch with strict + fallback -----------------

    private void fetchMarinasNear(@NonNull LatLng center) {
        final String apiKey = getMapsKeyFromManifest();
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(requireContext(), "Missing Maps API key (manifest)", Toast.LENGTH_SHORT).show();
            return;
        }

        // STRICT Nearby Search (type=marina, rankby=distance)
        final String nearbyUrl = String.format(
                Locale.US,
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                        "?location=%f,%f&rankby=distance&type=marina&key=%s",
                center.latitude, center.longitude, apiKey
        );

        executor.execute(() -> {
            List<MarinaItem> strict = callPlacesAndParse(nearbyUrl, center, true);
            if (strict != null && !strict.isEmpty()) {
                runOnUiThreadSafe(() -> updateMarinaListAndMarkers(strict));
                return;
            }

            // Fallback Text Search if Nearby returns none
            final String textUrl = String.format(
                    Locale.US,
                    "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                            "?query=%s&location=%f,%f&radius=%d&key=%s",
                    "boat+marina+yacht+club+harbor", center.latitude, center.longitude,
                    50000, apiKey // 50km (~31mi) max for Text Search
            );

            List<MarinaItem> fallback = callPlacesAndParse(textUrl, center, false);
            runOnUiThreadSafe(() -> {
                if (fallback != null && !fallback.isEmpty()) {
                    updateMarinaListAndMarkers(fallback);
                } else {
                    Toast.makeText(requireContext(), "No marinas found nearby.", Toast.LENGTH_SHORT).show();
                    updateMarinaListAndMarkers(new ArrayList<>());
                }
            });
        });
    }

    @Nullable
    private List<MarinaItem> callPlacesAndParse(String urlStr, LatLng center, boolean strictType) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray results = root.optJSONArray("results");
            ArrayList<MarinaItem> items = new ArrayList<>();
            if (results != null) {
                for (int i = 0; i < results.length(); i++) {
                    JSONObject r = results.getJSONObject(i);

                    // Filter to marinas
                    boolean isMarina = false;
                    JSONArray types = r.optJSONArray("types");
                    if (types != null) {
                        for (int t = 0; t < types.length(); t++) {
                            if ("marina".equals(types.optString(t))) { isMarina = true; break; }
                        }
                    }
                    if (!isMarina && !strictType) {
                        String nm = r.optString("name", "").toLowerCase(Locale.US);
                        if (nm.contains("marina") || nm.contains("yacht") ||
                                nm.contains("harbor") || nm.contains("harbour") ||
                                nm.contains("boat club") || nm.contains("yacht club")) {
                            isMarina = true;
                        }
                    }
                    if (!isMarina) continue;

                    JSONObject loc = r.optJSONObject("geometry").optJSONObject("location");
                    if (loc == null) continue;
                    double lat = loc.optDouble("lat");
                    double lng = loc.optDouble("lng");
                    double miles = distanceMiles(center.latitude, center.longitude, lat, lng);
                    if (miles > MAX_MILES) continue; // keep within 50 miles

                    String name = r.optString("name", "Marina");
                    String vicinity = r.optString("vicinity", r.optString("formatted_address", ""));
                    String placeId = r.optString("place_id", "");

                    items.add(new MarinaItem(name, vicinity, placeId, new LatLng(lat, lng), miles));
                }
            }
            return items;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void updateMarinaListAndMarkers(@NonNull List<MarinaItem> items) {
        if (mMap != null) {
            mMap.clear();
            for (MarinaItem it : items) {
                mMap.addMarker(new MarkerOptions()
                        .position(it.latLng)
                        .title(it.name)
                        .snippet(it.address));
            }
        }
        marinaData.clear();
        marinaData.addAll(items);
        marinaAdapter.notifyDataSetChanged();
    }

    private static double distanceMiles(double lat1, double lon1, double lat2, double lon2) {
        double R = 3958.7613;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    @Nullable
    private String getMapsKeyFromManifest() {
        try {
            var pm = requireContext().getPackageManager();
            var ai = pm.getApplicationInfo(requireContext().getPackageName(), PackageManager.GET_META_DATA);
            Bundle md = ai.metaData;
            return md != null ? md.getString("com.google.android.geo.API_KEY") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void runOnUiThreadSafe(Runnable r) {
        if (!isAdded() || getActivity() == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else requireActivity().runOnUiThread(r);
    }
}
