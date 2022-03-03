package com.prasunpersonal.jiscedebuggers.Models;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;

public class Notification {
    public static final String user_update = "New user update.";
    public static final String post_update = "New post update.";
    public static final String rating_update = "New rating update.";
    public static final String comment_update = "New comment update.";

    private String notificationId, title, subTitle, body, contentId, senderId;
    private int contentType;
    private long time;
    private boolean checked;

    public Notification() {}

    public Notification(String title, String body, String senderId, String contentId, int contentType) {
        this.notificationId = String.format(Locale.getDefault(), "%d_%d_%d", System.currentTimeMillis(), new Random().nextLong(), new Random().nextLong());
        this.title = title;
        this.subTitle = (contentType == FirebaseMessagingSender.POST_UPDATE || contentType == FirebaseMessagingSender.POST_RATING_UPDATE) ? post_update : (contentType == FirebaseMessagingSender.USER_UPDATE) ? user_update : comment_update;
        this.body = body;
        this.senderId = senderId;
        this.contentId = contentId;
        this.contentType = contentType;
        this.time = System.currentTimeMillis();
        this.checked = false;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isChecked() {
        return checked;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        Notification that = (Notification) o;
        return getNotificationId().equals(that.getNotificationId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNotificationId());
    }
}
