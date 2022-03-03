package com.prasunpersonal.jiscedebuggers.Services;

import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.NOTIFICATION_CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;
import java.util.Random;

import com.prasunpersonal.jiscedebuggers.Activities.CommentActivity;
import com.prasunpersonal.jiscedebuggers.Activities.FullPostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.ProfileOthersActivity;
import com.prasunpersonal.jiscedebuggers.Activities.QuestionAnswerActivity;
import com.prasunpersonal.jiscedebuggers.Models.Notification;
import com.prasunpersonal.jiscedebuggers.R;

@SuppressLint("UnspecifiedImmutableFlag")
public class FirebaseMessagingReceiver extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("TAG", "onMessageReceived: " + remoteMessage.getData());

        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String senderID = remoteMessage.getData().get("senderID");
        int contentType = Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("contentType")));
        String contentId = remoteMessage.getData().get("contentID");

        if (!Objects.equals(senderID, FirebaseAuth.getInstance().getUid())) {
            Intent resultIntent;
            if (ME != null) {
                if (contentType == FirebaseMessagingSender.USER_UPDATE) {
                    resultIntent = new Intent(this, ProfileOthersActivity.class).putExtra("USER_ID", contentId);
                } else if (contentType == FirebaseMessagingSender.POST_UPDATE || contentType == FirebaseMessagingSender.POST_RATING_UPDATE) {
                    resultIntent = new Intent(this, FullPostActivity.class).putExtra("POST_ID", contentId);
                } else if (contentType == FirebaseMessagingSender.USER_RATING_UPDATE) {
                    resultIntent = new Intent(this, ProfileOthersActivity.class).putExtra("USER_ID", contentId);
                } else if (contentType == FirebaseMessagingSender.POST_QUESTION || contentType == FirebaseMessagingSender.POST_ANSWER) {
                    resultIntent = new Intent(this, QuestionAnswerActivity.class).putExtra("FAQ_ID", contentId);
                } else {
                    resultIntent = new Intent(this, CommentActivity.class).putExtra("POST_ID", contentId);
                }
            } else {
                resultIntent = new Intent(this, NotificationLoadingScreenActivity.class).putExtra("ACTION_TYPE", contentType).putExtra("CONTENT_ID", contentId);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            android.app.Notification android_notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setAutoCancel(true)
                    .build();
            NotificationManagerCompat.from(this).notify(new Random().nextInt(), android_notification);
        }
    }


}
