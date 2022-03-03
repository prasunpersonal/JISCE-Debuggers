package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.LANGUAGE_TO_EXTENSION_MAP;
import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.NOTIFICATION_CHANNEL_ID;
import static com.prasunpersonal.jiscedebuggers.App.getFileSize;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.prasunpersonal.jiscedebuggers.Adapters.PostAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityProfileOthersBinding;
import com.prasunpersonal.jiscedebuggers.databinding.RatingDialogLayoutBinding;
import com.prasunpersonal.jiscedebuggers.databinding.ShowImageBinding;

import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProfileOthersActivity extends AppCompatActivity {
    private static final String TAG = ProfileOthersActivity.class.getSimpleName();
    String userId;
    User user;
    ArrayList<Post> posts;
    ActivityProfileOthersBinding binding;
    AlertDialog ratingDialog;
    RatingDialogLayoutBinding ratingBinding;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileOthersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userId = getIntent().getStringExtra("USER_ID");

        posts = new ArrayList<>();

        ratingBinding = RatingDialogLayoutBinding.inflate(getLayoutInflater());
        ratingDialog = new AlertDialog.Builder(this).setView(ratingBinding.getRoot()).setCancelable(false).create();

        binding.profileOthersToolbar.setNavigationOnClickListener(v -> finish());

        binding.profilePosts.setLayoutManager(new LinearLayoutManager(this));
        binding.profilePosts.setAdapter(new PostAdapter(posts, new PostAdapter.setOnEventListeners() {
            @Override
            public void OnPostClickListeners(Post post) {
                startActivity(new Intent(ProfileOthersActivity.this, FullPostActivity.class).putExtra("POST_ID", post.getPostID()));
            }

            @Override
            public void OnCommentClickListener(Post post) {
                startActivity(new Intent(ProfileOthersActivity.this, CommentActivity.class).putExtra("POST_ID", post.getPostID()));
            }

            @Override
            public void OnAuthorClick(User author) {
                startActivity(new Intent(ProfileOthersActivity.this, ProfileOthersActivity.class).putExtra("USER_ID", author.getUserID()));
            }

            @Override
            public void OnClickDownload(Post post) {
                checkPermissionAndSaveFile(post);
            }

            @Override
            public void OnClickEdit(Post post) {
                startActivity(new Intent(ProfileOthersActivity.this, CreatePostActivity.class).putExtra("EDITABLE_POST", Parcels.wrap(post)));
            }

            @Override
            public void OnReceivedSearchCount(int count) {

            }
        }));
        assert binding.profilePosts.getAdapter() != null;

        FirebaseFirestore.getInstance().collection("Users").document(userId).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "updateUi: ", error);
                return;
            }
            if (value != null && value.exists()) {
                user = value.toObject(User.class);
                assert user != null;
                binding.profileOthersToolbar.setTitle(user.getName());
                binding.profileOthersToolbar.setSubtitle(getString(R.string.app_name));
                try {
                    Glide.with(this).load(user.getProfilePic()).into(binding.profilePic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                binding.name.setText(user.getName());
                binding.email.setText(user.getEmail());
                binding.joined.setText(String.format(Locale.getDefault(), "Joined on %s", new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(user.getJoined()))));
                binding.postCount.setText(String.valueOf(user.getPostCount()));
                float rating = (float) user.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                binding.rating.setRating(rating);
                binding.ratingCount.setText(String.format(Locale.getDefault(), "%.01f", rating));
                binding.profilePic.setOnClickListener(v -> showImg(user.getProfilePic()));
                binding.ratingLayout.setOnClickListener(v -> {
                    try {
                        Glide.with(this).load(user.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(ratingBinding.ratingDP);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ratingBinding.ratingText.setText(user.getName());
                    ratingBinding.cancel.setOnClickListener(v1 -> ratingDialog.dismiss());
                    ratingBinding.submit.setOnClickListener(v1 -> {
                        if (ratingBinding.ratingBar.getRating() == 0) {
                            Toast.makeText(this, "Please set rating before submitting.", Toast.LENGTH_SHORT).show();
                        } else {
                            user.getRatings().put(FirebaseAuth.getInstance().getUid(), (int) ratingBinding.ratingBar.getRating());
                            FirebaseFirestore.getInstance().collection("Users").document(userId).update("ratings", user.getRatings()).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    new FirebaseMessagingSender(this, user.getUserID(), String.format(getString(R.string.rating_notification_template), ME.getName(), (int) ratingBinding.ratingBar.getRating()), FirebaseMessagingSender.USER_RATING_UPDATE, ME.getUserID()).sendMessage();
                                }
                            });
                            ratingDialog.dismiss();
                        }
                    });
                    ratingDialog.show();
                });
            } else {
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        FirebaseFirestore.getInstance().collection("Posts").whereEqualTo("postAuthor", userId).orderBy("postTime").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "updateUi: ", error);
                return;
            }
            if (value != null) {
                for (DocumentChange dc : value.getDocumentChanges()) {
                    Post post = dc.getDocument().toObject(Post.class);
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        posts.add(0, post);
                        binding.profilePosts.getAdapter().notifyItemInserted(0);
                    } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                        int i = posts.indexOf(post);
                        posts.remove(i);
                        binding.profilePosts.getAdapter().notifyItemRemoved(i);
                    } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                        int i = posts.indexOf(post);
                        posts.set(i, post);
                        binding.profilePosts.getAdapter().notifyItemChanged(i);
                    }
                }
            }
        });
    }

    @Override
    public void finish() {
        if (!user.getRatings().containsKey(FirebaseAuth.getInstance().getUid())) {
            try {
                Glide.with(getApplicationContext()).load(user.getProfilePic()).into(ratingBinding.ratingDP);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ratingBinding.ratingText.setText(String.format(getResources().getString(R.string.rating_text), user.getName()));
            ratingBinding.cancel.setOnClickListener(v1 -> {
                ratingDialog.dismiss();
                super.finish();
            });
            ratingBinding.submit.setOnClickListener(v1 -> {
                if (ratingBinding.ratingBar.getRating() == 0) {
                    Toast.makeText(this, "Please set rating before submitting.", Toast.LENGTH_SHORT).show();
                } else {
                    user.getRatings().put(FirebaseAuth.getInstance().getUid(), (int) ratingBinding.ratingBar.getRating());
                    FirebaseFirestore.getInstance().collection("Users").document(userId).update("ratings", user.getRatings()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            new FirebaseMessagingSender(this, user.getUserID(), String.format(getString(R.string.rating_notification_template), ME.getName(), (int) ratingBinding.ratingBar.getRating()), FirebaseMessagingSender.USER_RATING_UPDATE, ME.getUserID()).sendMessage();
                        }
                    });
                    ratingDialog.dismiss();
                    super.finish();
                }
            });
            ratingDialog.show();
        } else {
            super.finish();
        }
    }

    private void showImg(String uri){
        ShowImageBinding siBinding = ShowImageBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(siBinding.getRoot())
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(ActivityCompat.getDrawable(this, R.drawable.dialog_bg));

        siBinding.editableArea.setVisibility(View.GONE);

        try {
            Glide.with(getApplicationContext()).load(uri).placeholder(R.drawable.ic_user_circle).into(siBinding.showImg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        siBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        siBinding.btnUpload.setOnClickListener(v -> {
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            final NotificationCompat.Builder notification_builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Uploading Images")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true);
            notificationManager.notify(1, notification_builder.build());

            StorageReference storageRef = FirebaseStorage.getInstance().getReference(getResources().getString(R.string.app_name)).child(ME.getUserID()).child("profile_image");
            storageRef.putFile(Uri.parse(uri)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri1 -> FirebaseFirestore.getInstance().collection("Users").document(ME.getUserID()).update("profilePic", uri1.toString()).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()){
                            Toast.makeText(this, "Profile photo updated successfully.", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(this, "Cant update profile photo: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                }
                notificationManager.cancel(1);
            }).addOnProgressListener(snapshot -> {
                notification_builder.setContentText(getFileSize(snapshot.getBytesTransferred()) + "/" + getFileSize(snapshot.getTotalByteCount()))
                        .setProgress((int) snapshot.getTotalByteCount(), (int) snapshot.getBytesTransferred(), false);
                notificationManager.notify(1, notification_builder.build());
            });
            dialog.dismiss();
        });
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void showSettingsDialog() {
        if (alertDialog == null || !alertDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.ic_folder);
            builder.setTitle("Storage Permission");
            builder.setMessage("This app needs all files access permission. You can enable it in app settings.");
            builder.setCancelable(false);
            builder.setPositiveButton("Settings", (dialog, which) -> {
                dialog.cancel();
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    permissionRequest.launch(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    permissionRequest.launch(intent);
                }
            });
            builder.setNegativeButton("Exit", (dialog, which) -> dialog.cancel());

            alertDialog = builder.create();
            alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray)));
            alertDialog.show();
        }
    }

    private void checkPermissionAndSaveFile(Post post) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                saveToFile(post);
            } else {
                showSettingsDialog();
            }
        } else {
            Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                    saveToFile(post);
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).onSameThread().check();
        }
    }

    private void saveToFile(Post post) {
        File filePath = new File(String.format(Locale.getDefault(), "%s/%s/Files", Environment.getExternalStorageDirectory(), getString(R.string.app_name)));
        String fileName = String.format(Locale.getDefault(), "%s - %s%s", post.getPostTitle(), post.getPostLanguage(), LANGUAGE_TO_EXTENSION_MAP.get(post.getPostLanguage()));
        if (filePath.mkdirs()) Log.d(TAG, "File path created.");
        File file = new File(filePath, fileName);
        try {
            FileOutputStream fis = new FileOutputStream(file);
            fis.write(post.getPostText().getBytes());
            fis.close();
            Toast.makeText(this, "File saved to: " + file.getPath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    ActivityResultLauncher<Intent> permissionRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    });
}