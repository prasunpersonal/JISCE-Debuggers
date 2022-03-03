package com.prasunpersonal.jiscedebuggers.Services;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Activities.CommentActivity;
import com.prasunpersonal.jiscedebuggers.Activities.FullPostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.MainActivity;
import com.prasunpersonal.jiscedebuggers.Activities.ProfileOthersActivity;
import com.prasunpersonal.jiscedebuggers.Activities.QuestionAnswerActivity;
import com.prasunpersonal.jiscedebuggers.Models.User;

public class NotificationLoadingScreenActivity extends AppCompatActivity {
    int contentType;
    String contentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contentType = getIntent().getIntExtra("ACTION_TYPE",0);
        contentId = getIntent().getStringExtra("CONTENT_ID");

        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            Toast.makeText(this, "Login first to view content.", Toast.LENGTH_SHORT).show();
            finish();
        }
        FirebaseFirestore.getInstance().collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()){
                ME = task.getResult().toObject(User.class);
                if (contentType == FirebaseMessagingSender.USER_UPDATE) {
                    startActivity(new Intent(this, ProfileOthersActivity.class).putExtra("USER_ID", contentId));
                } else if (contentType == FirebaseMessagingSender.POST_UPDATE || contentType == FirebaseMessagingSender.POST_RATING_UPDATE) {
                    startActivity(new Intent(this, FullPostActivity.class).putExtra("POST_ID", contentId));
                } else if (contentType == FirebaseMessagingSender.USER_RATING_UPDATE) {
                    startActivity(new Intent(this, ProfileOthersActivity.class).putExtra("USER_ID", contentId));
                } else if (contentType == FirebaseMessagingSender.POST_QUESTION || contentType == FirebaseMessagingSender.POST_ANSWER) {
                    startActivity(new Intent(this, QuestionAnswerActivity.class).putExtra("FAQ_ID", contentId));
                } else {
                    startActivity(new Intent(this, CommentActivity.class).putExtra("POST_ID", contentId));
                }
            }else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        });
    }
}