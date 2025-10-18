package com.example.sailspots.ui.maps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.sailspots.R;

import java.util.List;
import java.util.Locale;

/**
 * Custom ArrayAdapter for displaying MarinaItem objects in a ListView.
 */
public class MarinaAdapter extends ArrayAdapter<MarinaItem> {

    /**
     * A listener interface to communicate favorite button clicks back to MapFragment.
     */
    public interface OnFavoriteClickListener {
        /**
         * Called when the favorite button for an item is clicked.
         * @param item The MarinaItem associated with the clicked row.
         */
        void onFavoriteClick(MarinaItem item);
    }

    private OnFavoriteClickListener favoriteClickListener;

    /**
     * Registers a callback to be invoked when a favorite button is clicked.
     * @param listener The listener to attach.
     */
    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteClickListener = listener;
    }

    /**
     * Constructor for the MarinaAdapter.
     * @param context The current context.
     * @param data The list of MarinaItem objects to display.
     */
    public MarinaAdapter(Context context, List<MarinaItem> data) {
        super(context, 0, data);
    }

    /**
     * Creates and returns the view for a single row in the list.
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_marina, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.tvSubtitle = convertView.findViewById(R.id.tvSubtitle);
            holder.btnFavorite = convertView.findViewById(R.id.btnFavorite);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MarinaItem item = getItem(position);

        if (item != null) {
            holder.tvTitle.setText(item.name);
            holder.tvSubtitle.setText(String.format(Locale.US, "%.1f mi • %s",
                    item.distanceMiles,
                    item.address == null ? "" : item.address));

            holder.btnFavorite.setSelected(item.isFavorite());
            applyFavUi(holder.btnFavorite);

            holder.btnFavorite.setOnClickListener(v -> {
                boolean newState = !holder.btnFavorite.isSelected();
                holder.btnFavorite.setSelected(newState);
                item.setFavorite(newState);
                applyFavUi(holder.btnFavorite);

                if (favoriteClickListener != null) {
                    favoriteClickListener.onFavoriteClick(item);
                }
            });
        }

        return convertView;
    }

    /**
     * Updates the visual appearance of the favorite button based on its selection state.
     * @param btn The ImageButton to update.
     */
    private void applyFavUi(ImageButton btn) {
        boolean isFav = btn.isSelected();
        btn.setImageResource(isFav ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
        btn.setContentDescription(isFav ? "Unfavorite" : "Mark as favorite");
    }

    /**
     * Holds view references to avoid repeated findViewById() calls.
     */
    static class ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
        ImageButton btnFavorite;
    }
}
