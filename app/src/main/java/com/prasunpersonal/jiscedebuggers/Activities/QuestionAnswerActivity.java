package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.LANGUAGE_TO_EXTENSION_MAP;
import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.prasunpersonal.jiscedebuggers.Adapters.PostAdapter;
import com.prasunpersonal.jiscedebuggers.Models.FAQ;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityQuestionAnswerBinding;
import com.prasunpersonal.jiscedebuggers.databinding.PostAnswerLayoutBinding;

import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuestionAnswerActivity extends AppCompatActivity {
    private static final String TAG = QuestionAnswerActivity.class.getSimpleName();
    ActivityQuestionAnswerBinding binding;
    String faqId;
    FAQ faq;
    ArrayList<Post> answers;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionAnswerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.QAToolbar);
        faqId = getIntent().getStringExtra("FAQ_ID");
        binding.QAToolbar.setNavigationOnClickListener(v -> finish());

        updateUi();

        binding.QARefresh.setOnRefreshListener(this::updateUi);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_answers, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.addPost) {
            if (faq == null) {
                Toast.makeText(this, "Please wait until the question is loaded.", Toast.LENGTH_SHORT).show();
            } else {
                showAnsSubmitDialog();
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
        binding.QARefresh.setRefreshing(true);
        FirebaseFirestore.getInstance().collection("FAQs").document(faqId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                faq = task.getResult().toObject(FAQ.class);
                assert faq!=null;
                binding.questionTitle.setText(faq.getQuestionTitle());
                binding.questionDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(faq.getQuestionTime())));
                binding.answerCount.setText(String.format(Locale.getDefault(), "Available answers: %d", faq.getAnswers().size()));
                binding.allAnswers.setLayoutManager(new LinearLayoutManager(this));
                binding.QAToolbar.setTitle(faq.getQuestionTitle());
                answers = new ArrayList<>();
                FirebaseFirestore.getInstance().collection("Users").document(faq.getQuestionAuthor()).get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful() && task1.getResult().exists()) {
                        User author = task1.getResult().toObject(User.class);
                        assert author != null;
                        try {
                            Glide.with(this).load(author.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(binding.authorDp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        binding.authorName.setText(author.getName());
                        binding.QAToolbar.setSubtitle(author.getName());
                    } else {
                        Log.d(TAG, "updateUi: ", task1.getException());
                        Toast.makeText(this, task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
                if (faq.getAnswers().isEmpty()) {
                    Toast.makeText(this, "No answers available.", Toast.LENGTH_SHORT).show();
                    binding.allAnswers.setAdapter(new PostAdapter(answers, new PostAdapter.setOnEventListeners() {
                        @Override
                        public void OnPostClickListeners(Post post) {
                            startActivity(new Intent(QuestionAnswerActivity.this, FullPostActivity.class).putExtra("POST_ID", post.getPostID()));
                        }

                        @Override
                        public void OnCommentClickListener(Post post) {
                            startActivity(new Intent(QuestionAnswerActivity.this, CommentActivity.class).putExtra("POST_ID", post.getPostID()));
                        }

                        @Override
                        public void OnAuthorClick(User author) {
                            startActivity(new Intent(QuestionAnswerActivity.this, ProfileOthersActivity.class).putExtra("USER_ID", author.getUserID()));
                        }

                        @Override
                        public void OnClickDownload(Post post) {
                            checkPermissionAndSaveFile(post);
                        }

                        @Override
                        public void OnClickEdit(Post post) {
                            startActivity(new Intent(QuestionAnswerActivity.this, CreatePostActivity.class).putExtra("EDITABLE_POST", Parcels.wrap(post)));
                        }

                        @Override
                        public void OnReceivedSearchCount(int count) {

                        }
                    }));
                } else {
                    FirebaseFirestore.getInstance().collection("Posts").whereIn("postID", faq.getAnswers()).orderBy("ratings", Query.Direction.DESCENDING).get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            for (QueryDocumentSnapshot doc : task1.getResult()) {
                                answers.add(doc.toObject(Post.class));
                            }
                        }
                        binding.allAnswers.setAdapter(new PostAdapter(answers, new PostAdapter.setOnEventListeners() {
                            @Override
                            public void OnPostClickListeners(Post post) {
                                startActivity(new Intent(QuestionAnswerActivity.this, FullPostActivity.class).putExtra("POST_ID", post.getPostID()));
                            }

                            @Override
                            public void OnCommentClickListener(Post post) {
                                startActivity(new Intent(QuestionAnswerActivity.this, CommentActivity.class).putExtra("POST_ID", post.getPostID()));
                            }

                            @Override
                            public void OnAuthorClick(User author) {
                                startActivity(new Intent(QuestionAnswerActivity.this, ProfileOthersActivity.class).putExtra("USER_ID", author.getUserID()));
                            }

                            @Override
                            public void OnClickDownload(Post post) {
                                checkPermissionAndSaveFile(post);
                            }

                            @Override
                            public void OnClickEdit(Post post) {
                                startActivity(new Intent(QuestionAnswerActivity.this, CreatePostActivity.class).putExtra("EDITABLE_POST", Parcels.wrap(post)));
                            }

                            @Override
                            public void OnReceivedSearchCount(int count) {

                            }
                        }));
                    });
                }
            } else {
                Log.d(TAG, "updateUi: ", task.getException());
                Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
            binding.QARefresh.setRefreshing(false);
        });
    }

    private void showAnsSubmitDialog() {
        PostAnswerLayoutBinding answerLayoutBinding = PostAnswerLayoutBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.SheetDialog);
        dialog.setContentView(answerLayoutBinding.getRoot());
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        answerLayoutBinding.submitAns.setOnClickListener(v -> {
            if (answerLayoutBinding.postId.getText().toString().trim().isEmpty()) {
                answerLayoutBinding.postId.setError("Please enter post ID first.");
                return;
            }
            String postId = answerLayoutBinding.postId.getText().toString().trim();
            if (faq.getAnswers().contains(postId)) {
                Toast.makeText(this, "This answer is already linked with this post.", Toast.LENGTH_SHORT).show();
            } else {
                FirebaseFirestore.getInstance().collection("Posts").document(postId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        faq.getAnswers().add(postId);
                        FirebaseFirestore.getInstance().collection("FAQs").document(faq.getQuestionId()).update("answers", faq.getAnswers()).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                updateUi();
                                new FirebaseMessagingSender(this, faq.getQuestionAuthor(), String.format(getString(R.string.answer_post), ME.getName(), faq.getQuestionTitle()), FirebaseMessagingSender.POST_ANSWER, faq.getQuestionId()).sendMessage();
                            }
                        });
                    } else {
                        Toast.makeText(this, "The post ID you entered is invalid.", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}