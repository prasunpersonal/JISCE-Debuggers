package com.prasunpersonal.jiscedebuggers.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prasunpersonal.jiscedebuggers.Models.FAQ;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.FaqLayoutBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.MyFAQViewHolder> implements Filterable {
    private static final String TAG = FAQAdapter.class.getSimpleName();
    private Context context;
    private final ArrayList<FAQ> faqs;
    private final ArrayList<FAQ> allFaqs;
    private final setOnEventListeners listeners;

    public FAQAdapter(ArrayList<FAQ> faqs, setOnEventListeners listeners) {
        this.faqs = faqs;
        this.allFaqs = new ArrayList<>(faqs);
        this.listeners = listeners;
    }

    @NonNull
    @Override
    public MyFAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyFAQViewHolder(FaqLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyFAQViewHolder holder, int position) {
        FirebaseFirestore.getInstance().collection("Users").document(faqs.get(position).getQuestionAuthor()).addSnapshotListener((value, error) -> {
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
            }
        });

        holder.binding.questionDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(faqs.get(position).getQuestionTime())));
        holder.binding.questionTitle.setText(faqs.get(position).getQuestionTitle());
        holder.binding.answerCount.setText(String.format(Locale.getDefault(), "Available answers: %d", faqs.get(position).getAnswers().size()));
        holder.itemView.setOnClickListener(v -> listeners.OnItemClicked(faqs.get(position)));
    }

    @Override
    public int getItemCount() {
        return faqs.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<FAQ> filteredFAQs = new ArrayList<>();
                if (constraint == null || constraint.toString().trim().isEmpty()) {
                    filteredFAQs.addAll(allFaqs);
                } else {
                    filteredFAQs.addAll(allFaqs.stream().filter(post -> post.getQuestionTitle().toLowerCase(Locale.ROOT).contains(constraint.toString().trim().toLowerCase(Locale.ROOT))).collect(Collectors.toList()));
                }
                filteredFAQs.sort((o1, o2) -> {
                    int r1 = o1.getAnswers().size();
                    int r2 = o2.getAnswers().size();
                    return r2-r1;
                });
                FilterResults results = new FilterResults();
                results.values = filteredFAQs;
                return results;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                faqs.clear();
                faqs.addAll((Collection<? extends FAQ>) results.values);
                listeners.OnReceivedSearchCount(faqs.size());
                notifyDataSetChanged();
            }
        };
    }

    public interface setOnEventListeners{
        void OnItemClicked(FAQ faq);
        void OnReceivedSearchCount(int size);
    }

    public static class MyFAQViewHolder extends RecyclerView.ViewHolder {
        FaqLayoutBinding binding;

        public MyFAQViewHolder(@NonNull FaqLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
