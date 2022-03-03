package com.prasunpersonal.jiscedebuggers.Models;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.HashMap;

@Parcel
public class User {
    private String email, name, phone, profilePic, userID;
    private long joined;
    private int postCount;
    private ArrayList<String> savedPosts;
    private HashMap<String, Integer> ratings;
    private HashMap<String, String> socialAuthProviders;

    public User() {}

    public User(@Nullable String name, @Nullable String email) {
        this.userID = FirebaseAuth.getInstance().getUid();
        this.name = name;
        this.email = email;
        this.joined = System.currentTimeMillis();
        this.profilePic = null;
        this.phone = null;
        this.savedPosts = new ArrayList<>();
        this.ratings = new HashMap<>();
        this.socialAuthProviders = new HashMap<>();
        this.postCount = 0;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ArrayList<String> getSavedPosts() {
        return savedPosts;
    }

    public void setSavedPosts(ArrayList<String> savedPosts) {
        this.savedPosts = savedPosts;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public long getJoined() {
        return joined;
    }

    public void setJoined(long joined) {
        this.joined = joined;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public HashMap<String, Integer> getRatings() {
        return ratings;
    }

    public void setRatings(HashMap<String, Integer> ratings) {
        this.ratings = ratings;
    }

    public HashMap<String, String> getSocialAuthProviders() {
        return socialAuthProviders;
    }

    public void setSocialAuthProviders(HashMap<String, String> socialAuthProviders) {
        this.socialAuthProviders = socialAuthProviders;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    @Override
    public int hashCode() {
        return this.userID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return this.getUserID().equals(user.getUserID());
    }
}
