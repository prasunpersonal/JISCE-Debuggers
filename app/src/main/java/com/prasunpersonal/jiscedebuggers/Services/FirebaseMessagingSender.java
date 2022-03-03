package com.prasunpersonal.jiscedebuggers.Services;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.prasunpersonal.jiscedebuggers.Models.Notification;
import com.prasunpersonal.jiscedebuggers.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FirebaseMessagingSender {
    private static final String TAG = FirebaseMessagingSender.class.getSimpleName();
    public static final String DEFAULT_TOPICS = "DEFAULT_TOPICS";
    public static final int USER_RATING_UPDATE = -1;
    public static final int USER_UPDATE = 0;
    public static final int POST_UPDATE = 1;
    public static final int DEFAULT_COMMENT_UPDATE = 2;
    public static final int REPLY_COMMENT_UPDATE = 3;
    public static final int REPLY_COMMENT_USER_UPDATE = 4;
    public static final int COMMENT_LIKE_UPDATE = 5;
    public static final int POST_RATING_UPDATE = 6;
    public static final int POST_QUESTION = 7;
    public static final int POST_ANSWER = 8;

    Context context;
    int contentType;
    String topic, title, body, senderID, contentId;


    public FirebaseMessagingSender(Context context, String topic, String body, int contentType, String contentID) {
        this.context = context;
        this.topic = topic;
        this.title = context.getString(R.string.app_name);
        this.body = body;
        this.senderID = FirebaseAuth.getInstance().getUid();
        this.contentType = contentType;
        this.contentId = contentID;
    }

    public void sendMessage() {
        Notification notification = new Notification(title, body, senderID, contentId, contentType);
        if (topic.equals(DEFAULT_TOPICS)) {
            FirebaseFirestore.getInstance().collection("Users").whereNotEqualTo("userID", senderID).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot doc : task.getResult()){
                        FirebaseFirestore.getInstance().collection("Users").document(doc.getId()).collection("Notifications").document(notification.getNotificationId()).set(notification);
                    }
                }
            });
        } else {
            if (!topic.equals(senderID)) {
                FirebaseFirestore.getInstance().collection("Users").document(topic).collection("Notifications").document(notification.getNotificationId()).set(notification);
            }
        }

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject message = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            message.put("to", "/topics/"+ topic);
            data.put("title", title);
            data.put("body", body);
            data.put("senderID", FirebaseAuth.getInstance().getUid());
            data.put("contentType", contentType);
            data.put("contentID", contentId);
            message.put("data", data);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, context.getResources().getString(R.string.SERVER_POST_URL), message, response -> {
                Log.d(TAG, "sendMessage: "+ response);
            }, error -> {
                Log.d(TAG, "sendMessage: ", error);
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> header = new HashMap<>();
                    header.put("content-type", "application/json");
                    header.put("authorization", "key=" + context.getResources().getString(R.string.MESSAGING_SERVER_KEY));
                    return header;
                }
            };
            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
