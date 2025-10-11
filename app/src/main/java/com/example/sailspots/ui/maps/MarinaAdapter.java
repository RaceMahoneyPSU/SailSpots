package com.example.sailspots.ui.maps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.sailspots.R;

import java.util.List;
import java.util.Locale;

public class MarinaAdapter extends ArrayAdapter<MarinaItem> {

    public MarinaAdapter(Context ctx, List<MarinaItem> data) {
        super(ctx, 0, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MarinaItem item = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_marina, parent, false);
        }
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = convertView.findViewById(R.id.tvSubtitle);

        if (item != null) {
            tvTitle.setText(item.name);
            tvSubtitle.setText(String.format(Locale.US, "%.1f mi • %s", item.distanceMiles, item.address == null ? "" : item.address));
        }
        return convertView;
    }
}
