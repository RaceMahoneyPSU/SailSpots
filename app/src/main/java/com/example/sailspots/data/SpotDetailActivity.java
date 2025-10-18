package com.example.sailspots.data;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sailspots.R;

public class SpotDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_detail);

        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);
        double miles = getIntent().getDoubleExtra("miles", 0);

        ((TextView)findViewById(R.id.tvName)).setText(name);
        ((TextView)findViewById(R.id.tvAddr)).setText(address);
        ((TextView)findViewById(R.id.tvCoords)).setText(lat + ", " + lng);
        ((TextView)findViewById(R.id.tvMiles)).setText(String.format("%.1f miles", miles));
    }
}
