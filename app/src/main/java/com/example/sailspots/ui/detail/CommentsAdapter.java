package com.example.sailspots.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sailspots.R;

/**
 * An adapter for displaying a list of comments in a RecyclerView.
 * This adapter uses ListAdapter for efficient updates, automatically handling
 * list changes with animations.
 */
public class CommentsAdapter extends ListAdapter<CommentItem, CommentsAdapter.VH> {

    /**
     * Constructor for the adapter.
     * It passes the DiffUtil.ItemCallback to the superclass constructor.
     */
    public CommentsAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * A static DiffUtil.ItemCallback instance.
     * This is a performance optimization that helps the ListAdapter determine
     * which items in a list have changed, added, removed, or moved.
     */
    private static final DiffUtil.ItemCallback<CommentItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CommentItem>() {
                /**
                 * Called to check whether two objects represent the same item.
                 * For example, if your items have unique IDs, this method should check if their IDs are the same.
                 */
                @Override
                public boolean areItemsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
                    // Use the unique document ID to check if two items are the same.
                    return oldItem.id.equals(newItem.id);
                }

                /**
                 * Called to check whether two items have the same data.
                 * This method is only called if areItemsTheSame() returns true.
                 * It checks if the visual representation of the item has changed.
                 */
                @Override
                public boolean areContentsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
                    // Check if all the visual content of the item is identical.
                    return oldItem.rating == newItem.rating
                            && oldItem.text.equals(newItem.text)
                            && oldItem.author.equals(newItem.author)
                            // A null-safe check for the timestamp.
                            && ((oldItem.createdAt == null && newItem.createdAt == null) ||
                            (oldItem.createdAt != null && oldItem.createdAt.equals(newItem.createdAt)));
                }
            };

    /**
     * Called when the RecyclerView needs a new ViewHolder to represent an item.
     * This is where we inflate the layout for a single list item.
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom layout (row_comment.xml) for a single list item.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_comment, parent, false);
        // Create and return a new ViewHolder with the inflated view.
        return new VH(v);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the ViewHolder's view.
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Get the CommentItem at the current position and bind its data to the ViewHolder.
        holder.bind(getItem(position));
    }

    /**
     * A ViewHolder describes an item view and holds references to the UI elements
     * within that view (e.g., TextViews, RatingBar). This is a performance optimization
     * that avoids repeated calls to findViewById().
     */
    static class VH extends RecyclerView.ViewHolder {
        // --- UI components for a single comment row ---
        final TextView tvAuthor;
        final TextView tvDate;
        final TextView tvComment;
        final RatingBar ratingBar;
        final TextView tvAvatarInitial;

        /**
         * Constructor for the ViewHolder.
         * @param itemView The root view for a single list item (the inflated card layout).
         */
        VH(@NonNull View itemView) {
            super(itemView);
            // Find and cache the views from the item's layout once.
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            tvComment = itemView.findViewById(R.id.tvCommentText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
        }

        /**
         * Binds the data from a CommentItem object to the views in the ViewHolder.
         * @param item The CommentItem containing the data to display.
         */
        void bind(CommentItem item) {
            // Set the text and rating for the views.
            tvAuthor.setText(item.author);
            tvDate.setText(item.dateLabel);
            tvComment.setText(item.text);
            ratingBar.setRating(item.rating);

            // Set the avatar's text to the first letter of the author's name.
            if (item.author != null && !item.author.isEmpty()) {
                tvAvatarInitial.setText(item.author.substring(0, 1).toUpperCase());
            } else {
                // Use a fallback character if the author name is missing.
                tvAvatarInitial.setText("?");
            }
        }
    }
}
