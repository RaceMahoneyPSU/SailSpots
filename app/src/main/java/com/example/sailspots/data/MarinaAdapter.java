package com.example.sailspots.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sailspots.R;
import com.google.android.libraries.places.api.model.Place;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * A RecyclerView adapter that displays a list of Place objects.
 *
 * Features:
 * - Uses ListAdapter and DiffUtil for efficient UI updates.
 * - Manages the "Favorite" heart icon state by checking against a set of known IDs.
 */
public class MarinaAdapter extends ListAdapter<Place, MarinaAdapter.VH> {

    // --- Interfaces ---

    /**
     * Interface to handle clicks on the favorite (heart) button.
     */
    public interface OnFavoriteClickListener {
        void onFavoriteClick(@NonNull Place place, int position);
    }

    /**
     * Interface to handle clicks on the row itself (navigation).
     */
    public interface OnMarinaClickListener {
        void onMarinaClick(@NonNull Place place, int position);
    }

    // --- Fields ---

    private final OnFavoriteClickListener favoriteClickListener;
    private OnMarinaClickListener marinaClickListener;

    // Set of favorite Place IDs used to toggle the heart icon state.
    @NonNull
    private Set<String> favoritePlaceIds = Collections.emptySet();

    /**
     * Constructor for the adapter.
     * Requires a listener to handle favorite actions.
     */
    public MarinaAdapter(@NonNull OnFavoriteClickListener favoriteClickListener) {
        super(DIFF_CALLBACK);
        this.favoriteClickListener = favoriteClickListener;
        setHasStableIds(true);
    }

    /**
     * Updates the set of known favorite IDs and refreshes the list.
     */
    public void setFavoritePlaceIds(@NonNull Set<String> ids) {
        this.favoritePlaceIds = ids;
        // Refresh visible items to update heart icons.
        notifyDataSetChanged();
    }

    /**
     * Sets the listener for general item clicks.
     */
    public void setOnMarinaClickListener(@Nullable OnMarinaClickListener listener) {
        this.marinaClickListener = listener;
    }

    // --- DiffUtil Logic ---

    private static final DiffUtil.ItemCallback<Place> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Place>() {
                @Override
                public boolean areItemsTheSame(@NonNull Place oldItem, @NonNull Place newItem) {
                    return oldItem.getId() != null
                            && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Place oldItem, @NonNull Place newItem) {
                    return Objects.equals(oldItem.getName(), newItem.getName())
                            && Objects.equals(oldItem.getAddress(), newItem.getAddress());
                }

                // Note: Favorite state is external to the Place object payload, so we don't check it here.
            };

    @Override
    public long getItemId(int position) {
        Place item = getItem(position);
        return (item.getId() != null) ? item.getId().hashCode() : position;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_marina, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), favoriteClickListener, marinaClickListener, favoritePlaceIds);
    }

    /**
     * ViewHolder class for list items.
     */
    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final ImageButton btnFavorite;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        /**
         * Binds data to views and sets up click listeners.
         */
        void bind(@NonNull Place place,
                  @Nullable OnFavoriteClickListener favListener,
                  @Nullable OnMarinaClickListener marinaClickListener,
                  @NonNull Set<String> favoritePlaceIds) {

            tvTitle.setText(place.getName());

            // Display address as subtitle.
            String address = place.getAddress();
            tvSubtitle.setText(address != null ? address : "");

            // Determine if the heart should be filled.
            boolean isFavorite = place.getId() != null && favoritePlaceIds.contains(place.getId());

            btnFavorite.setImageResource(R.drawable.favorite_heart_selector);
            btnFavorite.setSelected(isFavorite);

            // Heart click action.
            btnFavorite.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION && favListener != null) {
                    favListener.onFavoriteClick(place, p);
                }
            });

            // Row click action.
            itemView.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION && marinaClickListener != null) {
                    marinaClickListener.onMarinaClick(place, p);
                }
            });
        }
    }
}
