package com.example.sailspots.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sailspots.R;
import com.example.sailspots.data.MarinaAdapter;
import com.example.sailspots.data.SpotsRepository;
import com.example.sailspots.models.SpotsItem;
import com.example.sailspots.ui.detail.MarinaDetailActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritesFragment extends Fragment {

    private static final String TAG = "FavoritesFragment";

    private RecyclerView recyclerView;
    private TextView textNoFavorites;
    private ProgressBar progressBar;
    private MarinaAdapter adapter;
    private SpotsRepository spotsRepo;
    private ListenerRegistration favoritesListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // Initialize Views
        recyclerView = root.findViewById(R.id.recyclerFavorites);
        textNoFavorites = root.findViewById(R.id.textNoFavorites);
        progressBar = root.findViewById(R.id.progressBar);

        // Initialize Repository
        spotsRepo = new SpotsRepository();

        // Setup RecyclerView and Adapter
        setupRecyclerView();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Load Data when the fragment becomes visible
        loadFavorites();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up the listener to prevent memory leaks
        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // Initialize Adapter with click handlers
        adapter = new MarinaAdapter((place, position) -> {
            // "Favorite Heart" click logic: In this screen, clicking the heart means REMOVING from favorites.
            String id = place.getId();
            if (id == null) return;

            spotsRepo.deleteSpotById(id,
                    () -> Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show(),
                    e -> {
                        Toast.makeText(requireContext(), "Failed to remove", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error deleting spot", e);
                    });
        });

        adapter.setOnMarinaClickListener((place, position) -> {
            // "List Item" click logic: Open the detail view
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

        recyclerView.setAdapter(adapter);
    }

    private void loadFavorites() {
        showLoading(true);

        favoritesListener = spotsRepo.listenFavoriteIds(ids -> {
            if (ids == null || ids.isEmpty()) {
                showLoading(false);
                adapter.setFavoritePlaceIds(ids != null ? ids : Set.of());
                updateUI(new ArrayList<>());
                return;
            }

            // Let adapter know which IDs are favorites (all of them here).
            adapter.setFavoritePlaceIds(ids);
            fetchDetailsForIds(ids);

        }, e -> {
            showLoading(false);
            Log.e(TAG, "Error loading favorites", e);
            Toast.makeText(requireContext(), "Error loading favorites", Toast.LENGTH_SHORT).show();
        });
    }

    // Helper to fetch full object details for a set of IDs one by one
    private void fetchDetailsForIds(Set<String> ids) {
        List<Place> loadedPlaces = new ArrayList<>();
        // We need to track when all async calls are done
        final int totalToLoad = ids.size();
        final int[] loadCount = {0};

        for (String id : ids) {
            spotsRepo.getSpot(id, spotItem -> {
                if (spotItem != null) {
                    loadedPlaces.add(toPlace(spotItem));
                }
                loadCount[0]++;

                // If this was the last item, update UI
                if (loadCount[0] == totalToLoad) {
                    showLoading(false);
                    updateUI(loadedPlaces);
                }
            }, e -> {
                loadCount[0]++;
                if (loadCount[0] == totalToLoad) {
                    showLoading(false);
                    updateUI(loadedPlaces);
                }
            });
        }
    }

    private void updateUI(List<Place> places) {
        if (places.isEmpty()) {
            textNoFavorites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            adapter.submitList(new ArrayList<>());
        } else {
            textNoFavorites.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(places);
        }
    }

    /**
     * Converts a stored SpotsItem (favorite) into a lightweight Place object
     * so it can be rendered by MarinaAdapter.
     */
    private Place toPlace(SpotsItem spot) {
        LatLng latLng = (spot.getLatitude() != 0 && spot.getLongitude() != 0)
                ? new LatLng(spot.getLatitude(), spot.getLongitude())
                : null;

        Place.Builder builder = Place.builder()
                .setId(spot.getPlaceId())
                .setName(spot.getName())
                .setAddress(spot.getAddress());

        if (latLng != null) {
            builder.setLatLng(latLng);
        }

        return builder.build();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            textNoFavorites.setVisibility(View.GONE);
        }
    }
}
