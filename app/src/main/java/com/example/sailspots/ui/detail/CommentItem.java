package com.example.sailspots.ui.detail;

public class CommentItem {
    public final String author;
    public final int rating;
    public final String text;
    public final String dateLabel;

    public CommentItem(String author, int rating, String text, String dateLabel) {
        this.author = author;
        this.rating = rating;
        this.text = text;
        this.dateLabel = dateLabel;
    }
}
