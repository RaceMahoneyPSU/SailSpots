package com.example.sailspots.ui.detail;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sailspots.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MarinaDetailActivity extends AppCompatActivity {
    public static final String EXTRA_MARINA_NAME = "extra_marina_name";
    public static final String EXTRA_MARINA_ADDRESS = "extra_marina_address";
    public static final String EXTRA_PLACE_ID = "extra_place_id";
    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";

    private RecyclerView rvComments;
    private CommentsAdapter commentsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marina_detail);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // Top title and subtitle
        TextView tvMarinaName = findViewById(R.id.tvMarinaName);
        TextView tvMarinaAddress = findViewById(R.id.tvMarinaAddress);

        String name = getIntent().getStringExtra(EXTRA_MARINA_NAME);
        String address = getIntent().getStringExtra(EXTRA_MARINA_ADDRESS);

        tvMarinaName.setText(name != null ? name : "Unknown");
        tvMarinaAddress.setText(address != null ? address : "Unknown");

        // Lat/Lng for future weather integration
        double lat = getIntent().getDoubleExtra(EXTRA_LAT, Double.NaN);
        double lng = getIntent().getDoubleExtra(EXTRA_LNG, Double.NaN);

        // ---- Weather mock hookup (for now just fake data) ----
        TextView tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        TextView tvWeatherCondition = findViewById(R.id.tvWeatherCondition);
        TextView tvWeatherHiLo = findViewById(R.id.tvWeatherHiLo);
        TextView tvWeatherWind = findViewById(R.id.tvWeatherWind);
        TextView tvWeatherExtra = findViewById(R.id.tvWeatherExtra);

        // TODO Later: replace with real API call using lat/lng
        tvWeatherTemp.setText("78°");
        tvWeatherCondition.setText("Partly cloudy");
        tvWeatherHiLo.setText("H 82°  •  L 72°");
        tvWeatherWind.setText("Wind 9 kt");
        tvWeatherExtra.setText("Tide: rising");

        // RecyclerView for comments
        rvComments = findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter();
        rvComments.setAdapter(commentsAdapter);

        // Add fake comments for testing
        commentsAdapter.submitList(seedDummyComments());
    }

    private List<CommentItem> seedDummyComments() {
        List<CommentItem> items = new ArrayList<>();
        items.add(new CommentItem("Joe Smith", 5, "Amazing dock!", "Jan 5"));
        items.add(new CommentItem("Jane Doe", 3, "Parking is so convenient", "Jan 3"));
        return items;
    }

}
