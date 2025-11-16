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
import com.example.sailspots.models.MarinaItem;

import java.util.List;
import java.util.Objects;

/**
 * A RecyclerView adapter that efficiently displays a list of MarinaItem objects.
 * It uses ListAdapter and DiffUtil for optimized performance with dynamic data.
 */
public class MarinaAdapter extends ListAdapter<MarinaItem, MarinaAdapter.VH> {

    /**
     * An interface to handle clicks on the favorite button in a list item.
     */
    public interface OnFavoriteClickListener {
        /**
         * Called when the favorite button on an item is clicked.
         * @param item The MarinaItem that was clicked.
         * @param position The adapter position of the clicked item.
         */
        void onFavoriteClick(@NonNull MarinaItem item, int position);
    }

    public interface OnMarinaClickListener {
        void onMarinaClick(@NonNull MarinaItem item, int position);
    }

    private final OnFavoriteClickListener favoriteClickListener;
    private OnMarinaClickListener marinaClickListener;


    /**
     * Constructor for the adapter.
     * @param favoriteClickListener A listener to handle favorite button clicks.
     */
    public MarinaAdapter(@NonNull OnFavoriteClickListener favoriteClickListener) {
        super(DIFF);
        this.favoriteClickListener = favoriteClickListener;
        setHasStableIds(true); // Enable stable IDs for better animations.
    }

    public void setOnMarinaClickListener(@Nullable OnMarinaClickListener listener) {
        this.marinaClickListener = listener;
    }


    /**
     * DiffUtil configuration to calculate list changes efficiently.
     * This helps the RecyclerView perform optimized updates (e.g., animations).
     */
    private static final DiffUtil.ItemCallback<MarinaItem> DIFF =
            new DiffUtil.ItemCallback<MarinaItem>() {
                /**
                 * Checks if two items are the same entity (e.g., they have the same unique ID).
                 */
                @Override
                public boolean areItemsTheSame(@NonNull MarinaItem oldItem, @NonNull MarinaItem newItem) {
                    return oldItem.placeId != null && oldItem.placeId.equals(newItem.placeId);
                }
                /**
                 * Checks if the contents of two items are the same.
                 * Called only if areItemsTheSame() returns true.
                 */
                @Override
                public boolean areContentsTheSame(@NonNull MarinaItem oldItem, @NonNull MarinaItem newItem) {
                    return oldItem.isFavorite() == newItem.isFavorite()
                            && Objects.equals(oldItem.name, newItem.name)
                            && Objects.equals(oldItem.address, newItem.address)
                            && Double.compare(oldItem.distanceMiles, newItem.distanceMiles) == 0;
                }
                /**
                 * Creates a payload for partial updates if only certain fields changed.
                 * This allows for more efficient view updates.
                 */
                @Override
                public Object getChangePayload(@NonNull MarinaItem oldItem, @NonNull MarinaItem newItem) {
                    // If only the favorite state is different, return a "favorite" payload.
                    return (oldItem.isFavorite() != newItem.isFavorite()) ? "favorite" : null;
                }
            };

    /**
     * Returns a unique, stable ID for the item at the given position.
     * This is used by the RecyclerView to optimize animations.
     */
    @Override
    public long getItemId(int position) {
        MarinaItem item = getItem(position);
        return (item.placeId != null) ? item.placeId.hashCode() : position;
    }

    /**
     * Creates new ViewHolder instances by inflating the row layout.
     * Called by the RecyclerView when it needs a new view.
     */
    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_marina, parent, false);
        return new VH(v);
    }

    /**
     * Binds data to a ViewHolder for a full refresh of the item view.
     * Called by the RecyclerView to display the data at a specific position.
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), position, favoriteClickListener, marinaClickListener);
    }

    /**
     * An optimized version of onBindViewHolder that handles partial updates.
     * @param payloads A list of payloads from DiffUtil (e.g., "favorite").
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        // If the payload indicates only the favorite status changed, update only that view.
        if (!payloads.isEmpty() && payloads.contains("favorite")) {
            holder.bindFavoriteOnly(getItem(position));
        } else {
            // Otherwise, perform a full rebind.
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    /**
     * ViewHolder class that holds and manages the views for a single list item.
     * This improves performance by avoiding repeated findViewById() calls.
     */
    static class VH extends RecyclerView.ViewHolder {
        // View references for the list item.
        final TextView tvTitle;
        final TextView tvSubtitle;
        final ImageButton btnFavorite;

        /**
         * ViewHolder constructor.
         * @param itemView The root view of the list item layout.
         */
        VH(@NonNull View itemView) {
            super(itemView);
            // Find views by their ID.
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        void bind(MarinaItem item,
                  int position,
                  @Nullable OnFavoriteClickListener favListener) {
            bind(item, position, favListener, null);
        }

        /**
         * Binds a MarinaItem's data to the views (a "full" bind).
         * @param item The data item to display.
         * @param position The position of the item.
         * @param favListener The listener for favorite button clicks.
         */
        void bind(MarinaItem item,
                  int position,
                  @Nullable OnFavoriteClickListener favListener,
                  @Nullable OnMarinaClickListener marinaClickListener) {

            tvTitle.setText(item.name);
            tvSubtitle.setText(
                    item.address +
                            (item.distanceMiles > 0 ? String.format(" — %.1f mi", item.distanceMiles) : "")
            );

            // Set the visual state of the favorite button.
            btnFavorite.setImageResource(R.drawable.favorite_heart_selector);
            btnFavorite.setSelected(item.isFavorite());

            // Set the click listener for the favorite button.
            btnFavorite.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return; // Ignore clicks during layout changes.
                if (favListener != null) favListener.onFavoriteClick(item, p);
            });

            // Whole-row click -> open detail page
            itemView.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return; // Ignore clicks during layout changes.
                if (marinaClickListener != null) marinaClickListener.onMarinaClick(item, p);
            });

            }
            /**
             * Updates only the favorite button's state (a "partial" bind).
             * This is more efficient than rebinding the entire view.
             * @param item The data item with the updated favorite status.
             */
            void bindFavoriteOnly(MarinaItem item) {
                btnFavorite.setSelected(item.isFavorite());
            }
        }
    }

