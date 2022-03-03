package com.prasunpersonal.jiscedebuggers.Adapters;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.prasunpersonal.jiscedebuggers.Models.Comment;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.CommentLayoutBinding;
import com.prasunpersonal.jiscedebuggers.databinding.CommentOptionsBinding;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.MyCommentViewHolder> {
    private static final String TAG = CommentAdapter.class.getSimpleName();
    private Context context;
    private final ArrayList<Comment> comments;
    private final Post post;
    private final Comment parent;
    private final setCallBacks callBacks;
    FirebaseFirestore db;

    public CommentAdapter(ArrayList<Comment> comments, Post post, @Nullable Comment parent, setCallBacks callBacks) {
        this.comments = comments;
        this.post = post;
        this.callBacks = callBacks;
        this.parent = parent;
    }

    @NonNull
    @Override
    public MyCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyCommentViewHolder(CommentLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyCommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        ArrayList<Comment> allComments = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        if (parent != null) {
            FirebaseFirestore.getInstance().collection("Users").document(parent.getCommentAuthor()).addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.d(TAG, "onBindViewHolder: ", error);
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                if (value != null) {
                    User author = value.toObject(User.class);
                    assert author != null;

                    holder.binding.replyAuthor.setText(author.getName());
                    holder.binding.replyText.setText(parent.getCommentText());
                    holder.binding.replyMessage.setVisibility(View.VISIBLE);
                }
            });
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.binding.subComments.getLayoutParams();
            params.leftMargin = 0;
        }
        if (comment.getLikedBy().contains(FirebaseAuth.getInstance().getUid())) {
            holder.binding.likeComment.setTextColor(ContextCompat.getColor(context, R.color.blue));
        } else {
            holder.binding.likeComment.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
        holder.binding.commentLikeCount.setText(String.valueOf(comment.getLikedBy().size()));
        holder.binding.commentDate.setText(new SimpleDateFormat("MMMM dd, yyyy - hh:mm aa", Locale.getDefault()).format(new Date(comment.getCommentTime())));
        holder.binding.comment.setText(comment.getCommentText());
        holder.binding.replyComment.setOnClickListener(v -> callBacks.OnClickReply(comment));
        holder.binding.subComments.setLayoutManager(new LinearLayoutManager(context));
        holder.binding.subComments.setAdapter(new CommentAdapter(allComments, post, comment, callBacks));
        holder.binding.likeComment.setOnClickListener(v -> {
            if (comment.getLikedBy().contains(FirebaseAuth.getInstance().getUid())) {
                comment.getLikedBy().remove(FirebaseAuth.getInstance().getUid());
            } else {
                comment.getLikedBy().add(FirebaseAuth.getInstance().getUid());
                new FirebaseMessagingSender(context, post.getPostAuthor(), String.format(context.getString(R.string.comment_like_template), ME.getName(), post.getPostTitle(), post.getPostLanguage()), FirebaseMessagingSender.COMMENT_LIKE_UPDATE, post.getPostID()).sendMessage();
            }
            db.collection("Comments").document(comment.getCommentID()).update("likedBy", comment.getLikedBy());
        });

        db.collection("Users").document(comment.getCommentAuthor()).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onBindViewHolder: ", error);
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
            if (value != null) {
                User author = value.toObject(User.class);
                assert author != null;
                try {
                    Glide.with(context).load(author.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(holder.binding.authorDp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                holder.binding.authorName.setText(author.getName());

                holder.binding.authorDp.setOnClickListener(v -> callBacks.OnClickAuthor(author.getUserID()));
                holder.binding.authorName.setOnClickListener(v -> callBacks.OnClickAuthor(author.getUserID()));

            }
        });

        db.collection("Comments").whereEqualTo("parentId", comment.getCommentID()).orderBy("commentTime").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onBindViewHolder: ", error);
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                for (DocumentChange dc : value.getDocumentChanges()) {
                    DocumentChange.Type type = dc.getType();
                    Comment doc = dc.getDocument().toObject(Comment.class);
                    if (type == DocumentChange.Type.ADDED) {
                        allComments.add(doc);
                        if (holder.binding.subComments.getAdapter() != null) {
                            holder.binding.subComments.getAdapter().notifyItemInserted(allComments.size()-1);
                        }
                    } else if (type == DocumentChange.Type.REMOVED) {
                        int index = allComments.indexOf(doc);
                        allComments.remove(index);
                        if (holder.binding.subComments.getAdapter() != null) {
                            holder.binding.subComments.getAdapter().notifyItemRemoved(index);
                        }
                    } else if (type == DocumentChange.Type.MODIFIED) {
                        int index = allComments.indexOf(doc);
                        allComments.set(index, dc.getDocument().toObject(Comment.class));
                        if (holder.binding.subComments.getAdapter() != null) {
                            holder.binding.subComments.getAdapter().notifyItemChanged(index);
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Can't find post", Toast.LENGTH_SHORT).show();
            }
        });

        holder.binding.commentOption.setOnClickListener(v -> {
            CommentOptionsBinding optionsBinding = CommentOptionsBinding.inflate(LayoutInflater.from(context));
            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.SheetDialog);
            dialog.setContentView(optionsBinding.getRoot());

            if (comment.getCommentAuthor().equals(FirebaseAuth.getInstance().getUid()) || post.getPostAuthor().equals(FirebaseAuth.getInstance().getUid())) {
                if (comment.getCommentAuthor().equals(FirebaseAuth.getInstance().getUid())) {
                    optionsBinding.editComment.setVisibility(View.VISIBLE);
                } else {
                    optionsBinding.editComment.setVisibility(View.GONE);
                }
                optionsBinding.deleteComment.setVisibility(View.VISIBLE);
            } else {
                optionsBinding.editComment.setVisibility(View.GONE);
                optionsBinding.deleteComment.setVisibility(View.GONE);
            }

            optionsBinding.editComment.setOnClickListener(v1 -> {
                callBacks.OnClickEdit(comment);
                dialog.dismiss();
            });
            optionsBinding.copyComment.setOnClickListener(v1 -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", comment.getCommentText());
                clipboard.setPrimaryClip(clip);
                dialog.dismiss();
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            });
            optionsBinding.deleteComment.setOnClickListener(v1 -> {
                dialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle("Delete this comment?")
                        .setMessage("This is an irreversible process. Your comment will be deleted immediately from this post. And you will never get it back once you deleted. So be sure before deleting.")
                        .setPositiveButton("Delete", (dialog12, which) -> {
                            db.collection("Comments").document(comment.getCommentID()).delete().addOnCompleteListener(task -> {
                                deleteSubComments(comment.getCommentID());
                            });
                            dialog12.dismiss();
                        })
                        .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss());
                AlertDialog deleteAlert = builder.create();
                deleteAlert.setOnShowListener(dialog13 -> {
                    deleteAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.red));
                    deleteAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.gray));
                });
                deleteAlert.show();
            });
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public interface setCallBacks {
        void OnClickReply(Comment comment);

        void OnClickEdit(Comment comment);

        void OnClickAuthor(String authorId);
    }

    private void deleteSubComments(String parentId) {
        db.collection("Comments").whereEqualTo("parentId", parentId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                for(QueryDocumentSnapshot doc : task.getResult()) {
                    deleteSubComments(doc.getId());
                    db.collection("Comments").document(doc.getId()).delete();
                }
            }
        });

    }

    public static class MyCommentViewHolder extends RecyclerView.ViewHolder {
        CommentLayoutBinding binding;

        public MyCommentViewHolder(@NonNull CommentLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
