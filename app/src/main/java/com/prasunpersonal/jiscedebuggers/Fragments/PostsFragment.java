package com.prasunpersonal.jiscedebuggers.Fragments;

import static android.app.Activity.RESULT_OK;
import static com.facebook.FacebookSdk.getApplicationContext;
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
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

import com.prasunpersonal.jiscedebuggers.Activities.CommentActivity;
import com.prasunpersonal.jiscedebuggers.Activities.CreatePostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.FullPostActivity;
import com.prasunpersonal.jiscedebuggers.Activities.ProfileOthersActivity;
import com.prasunpersonal.jiscedebuggers.Adapters.PostAdapter;
import com.prasunpersonal.jiscedebuggers.Adapters.PostFilterAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Post;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.FilterDialogLayoutBinding;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentPostsBinding;

public class PostsFragment extends Fragment {
    private static final String TAG = PostsFragment.class.getSimpleName();
    public static ArrayList<Post> posts;
    public static ArrayList<String> filters;
    FragmentPostsBinding binding;
    private AlertDialog alertDialog;

    public PostsFragment() {}

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostsBinding.inflate(inflater, container, false);
        posts = new ArrayList<>();
        filters = new ArrayList<>();

        binding.allPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.searchClear.setOnClickListener(v1 -> binding.searchText.setText(null));
        binding.searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    binding.searchClear.setVisibility(View.VISIBLE);
                    binding.srcCount.setVisibility(View.VISIBLE);
                } else {
                    binding.searchClear.setVisibility(View.GONE);
                    binding.srcCount.setVisibility(View.GONE);
                }
                if (binding.allPosts.getAdapter() != null) {
                    ((PostAdapter) binding.allPosts.getAdapter()).getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateUi();
        binding.postsRefresh.setOnRefreshListener(this::updateUi);

        binding.filterBtn.setOnClickListener(v -> {
            ArrayList<String> tmpFilters = new ArrayList<>(filters);
            FilterDialogLayoutBinding dialogBinding = FilterDialogLayoutBinding.inflate(getLayoutInflater());
            BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.SheetDialog);
            dialog.setContentView(dialogBinding.getRoot());

            dialogBinding.filterCount.setText(String.format(getString(R.string.filter_count), tmpFilters.size()));
            dialogBinding.allFilters.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            dialogBinding.allFilters.setAdapter(new PostFilterAdapter(tmpFilters, () -> {
                dialogBinding.filterCount.setText(String.format(getString(R.string.filter_count), tmpFilters.size()));
                dialogBinding.clrFilter.setEnabled(tmpFilters.size() > 0);
            }));

            dialogBinding.applyFilter.setOnClickListener(v1 -> {
                filters.clear();
                filters.addAll(tmpFilters);
                updateUi();
                dialog.dismiss();
            });

            dialogBinding.clrFilter.setOnClickListener(v1 -> {
                tmpFilters.clear();
                dialogBinding.filterCount.setText(String.format(getString(R.string.filter_count), tmpFilters.size()));
                Objects.requireNonNull(dialogBinding.allFilters.getAdapter()).notifyDataSetChanged();
            });

            dialog.show();
        });

        return binding.getRoot();
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

    @SuppressLint("NotifyDataSetChanged")
    private void updateUi() {
        binding.postsRefresh.setRefreshing(true);
        posts.clear();
        if (filters.isEmpty()) {
            FirebaseFirestore.getInstance().collection("Posts").whereEqualTo("access", Post.ACCESS_PUBLIC).orderBy("postTime", Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Post post = doc.toObject(Post.class);
                        if (post.getPostAuthor().equals(ME.getUserID())) {
                            continue;
                        }
                        if (binding.searchText.getText().toString().trim().isEmpty()){
                            posts.add(post);
                        }  else {
                            if (post.getPostTitle().toLowerCase(Locale.ROOT).contains(binding.searchText.getText().toString().trim().toLowerCase(Locale.ROOT))) {
                                posts.add(post);
                            }
                        }
                    }
                    binding.allPosts.setAdapter(new PostAdapter(posts, new PostAdapter.setOnEventListeners() {
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
                            binding.srcCount.setText(String.format(getString(R.string.post_src_count), count));
                        }
                    }));
                } else {
                    Toast.makeText(requireContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
                binding.postsRefresh.setRefreshing(false);
            });
        } else {
            FirebaseFirestore.getInstance().collection("Posts").whereEqualTo("access", Post.ACCESS_PUBLIC).orderBy("postTime", Query.Direction.DESCENDING).whereIn("postLanguage", filters).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Post post = doc.toObject(Post.class);
                        if (post.getPostAuthor().equals(ME.getUserID())) {
                            continue;
                        }
                        if (binding.searchText.getText().toString().trim().isEmpty()){
                            posts.add(post);
                        }  else {
                            if (post.getPostTitle().toLowerCase(Locale.ROOT).contains(binding.searchText.getText().toString().trim().toLowerCase(Locale.ROOT))) {
                                posts.add(post);
                            }
                        }
                    }
                    binding.allPosts.setAdapter(new PostAdapter(posts, new PostAdapter.setOnEventListeners() {
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
                } else {
                    Toast.makeText(requireContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
                binding.postsRefresh.setRefreshing(false);
            });
        }
    }
}