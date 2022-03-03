package com.prasunpersonal.jiscedebuggers.Fragments;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import com.prasunpersonal.jiscedebuggers.Activities.FAQActivity;
import com.prasunpersonal.jiscedebuggers.Activities.MainActivity;
import com.prasunpersonal.jiscedebuggers.Activities.ReportActivity;
import com.prasunpersonal.jiscedebuggers.Activities.SavedPostsActivity;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.FragmentOptionsBinding;

public class OptionsFragment extends Fragment {
    private static final String TAG = OptionsFragment.class.getSimpleName();
    FragmentOptionsBinding binding;
    FirebaseFirestore db;

    public OptionsFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOptionsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        binding.savedPosts.setOnClickListener(v1 -> startActivity(new Intent(requireContext(), SavedPostsActivity.class)));
        binding.postQuestion.setOnClickListener(v -> startActivity(new Intent(requireContext(), FAQActivity.class)));

        binding.linkSocial.setOnClickListener(v1 -> {
            if (binding.socialLinkGroup.getVisibility() == View.VISIBLE) {
                binding.socialLinkGroup.setVisibility(View.GONE);
            } else {
                binding.socialLinkGroup.setVisibility(View.VISIBLE);
            }
        });
        binding.gLink.setOnClickListener(v1 -> Toast.makeText(getContext(), "This feature will be available soon.", Toast.LENGTH_SHORT).show());
        binding.fbLink.setOnClickListener(v1 -> Toast.makeText(getContext(), "This feature will be available soon.", Toast.LENGTH_SHORT).show());
        binding.gitLink.setOnClickListener(v1 -> Toast.makeText(getContext(), "This feature will be available soon.", Toast.LENGTH_SHORT).show());

        binding.settings.setOnClickListener(v1 -> {
            if (binding.settingsGroup.getVisibility() == View.VISIBLE) {
                binding.settingsGroup.setVisibility(View.GONE);
            } else {
                binding.settingsGroup.setVisibility(View.VISIBLE);
            }
        });

        binding.report.setOnClickListener(v -> startActivity(new Intent(requireContext(), ReportActivity.class)));

        binding.logout.setOnClickListener(v1 -> {
            FirebaseAuth.getInstance().signOut();
            FirebaseMessaging.getInstance().unsubscribeFromTopic(ME.getUserID());
            FirebaseMessaging.getInstance().unsubscribeFromTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
            startActivity(new Intent(getActivity(), MainActivity.class));
            requireActivity().finish();
        });
        return binding.getRoot();
    }
}