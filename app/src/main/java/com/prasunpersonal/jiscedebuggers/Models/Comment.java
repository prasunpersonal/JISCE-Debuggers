package com.prasunpersonal.jiscedebuggers.Models;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

@Parcel
public class Comment {
    private String commentText, commentID, commentAuthor, parentId;
    private long commentTime;
    private ArrayList<String> likedBy;

    public Comment() {}

    public Comment(String commentAuthor, String commentText, String parentId) {
        this.commentAuthor = commentAuthor;
        this.commentText = commentText;
        this.commentTime = System.currentTimeMillis();
        this.parentId = parentId;
        this.commentID = this.commentTime + "_" + new Random().nextLong();
        this.likedBy = new ArrayList<>();
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getCommentID() {
        return commentID;
    }

    public void setCommentID(String commentID) {
        this.commentID = commentID;
    }

    public String getCommentAuthor() {
        return commentAuthor;
    }

    public void setCommentAuthor(String commentAuthor) {
        this.commentAuthor = commentAuthor;
    }

    public long getCommentTime() {
        return commentTime;
    }

    public void setCommentTime(long commentTime) {
        this.commentTime = commentTime;
    }

    public ArrayList<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(ArrayList<String> likedBy) {
        this.likedBy = likedBy;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getCommentID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment = (Comment) o;
        return this.getCommentID().equals(comment.getCommentID());
    }
}
