package com.example.sailspots.ui.detail;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sailspots.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity that displays the detailed information for a single marina.
 * It shows the marina's name, address, weather info, and a list of user reviews.
 */
public class MarinaDetailActivity extends AppCompatActivity {
    // --- Constants for passing data via Intents ---
    public static final String EXTRA_MARINA_NAME = "extra_marina_name";
    public static final String EXTRA_MARINA_ADDRESS = "extra_marina_address";
    public static final String EXTRA_PLACE_ID = "extra_place_id";
    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";

    // --- UI Components ---
    private RecyclerView rvComments;
    private CommentsAdapter commentsAdapter;

    // --- Firebase Firestore ---
    private FirebaseFirestore db;
    private CollectionReference commentsRef; // Reference to the 'comments' sub-collection for this marina.
    private ListenerRegistration commentsRegistration; // Listens for real-time updates to comments.
    private String placeId; // The unique ID for the marina (spot) in Firestore.


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity from the XML file.
        setContentView(R.layout.activity_marina_detail);

        // --- Toolbar Setup ---
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // Configure the toolbar to have a back button.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // We use a custom TextView for the title.
        }
        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed() // Handle back button press.
        );

        // --- Get Data from Intent ---
        // Find the TextViews for the marina's name and address.
        TextView tvMarinaName = findViewById(R.id.tvMarinaName);
        TextView tvMarinaAddress = findViewById(R.id.tvMarinaAddress);

        // Retrieve data passed from the previous screen.
        placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        String name = getIntent().getStringExtra(EXTRA_MARINA_NAME);
        String address = getIntent().getStringExtra(EXTRA_MARINA_ADDRESS);

        // --- Firestore Initialization ---
        // If placeId is missing, we can't load or post comments.
        if (placeId == null || placeId.isEmpty()) {
            Toast.makeText(this, "Error: Marina ID is missing.", Toast.LENGTH_LONG).show();
            // Early exit if the required data is not present.
            // return; // Note: Returning here would leave a blank screen. Usually better to show an error state.
        } else {
            // Get a Firestore instance and create a reference to the specific comments collection.
            db = FirebaseFirestore.getInstance();
            commentsRef = db.collection("spots")
                    .document(placeId)
                    .collection("comments");
        }

        // Set the marina's name and address in the UI.
        tvMarinaName.setText(name != null ? name : "Unknown");
        tvMarinaAddress.setText(address != null ? address : "Unknown");

        // Lat/Lng for future weather integration.
        double lat = getIntent().getDoubleExtra(EXTRA_LAT, Double.NaN);
        double lng = getIntent().getDoubleExtra(EXTRA_LNG, Double.NaN);

        // ---- Weather mock hookup (for now just fake data) ----
        TextView tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        TextView tvWeatherCondition = findViewById(R.id.tvWeatherCondition);
        TextView tvWeatherHiLo = findViewById(R.id.tvWeatherHiLo);
        TextView tvWeatherWind = findViewById(R.id.tvWeatherWind);
        TextView tvWeatherExtra = findViewById(R.id.tvWeatherExtra);

        // TODO Later: replace with real API call using lat/lng.
        tvWeatherTemp.setText("78°");
        tvWeatherCondition.setText("Partly cloudy");
        tvWeatherHiLo.setText("H 82°  •  L 72°");
        tvWeatherWind.setText("Wind 9 kt");
        tvWeatherExtra.setText("Tide: rising");

        // --- RecyclerView for comments ---
        rvComments = findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(this)); // Arrange items in a vertical list.
        commentsAdapter = new CommentsAdapter(); // Create the adapter.
        rvComments.setAdapter(commentsAdapter); // Connect the adapter to the RecyclerView.

        // --- Load Comments from Firestore ---
        // Only try to load comments if our Firestore reference was successfully created.
        if (commentsRef != null) {
            // Listen for real-time changes to the comments, ordered by newest first.
            commentsRegistration = commentsRef
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((snap, e) -> {

                        // If there's an error or no data, show some dummy comments.
                        if (e != null || snap == null || snap.isEmpty()) {
                            commentsAdapter.submitList(seedDummyComments());
                            return;
                        }

                        // Convert the Firestore documents into a list of CommentItem objects.
                        List<CommentItem> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            list.add(CommentItem.fromSnapshot(doc));
                        }
                        // Update the adapter with the new list of comments.
                        commentsAdapter.submitList(list);
                    });
        }
        // Set up the "Add Comment" button.
        FloatingActionButton fabAddComment = findViewById(R.id.fabAddComment);
        fabAddComment.setOnClickListener(v -> showAddCommentDialog());
    }

    /**
     * Displays a dialog for the user to add a new review (comment and rating).
     */
    private void showAddCommentDialog() {
        // Don't show the dialog if we can't post a comment.
        if (commentsRef == null) {
            Toast.makeText(this, "Cannot add comments at this time.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Inflate the custom layout for the dialog.
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_comment, null);

        // Find the input views inside the dialog layout.
        RatingBar ratingBar = view.findViewById(R.id.ratingBarInput);
        EditText etName = view.findViewById(R.id.etName);
        EditText etComment = view.findViewById(R.id.etComment);

        // Build the alert dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Review")
                .setView(view)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                // We set a custom listener for the positive button to control when the dialog closes.
                .setPositiveButton("Post", null)
                .create();

        // This listener allows us to add validation before closing the dialog.
        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btnView -> {
                // Get user input from the dialog views.
                int rating = Math.round(ratingBar.getRating());
                String name = etName.getText().toString().trim();
                String commentText = etComment.getText().toString().trim();

                // --- Input Validation ---
                if (rating <= 0) {
                    Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
                    return; // Keep the dialog open.
                }

                if (commentText.isEmpty()) {
                    Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
                    return; // Keep the dialog open.
                }

                // Use a default name if the user leaves it blank.
                if (name.isEmpty()) {
                    name = "Anonymous Sailor";
                }

                // --- Prepare Data for Firestore ---
                Map<String, Object> data = new HashMap<>();
                data.put("authorName", name);
                data.put("rating", rating);
                data.put("text", commentText);
                data.put("createdAt", Timestamp.now()); // Use server timestamp.

                // This ensures the parent 'spot' document exists before adding a sub-collection item.
                if (placeId != null && !placeId.isEmpty()) {
                    db.collection("spots")
                            .document(placeId)
                            .set(new HashMap<String, Object>(), SetOptions.merge());
                }

                // --- Add the comment to Firestore ---
                commentsRef.add(data)
                        .addOnSuccessListener(ref -> {
                            // On success, show a confirmation and close the dialog.
                            Toast.makeText(this, "Review added.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            // On failure, log the error and show a detailed message to the user.
                            android.util.Log.e("MarinaDetailActivity", "Failed to add review", e);
                            Toast.makeText(this,
                                    "Failed to add review: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            });
        });

        // Show the configured dialog.
        dialog.show();
    }

    /**
     * Creates a list of pre-made comments to show when there's no connection
     * or no reviews have been added yet.
     * @return A list of dummy CommentItem objects.
     */
    private List<CommentItem> seedDummyComments() {
        List<CommentItem> list = new ArrayList<>();
        list.add(new CommentItem(
                "seed1",
                "Captain Morgan (EXAMPLE)",
                5,
                "Great spot to launch from — calm water and plenty of space to maneuver.",
                "Jan 8",
                null
        ));
        list.add(new CommentItem(
                "seed2",
                "Sarah Landon (EXAMPLE)",
                4,
                "Super easy access. Dock can get a little busy around sunset, but still manageable.",
                "Jan 5",
                null
        ));
        list.add(new CommentItem(
                "seed3",
                "Mark Rivera (EXAMPLE)",
                3,
                "Parking is decent and the ramp is in good shape. Would love to see more lighting at night.",
                "Jan 3",
                null
        ));
        return list;
    }

    /**
     * Called when the activity is being destroyed.
     * It's important to remove the Firestore listener to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detach the real-time listener if it exists.
        if (commentsRegistration != null) {
            commentsRegistration.remove();
            commentsRegistration = null;
        }
    }
}
