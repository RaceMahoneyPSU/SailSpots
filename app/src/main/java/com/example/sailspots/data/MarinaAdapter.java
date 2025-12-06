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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * A RecyclerView adapter that efficiently displays a list of Place objects.
 * It uses ListAdapter and DiffUtil for optimized performance with dynamic data.
 */
public class MarinaAdapter extends ListAdapter<Place, MarinaAdapter.VH> {

    /**
     * An interface to handle clicks on the favorite button in a list item.
     */
    public interface OnFavoriteClickListener {
        void onFavoriteClick(@NonNull Place place, int position);
    }

    /**
     * An interface to handle clicks on the row itself.
     */
    public interface OnMarinaClickListener {
        void onMarinaClick(@NonNull Place place, int position);
    }

    private final OnFavoriteClickListener favoriteClickListener;
    private OnMarinaClickListener marinaClickListener;

    // Backing set of favorite Place IDs (kept in sync by the fragment).
    @NonNull
    private Set<String> favoritePlaceIds = Collections.emptySet();

    /**
     * Constructor for the adapter.
     * @param favoriteClickListener A listener to handle favorite button clicks.
     */
    public MarinaAdapter(@NonNull OnFavoriteClickListener favoriteClickListener) {
        super(DIFF);
        this.favoriteClickListener = favoriteClickListener;
        setHasStableIds(true);
    }

    /**
     * Allows the fragment to provide/update the current set of favorite IDs.
     */
    public void setFavoritePlaceIds(@NonNull Set<String> ids) {
        this.favoritePlaceIds = ids;
        // Favorite state is derived from this set, so rebind all items.
        notifyDataSetChanged();
    }

    public void setOnMarinaClickListener(@Nullable OnMarinaClickListener listener) {
        this.marinaClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<Place> DIFF =
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

                @Override
                public Object getChangePayload(@NonNull Place oldItem, @NonNull Place newItem) {
                    // Favorite state is external to Place, so we don't detect it here.
                    return null;
                }
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
        holder.bind(getItem(position), position, favoriteClickListener, marinaClickListener, favoritePlaceIds);
    }

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

        void bind(@NonNull Place place,
                  int position,
                  @Nullable OnFavoriteClickListener favListener,
                  @Nullable OnMarinaClickListener marinaClickListener,
                  @NonNull Set<String> favoritePlaceIds) {

            tvTitle.setText(place.getName());

            // For now we only show the address as subtitle.
            String address = place.getAddress();
            if (address == null) address = "";
            tvSubtitle.setText(address);

            // Determine favorite state from the provided set of IDs.
            boolean isFavorite = place.getId() != null && favoritePlaceIds.contains(place.getId());

            btnFavorite.setImageResource(R.drawable.favorite_heart_selector);
            btnFavorite.setSelected(isFavorite);

            btnFavorite.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                if (favListener != null) favListener.onFavoriteClick(place, p);
            });

            itemView.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                if (marinaClickListener != null) marinaClickListener.onMarinaClick(place, p);
            });
        }
    }
}
