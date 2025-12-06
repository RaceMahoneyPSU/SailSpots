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

/**
 * Fragment responsible for displaying the user's list of favorite spots.
 *
 * Responsibilities:
 * - Listens to Firestore for changes in the user's "spots" collection.
 * - Fetches full details for saved spots and adapts them for the RecyclerView.
 * - Handles navigation to spot details and removal of favorites.
 */
public class FavoritesFragment extends Fragment {

    private static final String TAG = "FavoritesFragment";

    // --- UI Components ---
    private RecyclerView recyclerView;
    private TextView textNoFavorites;
    private ProgressBar progressBar;

    // --- Data Components ---
    private MarinaAdapter adapter;
    private SpotsRepository spotsRepo;
    private ListenerRegistration favoritesListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // --- View Initialization ---
        recyclerView = root.findViewById(R.id.recyclerFavorites);
        textNoFavorites = root.findViewById(R.id.textNoFavorites);
        progressBar = root.findViewById(R.id.progressBar);

        // --- Repository Setup ---
        spotsRepo = new SpotsRepository();

        // --- Adapter Setup ---
        setupRecyclerView();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Begin listening for data updates when the view is visible.
        loadFavorites();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detach listeners to prevent memory leaks.
        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
    }

    /**
     * Configures the RecyclerView and defines click behaviors for list items.
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // Initialize Adapter.
        adapter = new MarinaAdapter(this::removeFavorite);

        // Handle item clicks for navigation.
        adapter.setOnMarinaClickListener((place, position) -> {
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

    /**
     * Logic for clicking the heart icon on this screen.
     * Removes the item from the database immediately.
     */
    private void removeFavorite(@NonNull Place place, int position) {
        String id = place.getId();
        if (id == null) return;

        spotsRepo.deleteSpotById(id,
                () -> Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show(),
                e -> {
                    Toast.makeText(requireContext(), "Failed to remove", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting spot", e);
                });
    }

    /**
     * Sets up the Firestore listener to retrieve favorite IDs.
     * Once IDs are loaded, triggers a fetch for their full details.
     */
    private void loadFavorites() {
        showLoading(true);

        favoritesListener = spotsRepo.listenFavoriteIds(ids -> {
            if (ids == null || ids.isEmpty()) {
                showLoading(false);
                // Update adapter with empty set so it clears any old state.
                adapter.setFavoritePlaceIds(ids != null ? ids : Set.of());
                updateUI(new ArrayList<>());
                return;
            }

            // Inform adapter which IDs are favorites (all of them on this screen).
            adapter.setFavoritePlaceIds(ids);
            fetchDetailsForIds(ids);

        }, e -> {
            showLoading(false);
            Log.e(TAG, "Error loading favorites", e);
            Toast.makeText(requireContext(), "Error loading favorites", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Iterates through a set of IDs, fetching the full SpotsItem object for each.
     * Converts results to Place objects and updates the UI once all are loaded.
     */
    private void fetchDetailsForIds(Set<String> ids) {
        List<Place> loadedPlaces = new ArrayList<>();
        final int totalToLoad = ids.size();
        final int[] loadCount = {0};

        for (String id : ids) {
            spotsRepo.getSpot(id, spotItem -> {
                if (spotItem != null) {
                    loadedPlaces.add(toPlace(spotItem));
                }
                loadCount[0]++;
                checkLoadComplete(loadCount[0], totalToLoad, loadedPlaces);
            }, e -> {
                Log.e(TAG, "Failed to load spot details: " + id, e);
                loadCount[0]++;
                checkLoadComplete(loadCount[0], totalToLoad, loadedPlaces);
            });
        }
    }

    /**
     * Checks if the asynchronous loading of all items is complete.
     */
    private void checkLoadComplete(int currentCount, int total, List<Place> places) {
        if (currentCount == total) {
            showLoading(false);
            updateUI(places);
        }
    }

    /**
     * Updates the list visibility based on whether items exist.
     */
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
     * Converts a stored SpotsItem (database model) into a Place object (UI model).
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

    /**
     * Toggles the visibility of the progress bar and main content.
     */
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
