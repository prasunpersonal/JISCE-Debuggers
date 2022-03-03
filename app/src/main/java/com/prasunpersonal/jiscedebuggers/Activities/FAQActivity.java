package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prasunpersonal.jiscedebuggers.Adapters.FAQAdapter;
import com.prasunpersonal.jiscedebuggers.Adapters.PostAdapter;
import com.prasunpersonal.jiscedebuggers.Models.FAQ;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityFaqactivityBinding;
import com.prasunpersonal.jiscedebuggers.databinding.PostQuestionsLayoutBinding;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class FAQActivity extends AppCompatActivity {
    private static final String TAG = FAQActivity.class.getSimpleName();
    ActivityFaqactivityBinding binding;
    ArrayList<FAQ> faqs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaqactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.faqToolbar);

        faqs = new ArrayList<>();

        binding.faqToolbar.setNavigationOnClickListener(v -> finish());
        binding.allFAQs.setLayoutManager(new LinearLayoutManager(this));

        updateUi();

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
                if (binding.allFAQs.getAdapter() != null) {
                    ((FAQAdapter) binding.allFAQs.getAdapter()).getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.searchClear.setOnClickListener(v -> binding.searchText.setText(null));

        binding.faqsRefresh.setOnRefreshListener(this::updateUi);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post_question, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.post) {
            showDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUi() {
        binding.faqsRefresh.setRefreshing(true);
        faqs.clear();
        FirebaseFirestore.getInstance().collection("FAQs").orderBy("questionTime", Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    faqs.add(doc.toObject(FAQ.class));
                }
                binding.allFAQs.setAdapter(new FAQAdapter(faqs, new FAQAdapter.setOnEventListeners() {
                    @Override
                    public void OnItemClicked(FAQ faq) {
                        startActivity(new Intent(FAQActivity.this, QuestionAnswerActivity.class).putExtra("FAQ_ID", faq.getQuestionId()));
                    }

                    @Override
                    public void OnReceivedSearchCount(int size) {
                        binding.srcCount.setText(String.valueOf(size));
                    }
                }));
                binding.faqsRefresh.setRefreshing(false);
            } else {
                Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDialog() {
        PostQuestionsLayoutBinding pqlBinding = PostQuestionsLayoutBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.SheetDialog);
        dialog.setContentView(pqlBinding.getRoot());
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        try {
            Glide.with(this).load(ME.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(pqlBinding.authorDp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pqlBinding.authorName.setText(ME.getName());
        pqlBinding.authorEmail.setText(ME.getEmail());

        pqlBinding.requestPost.setOnClickListener(v -> {
            if (pqlBinding.questionTitle.getText().toString().trim().isEmpty()){
                Toast.makeText(this, "Enter your question first.", Toast.LENGTH_SHORT).show();
                return;
            }

            FAQ faq = new FAQ(pqlBinding.questionTitle.getText().toString().trim());
            FirebaseFirestore.getInstance().collection("FAQs").document(faq.getQuestionId()).set(faq).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (binding.allFAQs.getAdapter() != null) {
                        faqs.add(0, faq);
                        binding.allFAQs.getAdapter().notifyItemInserted(0);
                        new FirebaseMessagingSender(this, FirebaseMessagingSender.DEFAULT_TOPICS, String.format(Locale.getDefault(), getString(R.string.question_post), ME.getName(), faq.getQuestionTitle()), FirebaseMessagingSender.POST_QUESTION, faq.getQuestionId()).sendMessage();
                    }
                }
            });
            dialog.dismiss();
        });

        dialog.show();
    }
}