package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.LANGUAGE_TO_EXTENSION_MAP;
import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Adapters.PostAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.ActivitySavedPostsBinding;

public class SavedPostsActivity extends AppCompatActivity {
    private static final String TAG = SavedPostsActivity.class.getSimpleName();
    ActivitySavedPostsBinding binding;
    ArrayList<Post> posts;
    ArrayList<String> filters;
    private AlertDialog alertDialog;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.savedPostsToolbar);

        posts = new ArrayList<>();
        filters = new ArrayList<>();
        binding.savedPostsToolbar.setNavigationOnClickListener(v -> finish());
        binding.allPosts.setLayoutManager(new LinearLayoutManager(this));

        updateUi();

        binding.savedPostRefresh.setOnRefreshListener(this::updateUi);
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

    private void updateUi() {
        binding.savedPostRefresh.setRefreshing(true);
        posts.clear();
        if (ME.getSavedPosts().isEmpty()) {
            Toast.makeText(this, "You don't have any saved posts.", Toast.LENGTH_SHORT).show();
        } else {
            FirebaseFirestore.getInstance().collection("Posts").whereIn("postID", ME.getSavedPosts()).orderBy("postTime").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot doc : task.getResult()) {
                        Post post = doc.toObject(Post.class);
                        assert post!= null;
                        posts.add(post);
                    }
                    binding.allPosts.setAdapter(new PostAdapter(posts, new PostAdapter.setOnEventListeners() {
                        @Override
                        public void OnPostClickListeners(Post post) {
                            startActivity(new Intent(SavedPostsActivity.this, FullPostActivity.class).putExtra("POST_ID", post.getPostID()));
                        }

                        @Override
                        public void OnCommentClickListener(Post post) {
                            startActivity(new Intent(SavedPostsActivity.this, CommentActivity.class).putExtra("POST_ID", post.getPostID()));
                        }

                        @Override
                        public void OnAuthorClick(User author) {
                            startActivity(new Intent(SavedPostsActivity.this, ProfileOthersActivity.class).putExtra("USER_ID", author.getUserID()));
                        }

                        @Override
                        public void OnClickDownload(Post post) {
                            checkPermissionAndSaveFile(post);
                        }

                        @Override
                        public void OnClickEdit(Post post) {
                            startActivity(new Intent(SavedPostsActivity.this, CreatePostActivity.class).putExtra("EDITABLE_POST", Parcels.wrap(post)));
                        }

                        @Override
                        public void OnReceivedSearchCount(int count) {

                        }
                    }));
                } else {
                    Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
                binding.savedPostRefresh.setRefreshing(false);
            });
        }
    }
}