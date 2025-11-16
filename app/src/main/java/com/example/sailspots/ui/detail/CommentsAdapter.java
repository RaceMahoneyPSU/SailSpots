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

public class CommentsAdapter extends ListAdapter<CommentItem, CommentsAdapter.VH> {

    public CommentsAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<CommentItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CommentItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
                    return oldItem.author.equals(newItem.author) && oldItem.dateLabel.equals(newItem.dateLabel);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
                    return oldItem.rating == newItem.rating && oldItem.text.equals(newItem.text);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_comment, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }


    static class VH extends RecyclerView.ViewHolder {
        final TextView tvAuthor;
        final TextView tvDate;
        final TextView tvComment;
        final RatingBar ratingBar;
        final TextView tvAvatarInitial;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            tvComment = itemView.findViewById(R.id.tvCommentText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
        }

        void bind(CommentItem item) {
            tvAuthor.setText(item.author);
            tvDate.setText(item.dateLabel);
            tvComment.setText(item.text);
            ratingBar.setRating(item.rating);

            // Avatar initial: first letter of name
            if (item.author != null && !item.author.isEmpty()) {
                tvAvatarInitial.setText(item.author.substring(0, 1).toUpperCase());
            } else {
                tvAvatarInitial.setText("?");
            }
        }
    }
}

