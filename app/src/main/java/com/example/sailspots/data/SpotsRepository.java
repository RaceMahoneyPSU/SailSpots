package com.example.sailspots.data;

import com.example.sailspots.models.SpotsItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.SetOptions;


import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Repository class for handling all data operations related to 'Spots' in Firestore.
 * This class encapsulates the logic for adding, deleting, and retrieving spot data.
 */
public class SpotsRepository {

    // Get the singleton instance of FirebaseFirestore.
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Configures Firestore settings, such as enabling unlimited persistent cache.
     * This allows the app to work offline by caching Firestore data locally.
     */
    public void onCreate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Define settings to enable an unlimited-size cache for offline data access.
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                        // Use unlimited cache size.
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build())
                .build();
        db.setFirestoreSettings(settings);
    }


    /**
     * Gets a reference to the 'spots' collection for the currently logged-in user.
     * If no user is logged in, it defaults to a collection for an "anonymous" user.
     * @return A CollectionReference pointing to the user's spots.
     */
    private CollectionReference spotsCol() {
        // Get the current user's UID.
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous"; // Fallback for anonymous or unauthenticated users.
        // Return the reference to the sub-collection: /users/{uid}/spots
        return db.collection("users").document(uid).collection("spots");
    }

    /**
     * Sets up a real-time listener on the spots collection to get a set of favorite place IDs.
     * This is useful for quickly checking which spots are marked as favorites.
     * @param onIds A callback function that receives the set of place IDs.
     * @param onErr A callback function to handle any errors.
     * @return A ListenerRegistration object which can be used to detach the listener.
     */
    public ListenerRegistration listenFavoriteIds(Consumer<Set<String>> onIds,
                                                  Consumer<Exception> onErr) {
        // Attach a snapshot listener that fires whenever the collection changes.
        return spotsCol().addSnapshotListener((snap, e) -> {
            if (e != null) { onErr.accept(e); return; } // Handle errors.
            Set<String> ids = new HashSet<>();
            if (snap != null) {
                // Loop through all documents in the snapshot.
                for (DocumentSnapshot d : snap.getDocuments()) {
                    SpotsItem s = d.toObject(SpotsItem.class);
                    // Add the placeId to the set if it exists.
                    if (s != null && s.getPlaceId() != null) ids.add(s.getPlaceId());
                }
            }
            onIds.accept(ids); // Pass the resulting set of IDs to the callback.
        });
    }

    /**
     * Adds a new spot document to the user's collection.
     * @param item The SpotsItem object to add.
     * @param onOk A callback to run on successful addition.
     * @param onErr A callback to handle any errors.
     */
    public void addSpot(SpotsItem item, Runnable onOk, Consumer<Exception> onErr) {
        spotsCol().add(item)
                .addOnSuccessListener(docRef -> onOk.run())
                .addOnFailureListener(onErr::accept);
    }

    /**
     * Updates an existing spot or creates a new one if it doesn't exist (upsert).
     * Uses the document ID to find the spot.
     * @param docId The specific document ID of the spot to update/create.
     * @param item The SpotsItem object with new data.
     * @param onOk A callback to run on success.
     * @param onErr A callback to handle any errors.
     */
    public void upsertSpotById(String docId, SpotsItem item,
                               Runnable onOk, Consumer<Exception> onErr) {
        spotsCol().document(docId)
                // SetOptions.merge() updates only the fields in the item object.
                .set(item, SetOptions.merge())
                .addOnSuccessListener(v -> onOk.run())
                .addOnFailureListener(onErr::accept);
    }

    /**
     * Deletes a spot document from the collection using its document ID.
     * @param id The ID of the document to delete.
     * @param onOk A callback to run on successful deletion.
     * @param onErr A callback to handle any errors.
     */
    public void deleteSpot(String id, Runnable onOk, Consumer<Exception> onErr) {
        spotsCol().document(id).delete()
                .addOnSuccessListener(v -> onOk.run())
                .addOnFailureListener(onErr::accept);
    }

    /**
     * Deletes a spot document from the collection using its document ID.
     * @param docId The ID of the document to delete.
     * @param onOk A callback to run on successful deletion.
     * @param onErr A callback to handle any errors.
     */
    public void deleteSpotById(String docId, Runnable onOk, Consumer<Exception> onErr) {
        spotsCol().document(docId)
                .delete()
                .addOnSuccessListener(v -> onOk.run())
                .addOnFailureListener(onErr::accept);
    }

    /**
     * Retrieves a single spot document by its ID.
     * @param id The ID of the document to retrieve.
     * @param onOk A callback function that receives the retrieved SpotsItem.
     * @param onErr A callback to handle any errors.
     */
    public void getSpot(String id, Consumer<SpotsItem> onOk, Consumer<Exception> onErr) {
        spotsCol().document(id).get()
                // On success, convert the document to a SpotsItem object and pass to the callback.
                .addOnSuccessListener(doc -> onOk.accept(doc.toObject(SpotsItem.class)))
                .addOnFailureListener(onErr::accept);
    }


}
