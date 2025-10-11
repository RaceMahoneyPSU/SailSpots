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

public class MarinaAdapter extends ArrayAdapter<MarinaItem> {

    public interface OnFavoriteClickListener {
        void onFavoriteClick(MarinaItem item);
    }

    private OnFavoriteClickListener favoriteClickListener;

    public void setOnFavoriteClickListener(OnFavoriteClickListener l) {
        this.favoriteClickListener = l;
    }

    public MarinaAdapter(Context ctx, List<MarinaItem> data) {
        super(ctx, 0, data);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_marina, parent, false);
            h = new ViewHolder();
            h.tvTitle = convertView.findViewById(R.id.tvTitle);
            h.tvSubtitle = convertView.findViewById(R.id.tvSubtitle);
            h.btnFavorite = convertView.findViewById(R.id.btnFavorite);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        MarinaItem item = getItem(position);
        if (item != null) {
            h.tvTitle.setText(item.name);
            h.tvSubtitle.setText(String.format(Locale.US, "%.1f mi • %s",
                    item.distanceMiles,
                    item.address == null ? "" : item.address));

            // Reset to default state each bind
            h.btnFavorite.setSelected(false);
            applyFavUi(h.btnFavorite);

            h.btnFavorite.setOnClickListener(v -> {
                ImageButton btn = (ImageButton) v;
                boolean nowFav = !btn.isSelected();
                btn.setSelected(nowFav);
                applyFavUi(btn);

                // 🔹 Only trigger callback when newly favorited
                if (nowFav && favoriteClickListener != null) {
                    favoriteClickListener.onFavoriteClick(item);
                }
            });
        }

        return convertView;
    }

    private void applyFavUi(ImageButton btn) {
        boolean isFav = btn.isSelected();
        btn.setImageResource(isFav ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
        btn.setContentDescription(isFav ? "Unfavorite" : "Mark favorite");
    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
        ImageButton btnFavorite;
    }
}
