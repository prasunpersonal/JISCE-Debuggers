package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.LANGUAGE_TO_EXTENSION_MAP;
import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.RATING_STARS;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityFullPostBinding;
import com.prasunpersonal.jiscedebuggers.databinding.PostRatingDialogBinding;

public class FullPostActivity extends AppCompatActivity {
    private static final String TAG = FullPostActivity.class.getSimpleName();
    ActivityFullPostBinding binding;
    FirebaseFirestore db;
    String postId;
    Post post;
    User author;
    AlertDialog alertDialog;

    ActivityResultLauncher<Intent> permissionRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.fullPostToolbar);
        postId = getIntent().getStringExtra("POST_ID");
        db = FirebaseFirestore.getInstance();

        binding.fullPostToolbar.setNavigationOnClickListener(v -> finish());

        db.collection("Posts").document(postId).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onViewCreated: ", error);
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (value != null && value.exists()) {
                post = value.toObject(Post.class);
                db.collection("Users").document(post.getPostAuthor()).addSnapshotListener((value1, error1) -> {
                    if (error1 != null) {
                        Log.d(TAG, "onBindViewHolder: ", error1);
                        Toast.makeText(this, error1.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (value1 != null && value1.exists()) {
                        Log.d(TAG, "onCreate: ok");
                        author = value1.toObject(User.class);
                        assert author != null;

                        invalidateOptionsMenu();

                        binding.postShare.setOnClickListener(v -> {
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.share_template), post.getPostTitle(), post.getPostLanguage(), post.getPostText(), author.getName()));
                            sendIntent.setType("text/plain");
                            startActivity(Intent.createChooser(sendIntent, "Share via"));
                        });
                    } else {
                        Toast.makeText(this, "Can't find post", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

                if (post.getRatings().containsKey(ME.getUserID())) {
                    binding.postRating.setImageResource(RATING_STARS.get(post.getRatings().get(ME.getUserID())));
                    binding.postRating.setColorFilter(getColor(R.color.deep_sky_blue));
                    binding.ratingInput.setRating(post.getRatings().get(ME.getUserID()));
                } else {
                    binding.postRating.setImageResource(R.drawable.ic_star);
                    binding.postRating.setColorFilter(getColor(R.color.black));
                    binding.ratingInput.setRating(0);
                }
                float rating = (float) post.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                binding.postTotalRating.setRating(rating);
                binding.postRatingText.setText(String.format(getString(R.string.rated_by), rating, post.getRatings().size()));
                binding.fullPostToolbar.setTitle(post.getPostTitle());
                binding.fullPostToolbar.setSubtitle(post.getPostLanguage());
                binding.postText.setText(post.getPostText());
                binding.postText.post(() -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= binding.postText.getLineCount(); i++) {
                        sb.append(i);
                        if (i != binding.postText.getLineCount()) sb.append("\n");
                    }
                    binding.postLines.setText(sb.toString());
                });
                binding.postRating.setOnLongClickListener(v -> {
                    showRatingDialog();
                    return true;
                });
                binding.postRating.setOnClickListener(v -> {
                    if (post.getRatings().containsKey(ME.getUserID())) {
                        post.getRatings().remove(ME.getUserID());
                        db.collection("Posts").document(post.getPostID()).update("ratings", post.getRatings());
                    } else {
                        db.collection("Posts").document(post.getPostID()).update("ratings", post.getRatings()).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                new FirebaseMessagingSender(this, post.getPostAuthor(), String.format(getString(R.string.post_rate_template), ME.getName(), 5, post.getPostTitle(), post.getPostLanguage()), FirebaseMessagingSender.POST_RATING_UPDATE, post.getPostID()).sendMessage();
                            }
                        });
                        post.getRatings().put(ME.getUserID(), 5);
                    }
                });
                binding.ratingSubmit.setOnClickListener(v -> {
                    if (binding.ratingInput.getRating() > 0) {
                        post.getRatings().put(ME.getUserID(), (int) binding.ratingInput.getRating());
                        db.collection("Posts").document(post.getPostID()).update("ratings", post.getRatings()).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                new FirebaseMessagingSender(this, post.getPostAuthor(), String.format(getString(R.string.post_rate_template), ME.getName(), (int) binding.ratingInput.getRating(), post.getPostTitle(), post.getPostLanguage()), FirebaseMessagingSender.POST_RATING_UPDATE, post.getPostID()).sendMessage();
                            }
                        });
                        binding.ratingInputContainer.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Enter rating first.", Toast.LENGTH_SHORT).show();
                    }
                });
                binding.postComment.setOnClickListener(v -> startActivity(new Intent(this, CommentActivity.class).putExtra("POST_ID", post.getPostID())));
            } else {
                Toast.makeText(this, "Can't find post", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.full_screen_post_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (author != null) {
            menu.findItem(R.id.edit).setVisible(author.getUserID().equals(FirebaseAuth.getInstance().getUid()));
            menu.findItem(R.id.delete).setVisible(author.getUserID().equals(FirebaseAuth.getInstance().getUid()));
            menu.findItem(R.id.save).setVisible(!author.getUserID().equals(FirebaseAuth.getInstance().getUid()));

            if (!author.getUserID().equals(FirebaseAuth.getInstance().getUid())) {
                if (ME.getSavedPosts().contains(post.getPostID())) {
                    menu.findItem(R.id.save).setIcon(R.drawable.ic_save_yes);
                } else {
                    menu.findItem(R.id.save).setIcon(R.drawable.ic_save_no);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.download) {
            if (post != null && author != null) {
                checkPermissionAndSaveFile();
            } else {
                Toast.makeText(this, "Action can't be done at this moment! Please try after some time.", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", binding.postText.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.edit) {
            if (post != null) {
                startActivity(new Intent(this, CreatePostActivity.class).putExtra("EDITABLE_POST", Parcels.wrap(post)));
            } else {
                Toast.makeText(this, "Action can't be done at this moment! Please try after some time.", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.save) {
            if (ME.getSavedPosts().contains(post.getPostID())) {
                ME.getSavedPosts().remove(post.getPostID());
                Toast.makeText(this, "Removing from saved posts.", Toast.LENGTH_SHORT).show();
            } else {
                ME.getSavedPosts().add(post.getPostID());
                Toast.makeText(this, "Adding to saved posts.", Toast.LENGTH_SHORT).show();
            }
            db.collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).update("savedPosts", ME.getSavedPosts());
            invalidateOptionsMenu();
        } else if (itemId == R.id.delete) {
            if (post != null && author != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Delete this post?")
                        .setMessage("This is an irreversible process. Your post will be deleted immediately from our server as well as likes and comments of this post. And you will never get it back once you deleted. So be sure before deleting.")
                        .setPositiveButton("Delete", (dialog12, which) -> {
                            dialog12.dismiss();
                            db.collection("Posts").document(post.getPostID()).get().addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    db.collection("Users").document(post.getPostAuthor()).update("postCount", author.getPostCount() - 1);
                                    db.collection("Posts").document(post.getPostID()).delete();
                                }
                            });
                        }).setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss());

                AlertDialog deleteAlert = builder.create();
                deleteAlert.setOnShowListener(dialog13 -> {
                    deleteAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
                    deleteAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.gray));
                });
                deleteAlert.show();
            } else {
                Toast.makeText(this, "Action can't be done at this moment! Please try after some time.", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
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

    private void checkPermissionAndSaveFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                saveToFile();
            } else {
                showSettingsDialog();
            }
        } else {
            Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                    saveToFile();
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).onSameThread().check();
        }
    }

    private void saveToFile() {
        File filePath = new File(String.format(Locale.getDefault(), "%s/%s/Files", Environment.getExternalStorageDirectory(), getString(R.string.app_name)));
        String fileName = String.format(Locale.getDefault(), "%s_%s_%s%s", post.getPostTitle(), post.getPostLanguage(), author.getName(), LANGUAGE_TO_EXTENSION_MAP.get(post.getPostLanguage()));
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

    private void showRatingDialog() {
        PostRatingDialogBinding dialogBinding = PostRatingDialogBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (post.getRatings().containsKey(ME.getUserID())) {
            dialogBinding.ratingInput.setRating(post.getRatings().get(ME.getUserID()));
        }

        dialogBinding.ratingSubmit.setOnClickListener(v -> {
            if (dialogBinding.ratingInput.getRating() > 0) {
                post.getRatings().put(ME.getUserID(), (int) dialogBinding.ratingInput.getRating());
                FirebaseFirestore.getInstance().collection("Posts").document(post.getPostID()).update("ratings", post.getRatings()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        new FirebaseMessagingSender(this, post.getPostAuthor(), String.format(getString(R.string.post_rate_template), ME.getName(), (int) dialogBinding.ratingInput.getRating(), post.getPostTitle(), post.getPostLanguage()), FirebaseMessagingSender.POST_RATING_UPDATE, post.getPostID()).sendMessage();
                    }
                });
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please set rating first!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

}