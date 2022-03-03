package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Adapters.CommentAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Comment;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityCommentBinding;

public class CommentActivity extends AppCompatActivity {
    private static final String TAG = CommentActivity.class.getSimpleName();
    ActivityCommentBinding binding;
    FirebaseFirestore db;
    InputMethodManager imm;
    Post post;
    String postId;
    private Comment REPLY_COMMENT, EDIT_COMMENT;
    ArrayList<Comment> allComments;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.commentToolbar);

        postId = getIntent().getStringExtra("POST_ID");
        db = FirebaseFirestore.getInstance();
        allComments = new ArrayList<>();

        binding.commentToolbar.setNavigationOnClickListener(v -> finish());

        db.collection("Posts").document(postId).addSnapshotListener((value, error) -> {
            if (error != null){
                Log.d(TAG, "onCreate: ", error);
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (value != null && value.exists()) {
                post = value.toObject(Post.class);
                assert post != null;
                binding.commentToolbar.setSubtitle(post.getPostTitle());
                binding.comments.setLayoutManager(new LinearLayoutManager(this));
                binding.comments.setAdapter(new CommentAdapter(allComments, post, null, new CommentAdapter.setCallBacks() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void OnClickReply(Comment comment) {
                        REPLY_COMMENT = comment;
                        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT);
                        binding.actionType.setText("Replying to");
                        binding.replyText.setText(REPLY_COMMENT.getCommentText());
                        binding.replyMessage.setVisibility(View.VISIBLE);
                        binding.commentInput.requestFocus();
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void OnClickEdit(Comment comment) {
                        EDIT_COMMENT = comment;
                        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT);
                        binding.actionType.setText("Editing comment");
                        binding.replyText.setText(EDIT_COMMENT.getCommentText());
                        binding.commentInput.setText(EDIT_COMMENT.getCommentText());
                        binding.replyMessage.setVisibility(View.VISIBLE);
                        binding.commentInput.requestFocus();
                    }

                    @Override
                    public void OnClickAuthor(String authorId) {
                        startActivity(new Intent(CommentActivity.this, ProfileOthersActivity.class).putExtra("USER_ID", authorId));
                    }
                }));
                binding.commentPostBtn.setOnClickListener(v1 -> {
                    if (binding.commentInput.getText().toString().trim().length() == 0) {
                        Toast.makeText(this, "You can't post an empty comment", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(binding.commentInput.getWindowToken(), 0);
                    binding.commentInput.clearFocus();

                    if (EDIT_COMMENT != null) {
                        EDIT_COMMENT.setCommentText(binding.commentInput.getText().toString().trim());
                        publishComment(EDIT_COMMENT, null);
                    } else if (REPLY_COMMENT != null) {
                        Comment newComment = new Comment(FirebaseAuth.getInstance().getUid(), binding.commentInput.getText().toString().trim(), REPLY_COMMENT.getCommentID());
                        publishComment(newComment, REPLY_COMMENT);
                    } else {
                        Comment newComment = new Comment(FirebaseAuth.getInstance().getUid(), binding.commentInput.getText().toString().trim(), post.getPostID());
                        publishComment(newComment, null);
                    }

                    binding.replyMessage.setVisibility(View.GONE);
                    binding.commentInput.getText().clear();
                    binding.commentInput.clearFocus();
                    if (binding.comments.getAdapter() != null) {
                        binding.comments.getAdapter().notifyDataSetChanged();
                    }
                });
                binding.cancelReply.setOnClickListener(v1 -> {
                    REPLY_COMMENT = null;
                    EDIT_COMMENT = null;
                    binding.replyMessage.setVisibility(View.GONE);
                });
                db.collection("Comments").whereEqualTo("parentId", post.getPostID()).orderBy("commentTime").addSnapshotListener((value1, error1) -> {
                    if (error1 != null) {
                        Log.d(TAG, "onBindViewHolder: ", error1);
                        Toast.makeText(this, error1.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (value1 != null) {
                        for (DocumentChange dc : value1.getDocumentChanges()) {
                            DocumentChange.Type type = dc.getType();
                            Comment doc = dc.getDocument().toObject(Comment.class);
                            if (type == DocumentChange.Type.ADDED) {
                                allComments.add(doc);
                                if (binding.comments.getAdapter() != null) {
                                    binding.comments.getAdapter().notifyItemInserted(allComments.size()-1);
                                }
                            } else if (type == DocumentChange.Type.REMOVED) {
                                int index = allComments.indexOf(doc);
                                allComments.remove(index);
                                if (binding.comments.getAdapter() != null) {
                                    binding.comments.getAdapter().notifyItemRemoved(index);
                                }
                            } else if (type == DocumentChange.Type.MODIFIED) {
                                int index = allComments.indexOf(doc);
                                allComments.set(index, doc);
                                if (binding.comments.getAdapter() != null) {
                                    binding.comments.getAdapter().notifyItemChanged(index);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Can't find post", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void publishComment(Comment comment, final Comment replyComment) {
        db.collection("Comments").document(comment.getCommentID()).set(comment).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (replyComment != null) {
                    new FirebaseMessagingSender(this, replyComment.getCommentAuthor(), String.format(Locale.getDefault(), getString(R.string.comment_reply_template), ME.getName()), FirebaseMessagingSender.REPLY_COMMENT_UPDATE, post.getPostID()).sendMessage();
                    if (!replyComment.getCommentAuthor().equals(post.getPostAuthor())) {
                        new FirebaseMessagingSender(this, post.getPostAuthor(), String.format(Locale.getDefault(), getString(R.string.comment_reply_author_template), ME.getName()), FirebaseMessagingSender.REPLY_COMMENT_USER_UPDATE, post.getPostID()).sendMessage();
                    }
                } else {
                    new FirebaseMessagingSender(this, post.getPostAuthor(), String.format(Locale.getDefault(), getString(R.string.comment_default_template), ME.getName()), FirebaseMessagingSender.DEFAULT_COMMENT_UPDATE, post.getPostID()).sendMessage();
                }
            } else {
                Log.d(TAG, "onCreate: ", task.getException());
                Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}