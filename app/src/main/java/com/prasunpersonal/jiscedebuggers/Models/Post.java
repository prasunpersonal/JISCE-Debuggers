package com.prasunpersonal.jiscedebuggers.Models;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

import org.parceler.Parcel;

import java.util.HashMap;
import java.util.Objects;

@Parcel
public class Post {
    public static final int ACCESS_PRIVATE = 0;
    public static final int ACCESS_PUBLIC = 1;
    private String postText, postTitle, postLanguage, postID, postAuthor;
    private long postTime;
    private int access;
    private HashMap<String, Integer> ratings;

    public Post() {}

    public Post(@NonNull String postTitle, @NonNull String postLanguage, @NonNull String postText, int access) {
        this.postText = postText;
        this.postTitle = postTitle;
        this.postLanguage = postLanguage;
        this.postTime = System.currentTimeMillis();
        this.ratings = new HashMap<>();
        this.access = access;
        this.postAuthor = FirebaseAuth.getInstance().getUid();
        this.postID = postTime + "_" + FirebaseAuth.getInstance().getUid();
    }

    public String getPostText() {
        return postText;
    }

    public void setPostText(String postText) {
        this.postText = postText;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public String getPostID() {
        return postID;
    }

    public void setPostID(String postID) {
        this.postID = postID;
    }

    public long getPostTime() {
        return postTime;
    }

    public void setPostTime(long postTime) {
        this.postTime = postTime;
    }

    public String getPostAuthor() {
        return postAuthor;
    }

    public void setPostAuthor(String postAuthor) {
        this.postAuthor = postAuthor;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostLanguage() {
        return postLanguage;
    }

    public void setPostLanguage(String postLanguage) {
        this.postLanguage = postLanguage;
    }

    public HashMap<String, Integer> getRatings() {
        return ratings;
    }

    public void setRatings(HashMap<String, Integer> ratings) {
        this.ratings = ratings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post)) return false;
        Post post = (Post) o;
        return getPostID().equals(post.getPostID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPostID());
    }
}
