package com.prasunpersonal.jiscedebuggers.Fragments;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.Activities.CommentActivity;
import com.prasunpersonal.jiscedebuggers.Activities.FullPostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.ProfileOthersActivity;
import com.prasunpersonal.jiscedebuggers.Activities.QuestionAnswerActivity;
import com.prasunpersonal.jiscedebuggers.Adapters.NotificationAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Notification;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentNotificationBinding;

public class NotificationFragment extends Fragment {
    private static final String TAG = NotificationFragment.class.getSimpleName();
    FragmentNotificationBinding binding;
    FirebaseFirestore db;
    ArrayList<Notification> notifications;

    public NotificationFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        notifications = new ArrayList<>();

        binding.allNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.allNotifications.setAdapter(new NotificationAdapter(notifications, notification -> {
            if (notification.getContentType() == FirebaseMessagingSender.USER_UPDATE) {
                startActivity(new Intent(requireContext(), ProfileOthersActivity.class).putExtra("USER_ID", notification.getContentId()));
            } else if (notification.getContentType() == FirebaseMessagingSender.POST_UPDATE || notification.getContentType() == FirebaseMessagingSender.POST_RATING_UPDATE) {
                startActivity(new Intent(requireContext(), FullPostActivity.class).putExtra("POST_ID", notification.getContentId()));
            } else if (notification.getContentType() == FirebaseMessagingSender.USER_RATING_UPDATE) {
                startActivity(new Intent(requireContext(), ProfileOthersActivity.class).putExtra("USER_ID", notification.getContentId()));
            } else if (notification.getContentType() == FirebaseMessagingSender.POST_QUESTION || notification.getContentType() == FirebaseMessagingSender.POST_ANSWER) {
                startActivity(new Intent(requireContext(), QuestionAnswerActivity.class).putExtra("FAQ_ID", notification.getContentId()));
            } else {
                startActivity(new Intent(requireContext(), CommentActivity.class).putExtra("POST_ID", notification.getContentId()));
            }
            if (!notification.isChecked()) {
                db.collection("Users").document(ME.getUserID()).collection("Notifications").document(notification.getNotificationId()).update("checked", true);
            }
        }));

        db.collection("Users").document(ME.getUserID()).collection("Notifications").orderBy("time").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onViewCreated: ", error);
                return;
            }

            if (value != null) {
                for (DocumentChange dc : value.getDocumentChanges()) {
                    DocumentChange.Type type = dc.getType();
                    if (type == DocumentChange.Type.ADDED) {
                        notifications.add(0, dc.getDocument().toObject(Notification.class));
                        if (binding.allNotifications.getAdapter() != null) {
                            binding.allNotifications.getAdapter().notifyItemInserted(0);
                        }
                    } else if (type == DocumentChange.Type.REMOVED) {
                        int index = notifications.indexOf(dc.getDocument().toObject(Notification.class));
                        notifications.remove(index);
                        if (binding.allNotifications.getAdapter() != null) {
                            binding.allNotifications.getAdapter().notifyItemRemoved(index);
                        }
                    } else if (type == DocumentChange.Type.MODIFIED) {
                        int index = notifications.indexOf(dc.getDocument().toObject(Notification.class));
                        notifications.set(index, dc.getDocument().toObject(Notification.class));
                        if (binding.allNotifications.getAdapter() != null) {
                            binding.allNotifications.getAdapter().notifyItemChanged(index);
                        }
                    }
                }

                if (notifications.isEmpty()) {
                    binding.clrAll.setVisibility(View.GONE);
                } else {
                    binding.clrAll.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.clrAll.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("Delete all notifications?")
                    .setMessage("Your notifications will be deleted immediately from our server and you will never get these back. So be sure before deleting.")
                    .setPositiveButton("Delete All", (dialog1, which) -> {
                        dialog1.dismiss();
                        db.collection("Users").document(ME.getUserID()).collection("Notifications").orderBy("time", Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                for(QueryDocumentSnapshot doc : task.getResult()) {
                                    db.collection("Users").document(ME.getUserID()).collection("Notifications").document(doc.getId()).delete();
                                }
                            }
                        });
                    }).setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss());

            AlertDialog deleteAlert = builder.create();
            deleteAlert.setOnShowListener(dialog13 -> {
                deleteAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                deleteAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            });
            deleteAlert.show();
        });

        return binding.getRoot();
    }
}