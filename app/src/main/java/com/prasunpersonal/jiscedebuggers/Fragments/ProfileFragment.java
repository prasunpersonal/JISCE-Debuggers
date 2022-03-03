package com.prasunpersonal.jiscedebuggers.Fragments;

import static android.app.Activity.RESULT_OK;
import static com.prasunpersonal.jiscedebuggers.App.LANGUAGE_TO_EXTENSION_MAP;
import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.NOTIFICATION_CHANNEL_ID;
import static com.prasunpersonal.jiscedebuggers.App.getFileSize;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

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

import com.prasunpersonal.jiscedebuggers.Activities.CommentActivity;
import com.prasunpersonal.jiscedebuggers.Activities.CreatePostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.FullPostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.ProfileOthersActivity;
import com.prasunpersonal.jiscedebuggers.Adapters.PostAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentProfileBinding;
import com.prasunpersonal.jiscedebuggers.databinding.ImgOptionsBinding;
import com.prasunpersonal.jiscedebuggers.databinding.ShowImageBinding;

public class ProfileFragment extends Fragment {
    private static final String TAG = PostsFragment.class.getSimpleName();
    ArrayList<Post> posts;
    FragmentProfileBinding binding;
    FirebaseFirestore db;

    ActivityResultLauncher<Intent> editImageResponse = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            showImg(result.getData().getData().toString(), false, true);
        }
    });
    private AlertDialog alertDialog;

    public ProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        posts = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        binding.profilePosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.profilePosts.setAdapter(new PostAdapter(posts, new PostAdapter.setOnEventListeners() {
            @Override
            public void OnPostClickListeners(Post post) {
                startActivity(new Intent(getContext(), FullPostActivity.class).putExtra("POST_ID", post.getPostID()));
            }

            @Override
            public void OnCommentClickListener(Post post) {
                startActivity(new Intent(getContext(), CommentActivity.class).putExtra("POST_ID", post.getPostID()));
            }

            @Override
            public void OnAuthorClick(User author) {
                startActivity(new Intent(getContext(), ProfileOthersActivity.class).putExtra("USER_ID", author.getUserID()));
            }

            @Override
            public void OnClickDownload(Post post) {
                checkPermissionAndSaveFile(post);
            }

            @Override
            public void OnClickEdit(Post post) {
                startActivity(new Intent(requireContext(), CreatePostActivity.class).putExtra("EDITABLE_POST", Parcels.wrap(post)));
            }

            @Override
            public void OnReceivedSearchCount(int count) {

            }
        }));
        assert binding.profilePosts.getAdapter() != null;

        FirebaseFirestore.getInstance().collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "updateUi: ", error);
                return;
            }
            if (value != null && value.exists()) {
                ME = value.toObject(User.class);
                try {
                    Glide.with(requireContext()).load(ME.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(binding.profilePic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                binding.name.setText(ME.getName());
                binding.email.setText(ME.getEmail());
                binding.phone.setText(ME.getPhone());
                binding.joined.setText(String.format(Locale.getDefault(), "Joined on %s", new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(ME.getJoined()))));
                binding.postCount.setText(String.valueOf(ME.getPostCount()));
                float rating = (float) ME.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                binding.rating.setRating(rating);
                binding.ratingCount.setText(String.format(Locale.getDefault(), "%.01f", rating));
                binding.profilePic.setOnClickListener(v -> {
                    ImgOptionsBinding ioBinding = ImgOptionsBinding.inflate(getLayoutInflater());
                    BottomSheetDialog dialog = new BottomSheetDialog(v.getContext(), R.style.SheetDialog);
                    dialog.setContentView(ioBinding.getRoot());

                    if (ME.getProfilePic() == null){
                        ioBinding.deleteImg.setVisibility(View.GONE);
                        ioBinding.viewImg.setVisibility(View.GONE);
                    }else {
                        ioBinding.deleteImg.setVisibility(View.VISIBLE);
                        ioBinding.viewImg.setVisibility(View.VISIBLE);
                    }

                    ioBinding.editImg.setOnClickListener(v1 -> {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        editImageResponse.launch(intent);
                    });

                    ioBinding.viewImg.setOnClickListener(v1 -> {
                        dialog.dismiss();
                        showImg(ME.getProfilePic(), true, false);
                    });

                    ioBinding.deleteImg.setOnClickListener(v1 -> {
                        dialog.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                                .setTitle("Delete profile photo?")
                                .setMessage("Are you sure about this process?")
                                .setPositiveButton("Delete", (dialog1, which) -> {
                                    dialog1.dismiss();
                                    db.collection("Users").document(ME.getProfilePic()).update("profilePic", null).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()){
                                            StorageReference storageRef = FirebaseStorage.getInstance().getReference(getResources().getString(R.string.app_name)).child(ME.getUserID()).child("profile_image");
                                            storageRef.delete();
                                            Toast.makeText(requireContext(), "Profile photo deleted successfully.", Toast.LENGTH_SHORT).show();
                                        }else {
                                            Toast.makeText(requireContext(), "Can't delete profile photo: " + Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss());
                        AlertDialog deleteAlert = builder.create();
                        deleteAlert.setOnShowListener(dialog13 -> deleteAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)));
                        deleteAlert.show();
                    });
                    dialog.show();
                });
            }
        });
        FirebaseFirestore.getInstance().collection("Posts").whereEqualTo("postAuthor", ME.getUserID()).orderBy("postTime").addSnapshotListener((value, error) -> {
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


        return binding.getRoot();
    }

    private void showImg(String uri, boolean cancelable, boolean editable){
        ShowImageBinding siBinding = ShowImageBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(siBinding.getRoot())
                .setCancelable(cancelable);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(ActivityCompat.getDrawable(requireContext(), R.drawable.dialog_bg));

        if (editable){
            siBinding.editableArea.setVisibility(View.VISIBLE);
        }else {
            siBinding.editableArea.setVisibility(View.GONE);
        }

        Glide.with(requireContext()).load(uri).placeholder(R.drawable.ic_user_circle).into(siBinding.showImg);
        siBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        siBinding.btnUpload.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            final NotificationCompat.Builder notification_builder = new NotificationCompat.Builder(requireContext(), NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Uploading Images")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true);
            notificationManager.notify(1, notification_builder.build());

            StorageReference storageRef = FirebaseStorage.getInstance().getReference(getResources().getString(R.string.app_name)).child(ME.getUserID()).child("profile_image");
            storageRef.putFile(Uri.parse(uri)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri1 -> db.collection("Users").document(ME.getUserID()).update("profilePic", uri1.toString()).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()){
                            Toast.makeText(requireContext(), "Profile photo updated successfully.", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(requireContext(), "Cant update profile photo: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
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
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setIcon(R.drawable.ic_folder);
            builder.setTitle("Storage Permission");
            builder.setMessage("This app needs all files access permission. You can enable it in app settings.");
            builder.setCancelable(false);
            builder.setPositiveButton("Settings", (dialog, which) -> {
                dialog.cancel();
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", requireContext().getApplicationContext().getPackageName())));
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
            alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)));
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
            Dexter.withContext(requireContext()).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
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
            Toast.makeText(requireContext(), "File saved to: " + file.getPath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    ActivityResultLauncher<Intent> permissionRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Toast.makeText(requireContext(), "Permission Granted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    });
}