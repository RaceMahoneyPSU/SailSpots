package com.example.sailspots.data;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.sailspots.ui.settings.SettingsFragment;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A RecyclerView adapter that efficiently displays a list of MarinaItem objects.
 * It uses ListAdapter and DiffUtil for optimized performance with dynamic data.
 */
public class MarinaAdapter extends ListAdapter<MarinaItem, MarinaAdapter.VH> {

    // Conversion constants
    private static final double MILES_TO_KM = 1.60934;

    /**
     * An interface to handle clicks on the favorite button in a list item.
     */
    public interface OnFavoriteClickListener {
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
        setHasStableIds(true);
    }

    public void setOnMarinaClickListener(@Nullable OnMarinaClickListener listener) {
        this.marinaClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<MarinaItem> DIFF =
            new DiffUtil.ItemCallback<MarinaItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MarinaItem oldItem, @NonNull MarinaItem newItem) {
                    return oldItem.placeId != null && oldItem.placeId.equals(newItem.placeId);
                }
                @Override
                public boolean areContentsTheSame(@NonNull MarinaItem oldItem, @NonNull MarinaItem newItem) {
                    return oldItem.isFavorite() == newItem.isFavorite()
                            && Objects.equals(oldItem.name, newItem.name)
                            && Objects.equals(oldItem.address, newItem.address)
                            && Double.compare(oldItem.distanceMeters, newItem.distanceMeters) == 0;
                }
                @Override
                public Object getChangePayload(@NonNull MarinaItem oldItem, @NonNull MarinaItem newItem) {
                    return (oldItem.isFavorite() != newItem.isFavorite()) ? "favorite" : null;
                }
            };

    @Override
    public long getItemId(int position) {
        MarinaItem item = getItem(position);
        return (item.placeId != null) ? item.placeId.hashCode() : position;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_marina, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), position, favoriteClickListener, marinaClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("favorite")) {
            holder.bindFavoriteOnly(getItem(position));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
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

        void bind(MarinaItem item,
                  int position,
                  @Nullable OnFavoriteClickListener favListener,
                  @Nullable OnMarinaClickListener marinaClickListener) {

            tvTitle.setText(item.name);

            // --- UPDATED LOGIC: Handle Unit Conversion ---
            Context context = itemView.getContext();
            SharedPreferences prefs = context.getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
            boolean useKm = prefs.getBoolean(SettingsFragment.KEY_USE_KM, false);

            String distanceString;
            if (item.distanceMeters > 0) {
                if (useKm) {
                    double km = item.distanceMeters * MILES_TO_KM;
                    distanceString = String.format(Locale.US, " — %.1f km", km);
                } else {
                    distanceString = String.format(Locale.US, " — %.1f mi", item.distanceMeters);
                }
            } else {
                distanceString = "";
            }

            tvSubtitle.setText(item.address + distanceString);

            btnFavorite.setImageResource(R.drawable.favorite_heart_selector);
            btnFavorite.setSelected(item.isFavorite());

            btnFavorite.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                if (favListener != null) favListener.onFavoriteClick(item, p);
            });

            itemView.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                if (marinaClickListener != null) marinaClickListener.onMarinaClick(item, p);
            });
        }

        void bindFavoriteOnly(MarinaItem item) {
            btnFavorite.setSelected(item.isFavorite());
        }
    }
}
