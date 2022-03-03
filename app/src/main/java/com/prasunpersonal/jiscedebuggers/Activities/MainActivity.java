package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(() -> findViewById(R.id.progressBar).setVisibility(View.VISIBLE), 1000);

        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            new Handler().postDelayed(() -> {
                startActivity(new Intent(MainActivity.this, SignupLogin.class));
                overridePendingTransition(R.anim.slide_up, R.anim.no_anim);
                finish();
            }, 1000);
        }else {
            FirebaseFirestore.getInstance().collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()){
                    ME = task.getResult().toObject(User.class);
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                }else {
                    Toast.makeText(this, "Something went wrong! Please login again.", Toast.LENGTH_SHORT).show();
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, SignupLogin.class));
                }
                finish();
            });
        }
    }
}