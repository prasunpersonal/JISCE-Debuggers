package com.prasunpersonal.jiscedebuggers.Fragments;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Activities.ProfileOthersActivity;
import com.prasunpersonal.jiscedebuggers.Adapters.UserAdapter;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentUsersBinding;

public class UsersFragment extends Fragment {
    public static ArrayList<User> users;
    FragmentUsersBinding binding;

    public UsersFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        users = new ArrayList<>();

        binding.allUsers.setLayoutManager(new GridLayoutManager(getContext(), 2));

        binding.searchClear.setOnClickListener(v1 -> binding.searchText.setText(null));
        binding.searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.searchClear.setVisibility(View.VISIBLE);
                    binding.srcCount.setVisibility(View.VISIBLE);
                } else {
                    binding.searchClear.setVisibility(View.GONE);
                    binding.srcCount.setVisibility(View.GONE);
                }
                if (binding.allUsers.getAdapter() != null) {
                    ((UserAdapter) binding.allUsers.getAdapter()).getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateUi();
        binding.usersRefresh.setOnRefreshListener(this::updateUi);
        return binding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateUi() {
        binding.usersRefresh.setRefreshing(true);
        users.clear();
        FirebaseFirestore.getInstance().collection("Users").orderBy("postCount", Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    User user = doc.toObject(User.class);
                    if (user.getUserID().equals(ME.getUserID())) {continue;}
                    users.add(user);
                }
                binding.allUsers.setAdapter(new UserAdapter(users, new UserAdapter.setOnUserClickListener() {
                    @Override
                    public void OnUserClickListener(User user) {
                        startActivity(new Intent(requireContext(), ProfileOthersActivity.class).putExtra("USER_ID", user.getUserID()));
                    }

                    @Override
                    public void OnReceivedSearchCount(int count) {
                        binding.srcCount.setText(String.format(getString(R.string.user_src_count), count));
                    }
                }));
            } else {
                Toast.makeText(requireContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
            binding.usersRefresh.setRefreshing(false);
        });
    }
}