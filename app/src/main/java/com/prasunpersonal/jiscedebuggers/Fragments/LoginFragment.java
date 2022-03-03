package com.prasunpersonal.jiscedebuggers.Fragments;

import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.closeKeyboard;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Activities.HomeActivity;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {
    private static final String TAG = LoginFragment.class.getSimpleName();
    FragmentLoginBinding binding;
    FirebaseAuth auth;

    public LoginFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();

        binding.loginBtn.setOnClickListener(v -> {
            if (binding.lEmail.getText().toString().trim().length() == 0) {
                binding.lEmail.setError("Email can't be empty.");
                return;
            }
            if (binding.lPass.getText().toString().trim().length() < 6) {
                binding.lPass.setError("Password must have at least 6 characters.");
                return;
            }

            closeKeyboard(requireActivity());
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Logging in");
            progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            progressDialog.setCancelable(false);
            progressDialog.show();

            auth.signInWithEmailAndPassword(binding.lEmail.getText().toString(), binding.lPass.getText().toString()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseFirestore.getInstance().collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            if (task1.getResult().exists()) {
                                progressDialog.dismiss();
                                ME = task1.getResult().toObject(User.class);
                                FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                                FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                                startActivity(new Intent(requireContext(), HomeActivity.class));
                                requireActivity().finish();
                            } else {
                                Objects.requireNonNull(auth.getCurrentUser()).delete().addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        progressDialog.dismiss();
                                        Toast.makeText(requireContext(), "No user found, please signup.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            progressDialog.dismiss();
                            Log.d(TAG, "onCreate: ", task.getException());
                            Toast.makeText(requireContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    Log.d(TAG, "onCreate: ", task.getException());
                    Toast.makeText(requireContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        return binding.getRoot();
    }
}