package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.EXTENSION_TO_LANGUAGE_MAP;
import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.PROGRAMING_LANGUAGES;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityCreatePostBinding;

import org.apache.commons.io.IOUtils;
import org.parceler.Parcels;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class CreatePostActivity extends AppCompatActivity {
    private static final String TAG = CreatePostActivity.class.getSimpleName();
    ActivityCreatePostBinding binding;
    FirebaseFirestore db;
    Post  editablePost;
    ArrayList<String> programingLanguages;

    ActivityResultLauncher<Intent> selectFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            try {
                binding.postText.setText(getCodeFromFile(result.getData().getData()));
                Cursor cursor = getContentResolver().query(result.getData().getData(), new String[]{MediaStore.Files.FileColumns.DISPLAY_NAME}, null, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    String fileName = cursor.getString(0);
                    binding.postTitle.setText(fileName.substring(0, fileName.lastIndexOf(".")).replaceAll("[-_]", " "));
                    binding.postLanguage.setSelection(Math.max(programingLanguages.indexOf(EXTENSION_TO_LANGUAGE_MAP.get(fileName.substring(fileName.lastIndexOf(".")))), 0));
                    Log.d(TAG, ": "+fileName.substring(fileName.lastIndexOf(".")));
                    cursor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.createPostToolbar);
        binding.createPostToolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        programingLanguages = new ArrayList<>(PROGRAMING_LANGUAGES);
        programingLanguages.add(0, "SELECT LANGUAGE");

        editablePost = Parcels.unwrap(getIntent().getParcelableExtra("EDITABLE_POST"));

        binding.postLanguage.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, programingLanguages));

        if (editablePost != null) {
            binding.createPostToolbar.setTitle("Edit Post");
            binding.createPostToolbar.setSubtitle(editablePost.getPostTitle());
            binding.postTitle.setText(editablePost.getPostTitle());
            binding.postLanguage.setSelection(programingLanguages.indexOf(editablePost.getPostLanguage()));
            binding.publicRadioBtn.setChecked(editablePost.getAccess() == Post.ACCESS_PUBLIC);
            binding.privateRadioBtn.setChecked(editablePost.getAccess() == Post.ACCESS_PRIVATE);
            binding.postText.setText(editablePost.getPostText());
        }

        binding.postText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.postLines.getLineCount() != binding.postText.getLineCount()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= binding.postText.getLineCount(); i++) {
                        sb.append(i);
                        if (i != binding.postText.getLineCount()) sb.append("\n");
                    }
                    binding.postLines.setText(sb.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_post, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.importFile) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/*");
            selectFile.launch(intent);
        } else if (item.getItemId() == R.id.postContainer) {
            uploadPost();
        }
        return super.onOptionsItemSelected(item);
    }

    private String getCodeFromFile(Uri uri) throws IOException {
        return IOUtils.toString(getContentResolver().openInputStream(uri), Charset.defaultCharset());
    }

    private void uploadPost() {
        if(binding.postTitle.getText().toString().trim().isEmpty()){
            binding.postTitle.setError("Post title can't be empty.");
            return;
        }
        if(binding.postLanguage.getSelectedItemPosition() == 0){
            Toast.makeText(this, "Please select a language first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(binding.postText.getText().toString().trim().isEmpty()){
            binding.postText.setError("Post text can't be empty.");
            return;
        }
        if (editablePost == null) {
            editablePost = new Post(binding.postTitle.getText().toString().trim(), binding.postLanguage.getSelectedItem().toString(), binding.postText.getText().toString().trim(), (binding.postAccessOptions.getCheckedRadioButtonId() == binding.publicRadioBtn.getId()) ? Post.ACCESS_PUBLIC : Post.ACCESS_PRIVATE);
            db.collection("Posts").document(editablePost.getPostID()).set(editablePost).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    db.collection("Users").document(ME.getUserID()).update("postCount", ME.getPostCount()+1);
                    if (editablePost.getAccess() == Post.ACCESS_PUBLIC) {
                        new FirebaseMessagingSender(this, FirebaseMessagingSender.DEFAULT_TOPICS, String.format(Locale.getDefault(), getString(R.string.post_notification_template), ME.getName(), editablePost.getPostTitle(), editablePost.getPostLanguage()), FirebaseMessagingSender.POST_UPDATE, editablePost.getPostID()).sendMessage();
                    }
                } else {
                    Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            editablePost.setPostTitle(binding.postTitle.getText().toString().trim());
            editablePost.setPostLanguage(binding.postLanguage.getSelectedItem().toString());
            editablePost.setAccess((binding.postAccessOptions.getCheckedRadioButtonId() == binding.publicRadioBtn.getId()) ? Post.ACCESS_PUBLIC : Post.ACCESS_PRIVATE);
            editablePost.setPostText(binding.postText.getText().toString().trim());
            db.collection("Posts").document(editablePost.getPostID()).set(editablePost);
        }
        finish();
    }
}