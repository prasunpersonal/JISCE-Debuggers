package com.prasunpersonal.jiscedebuggers.Adapters;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.prasunpersonal.jiscedebuggers.Models.Notification;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.NotificationLayoutBinding;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyPostViewHolder> {
    private static final String TAG = NotificationAdapter.class.getSimpleName();
    private Context context;
    private final ArrayList<Notification> notifications;
    private final setOnEventListeners listener;
    FirebaseFirestore db;

    public NotificationAdapter(ArrayList<Notification> notifications, setOnEventListeners listeners) {
        this.notifications = notifications;
        this.listener = listeners;
    }

    @NonNull
    @Override
    public MyPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyPostViewHolder(NotificationLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyPostViewHolder holder, int position) {
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up));
        Notification notification = notifications.get(position);
        db = FirebaseFirestore.getInstance();

        holder.binding.notiTtext.setText(notification.getBody());

        holder.binding.notiTypeImg.setImageResource((notification.getContentType() == FirebaseMessagingSender.USER_UPDATE) ? R.drawable.ic_user :
                (notification.getContentType() == FirebaseMessagingSender.POST_UPDATE) ? R.drawable.ic_notification_post :
                        (notification.getContentType() == FirebaseMessagingSender.POST_RATING_UPDATE || notification.getContentType() == FirebaseMessagingSender.USER_RATING_UPDATE) ? R.drawable.ic_star_half :
                                (notification.getContentType() == FirebaseMessagingSender.COMMENT_LIKE_UPDATE) ?  R.drawable.ic_liked_yes :
                                        (notification.getContentType() == FirebaseMessagingSender.POST_QUESTION || notification.getContentType() == FirebaseMessagingSender.POST_ANSWER) ? R.drawable.ic_question_answer :
                                                R.drawable.ic_notification_comment);

        holder.binding.notiTime.setText(new SimpleDateFormat("MMMM dd, yyyy - hh:mm aa", Locale.getDefault()).format(new Date(notification.getTime())));

        db.collection("Users").document(notification.getSenderId()).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onViewCreated: ", error);
                return;
            }

            if (value != null && value.exists()) {
                User user = value.toObject(User.class);
                assert user != null;
                try {
                    Glide.with(context).load(user.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(holder.binding.notiAuthImg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (notification.isChecked()) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.gray_lite));
        } else {
            holder.itemView.setBackgroundColor(context.getColor(R.color.light_sky_blue));
        }

        holder.itemView.setOnClickListener(v -> listener.OnNotificationClickListeners(notification));
        holder.binding.notiOptions.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Delete this notification?")
                    .setMessage("Your notification will be deleted immediately from our server and you will never get it back. So be sure before deleting.")
                    .setPositiveButton("Delete", (dialog1, which) -> {
                        dialog1.dismiss();
                        db.collection("Users").document(ME.getUserID()).collection("Notifications").document(notification.getNotificationId()).delete();
                    }).setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss());

            AlertDialog deleteAlert = builder.create();
            deleteAlert.setOnShowListener(dialog13 -> {
                deleteAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.red));
                deleteAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.gray));
            });
            deleteAlert.show();
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public interface setOnEventListeners {
        void OnNotificationClickListeners(Notification notification);
    }

    protected static class MyPostViewHolder extends RecyclerView.ViewHolder {
        NotificationLayoutBinding binding;

        public MyPostViewHolder(@NonNull NotificationLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
