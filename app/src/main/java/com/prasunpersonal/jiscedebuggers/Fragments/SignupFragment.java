package com.prasunpersonal.jiscedebuggers.Fragments;

import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.closeKeyboard;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Locale;
import java.util.Objects;

import com.prasunpersonal.jiscedebuggers.Activities.HomeActivity;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentSignupBinding;

public class SignupFragment extends Fragment {
    private static final String TAG = SignupFragment.class.getSimpleName();
    FragmentSignupBinding binding;
    FirebaseAuth auth;

    public SignupFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();

        binding.signupBtn.setOnClickListener(view1 -> {
            if (binding.sName.getText().toString().trim().isEmpty()) {
                binding.sName.setError("Name can't be empty!");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(binding.sEmail.getText().toString().trim()).matches()) {
                binding.sEmail.setError("Enter a valid email address.");
                return;
            }
            if (!Patterns.PHONE.matcher(binding.ccp.getSelectedCountryCodeWithPlus() + binding.sPhone.getText().toString()).matches()) {
                binding.sPhone.setError("Enter a valid phone no.!");
                return;
            }
            if (binding.sPass.getText().toString().length() < 6) {
                binding.sPass.setError("Password must have at least 6 characters");
                return;
            }
            if (binding.sPass2.getText().toString().contains(" ")) {
                binding.sPass2.setError("Password can't contain a whitespace");
                return;
            }
            if (!binding.sPass.getText().toString().equals(binding.sPass2.getText().toString())) {
                binding.sPass2.setError("Passwords doesn't match.");
                return;
            }

            closeKeyboard(requireActivity());
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setTitle("Creating Account");
            progressDialog.setMessage("Please wait while we create your account.");
            progressDialog.show();
            progressDialog.setCancelable(false);

            auth.createUserWithEmailAndPassword(binding.sEmail.getText().toString().trim(), binding.sPass.getText().toString().trim()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    User user = new User(binding.sName.getText().toString(), binding.sEmail.getText().toString());
                    user.setPhone(binding.ccp.getSelectedCountryCodeWithPlus() + binding.sPhone.getText().toString().trim());
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("Users").document(auth.getCurrentUser().getUid()).set(user).addOnCompleteListener(task12 -> {
                        progressDialog.dismiss();
                        if (task12.isSuccessful()) {
                            ME = user;
                            new FirebaseMessagingSender(requireContext(), FirebaseMessagingSender.DEFAULT_TOPICS, String.format(Locale.getDefault(), getString(R.string.user_notification_template), ME.getName()), FirebaseMessagingSender.USER_UPDATE, ME.getUserID()).sendMessage();
                            FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                            FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                            startActivity(new Intent(requireContext(), HomeActivity.class));
                            requireActivity().finish();
                        } else {
                            Toast.makeText(requireContext(), task12.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "onCreateView: ",task12.getException());
                        }
                    });
                } else {
                    Toast.makeText(requireContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onCreateView: ",task.getException());
                    progressDialog.dismiss();
                }
            });
        });


        return binding.getRoot();
    }
}