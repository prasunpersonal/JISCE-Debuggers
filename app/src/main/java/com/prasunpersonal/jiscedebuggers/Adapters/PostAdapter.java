package com.prasunpersonal.jiscedebuggers.Adapters;

import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.RATING_STARS;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prasunpersonal.jiscedebuggers.Models.FAQ;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.PostLayoutBinding;
import com.prasunpersonal.jiscedebuggers.databinding.PostOptionsBinding;
import com.prasunpersonal.jiscedebuggers.databinding.PostRatingDialogBinding;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyPostViewHolder> implements Filterable {
    private static final String TAG = PostAdapter.class.getSimpleName();
    private Context context;
    private final ArrayList<Post> posts;
    private final ArrayList<Post> allPosts;
    private final setOnEventListeners listener;

    public PostAdapter(ArrayList<Post> posts, setOnEventListeners listeners) {
        this.posts = posts;
        this.allPosts = new ArrayList<>(posts);
        this.listener = listeners;
    }

    @NonNull
    @Override
    public MyPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyPostViewHolder(PostLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyPostViewHolder holder, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Post post = posts.get(position);

        db.collection("Users").document(post.getPostAuthor()).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onBindViewHolder: ",error);
            }
            if (value != null && value.exists()) {
                User author = value.toObject(User.class);
                assert author != null;
                holder.binding.authorName.setText(author.getName());
                try {
                    Glide.with(context).load(author.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(holder.binding.authorDp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                holder.binding.postShare.setOnClickListener(v -> {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(context.getString(R.string.share_template), post.getPostTitle(), post.getPostLanguage(), post.getPostText(), author.getName()));
                    sendIntent.setType("text/plain");
                    context.startActivity(Intent.createChooser(sendIntent,"Share via"));
                });
                holder.binding.authorName.setOnClickListener(v -> {
                    if (!post.getPostAuthor().equals(ME.getUserID())) {
                        listener.OnAuthorClick(author);
                    }
                });
                holder.binding.authorDp.setOnClickListener(v -> {
                    if (!post.getPostAuthor().equals(ME.getUserID())) {
                        listener.OnAuthorClick(author);
                    }
                });
            }
        });

        if (post.getRatings().containsKey(ME.getUserID())) {
            holder.binding.postRating.setImageResource(RATING_STARS.get(post.getRatings().get(ME.getUserID())));
            holder.binding.postRating.setColorFilter(context.getColor(R.color.deep_sky_blue));
            holder.binding.ratingInput.setRating(post.getRatings().get(ME.getUserID()));
        } else {
            holder.binding.postRating.setImageResource(R.drawable.ic_star);
            holder.binding.postRating.setColorFilter(context.getColor(R.color.black));
            holder.binding.ratingInput.setRating(0);
        }
        float rating = (float) post.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
        holder.binding.ratingInputContainer.setVisibility(View.GONE);
        holder.binding.postTotalRating.setRating(rating);
        holder.binding.postRatingText.setText(String.format(context.getString(R.string.rated_by), rating, post.getRatings().size()));
        holder.binding.postDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(post.getPostTime())));
        holder.binding.postTitle.setText(post.getPostTitle());
        holder.binding.postLanguage.setText(post.getPostLanguage());
        holder.binding.postText.setText(post.getPostText());
        holder.binding.postText.post(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= holder.binding.postText.getLineCount(); i++) {
                sb.append(i);
                if (i != holder.binding.postText.getLineCount()) sb.append("\n");
            }
            holder.binding.postLines.setText(sb.toString());
        });
        holder.binding.postText.setOnClickListener(v -> listener.OnPostClickListeners(post));
        holder.binding.postLines.setOnClickListener(v -> listener.OnPostClickListeners(post));
        holder.binding.postRating.setOnLongClickListener(v -> {
            showRatingDialog(position);
            return true;
        });
        holder.binding.postRating.setOnClickListener(v -> {
            if (post.getRatings().containsKey(ME.getUserID())) {
                post.getRatings().remove(ME.getUserID());
                db.collection("Posts").document(post.getPostID()).update("ratings", post.getRatings()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notifyItemChanged(position);
                    }
                });
            } else {
                post.getRatings().put(ME.getUserID(), 5);
                db.collection("Posts").document(post.getPostID()).update("ratings", post.getRatings()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notifyItemChanged(position);
                        new FirebaseMessagingSender(context, post.getPostAuthor(), String.format(context.getString(R.string.post_rate_template), ME.getName(), 5, post.getPostTitle(), post.getPostLanguage()), FirebaseMessagingSender.POST_RATING_UPDATE, post.getPostID()).sendMessage();
                    }
                });
            }
        });
        holder.binding.postComment.setOnClickListener(v -> listener.OnCommentClickListener(post));
        holder.binding.options.setOnClickListener(v -> {
            PostOptionsBinding optionsBinding = PostOptionsBinding.inflate(LayoutInflater.from(context));
            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.SheetDialog);
            dialog.setContentView(optionsBinding.getRoot());
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            optionsBinding.savePost.setEnabled(!post.getPostAuthor().equals(ME.getUserID()));
            optionsBinding.editPost.setEnabled(post.getPostAuthor().equals(ME.getUserID()));
            optionsBinding.deletePost.setEnabled(post.getPostAuthor().equals(ME.getUserID()));

            if (optionsBinding.savePost.isEnabled()) {
                if (ME.getSavedPosts().contains(post.getPostID())) {
                    optionsBinding.savePost.setImageResource(R.drawable.ic_save_yes);
                    optionsBinding.savePost.setColorFilter(context.getColor(R.color.deep_sky_blue));
                    optionsBinding.savePostTxt.setText("Remove from saved posts");
                } else {
                    optionsBinding.savePost.setImageResource(R.drawable.ic_save_no);
                    optionsBinding.savePost.setColorFilter(context.getColor(android.R.color.black));
                    optionsBinding.savePostTxt.setText("Add to saved posts");
                }
            }
            optionsBinding.copyPostId.setOnClickListener(v1 -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", post.getPostID());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            optionsBinding.copyPostText.setOnClickListener(v1 -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", post.getPostText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            optionsBinding.download.setOnClickListener(v1 -> {
                listener.OnClickDownload(post);
                dialog.dismiss();
            });
            optionsBinding.savePost.setOnClickListener(v1 -> {
                if (ME.getSavedPosts().contains(post.getPostID())) {
                    ME.getSavedPosts().remove(post.getPostID());
                    Toast.makeText(context, "Removing from saved posts.", Toast.LENGTH_SHORT).show();
                } else {
                    ME.getSavedPosts().add(post.getPostID());
                    Toast.makeText(context, "Adding to saved posts.", Toast.LENGTH_SHORT).show();
                }
                db.collection("Users").document(ME.getUserID()).update("savedPosts", ME.getSavedPosts()).addOnCompleteListener(task -> notifyItemChanged(position));
                dialog.dismiss();
            });
            optionsBinding.editPost.setOnClickListener(v1 -> {
                listener.OnClickEdit(post);
                dialog.dismiss();
            });
            optionsBinding.deletePost.setOnClickListener(v1 -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle("Delete this post?")
                        .setMessage("This is an irreversible process. Your post will be deleted immediately from our server as well as likes and comments of this post. And you will never get it back once you deleted. So be sure before deleting.")
                        .setPositiveButton("Delete", (dialog12, which) -> {
                            dialog12.dismiss();
                            db.collection("Posts").document(post.getPostID()).get().addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    db.collection("Posts").document(post.getPostID()).delete().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            db.collection("Users").document(post.getPostAuthor()).update("postCount", ME.getPostCount() - 1);
                                            db.collection("FAQs").whereArrayContains("answers", post.getPostID()).get().addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    for (QueryDocumentSnapshot doc : task1.getResult()) {
                                                        FAQ faq = doc.toObject(FAQ.class);
                                                        faq.getAnswers().remove(post.getPostID());
                                                        db.collection("FAQs").document(faq.getQuestionId()).update("answers", faq.getAnswers());
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }).setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss());

                AlertDialog deleteAlert = builder.create();
                deleteAlert.setOnShowListener(dialog13 -> {
                    deleteAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.red));
                    deleteAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.gray));
                });
                dialog.dismiss();
                deleteAlert.show();
            });

            dialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<Post> filteredPosts = new ArrayList<>();
                if (constraint == null || constraint.toString().trim().isEmpty()) {
                    filteredPosts.addAll(allPosts);
                } else {
                    filteredPosts.addAll(allPosts.stream().filter(post -> post.getPostTitle().toLowerCase(Locale.ROOT).contains(constraint.toString().trim().toLowerCase(Locale.ROOT))).collect(Collectors.toList()));
                }
                filteredPosts.sort((o1, o2) -> {
                    float r1 = (float) o1.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                    float r2 = (float) o2.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                    return (int) (r2-r1);
                });
                FilterResults results = new FilterResults();
                results.values = filteredPosts;
                return results;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                posts.clear();
                Log.d(TAG, "publishResults: "+results.values);
                posts.addAll((Collection<? extends Post>) results.values);
                listener.OnReceivedSearchCount(posts.size());
                notifyDataSetChanged();
            }
        };
    }

    public interface setOnEventListeners {
        void OnPostClickListeners(Post post);
        void OnCommentClickListener(Post post);
        void OnAuthorClick(User author);
        void OnClickDownload(Post post);
        void OnClickEdit(Post post);
        void OnReceivedSearchCount(int count);
    }

    protected static class MyPostViewHolder extends RecyclerView.ViewHolder {
        PostLayoutBinding binding;

        public MyPostViewHolder(@NonNull PostLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private void showRatingDialog(int position) {
        Post post = posts.get(position);

        PostRatingDialogBinding dialogBinding = PostRatingDialogBinding.inflate(LayoutInflater.from(context));
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogBinding.getRoot()).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (post.getRatings().containsKey(ME.getUserID())) {
            dialogBinding.ratingInput.setRating(post.getRatings().get(ME.getUserID()));
        }

        dialogBinding.ratingSubmit.setOnClickListener(v -> {
            if (dialogBinding.ratingInput.getRating() > 0) {
                post.getRatings().put(ME.getUserID(), (int) dialogBinding.ratingInput.getRating());
                FirebaseFirestore.getInstance().collection("Posts").document(post.getPostID()).update("ratings", post.getRatings()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notifyItemChanged(position);
                        new FirebaseMessagingSender(context, post.getPostAuthor(), String.format(context.getString(R.string.post_rate_template), ME.getName(), (int) dialogBinding.ratingInput.getRating(), post.getPostTitle(), post.getPostLanguage()), FirebaseMessagingSender.POST_RATING_UPDATE, post.getPostID()).sendMessage();
                    }
                });
                dialog.dismiss();
            } else {
                Toast.makeText(context, "Please set rating first!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
