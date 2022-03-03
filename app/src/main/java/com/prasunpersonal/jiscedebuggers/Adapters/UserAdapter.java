package com.prasunpersonal.jiscedebuggers.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.UserLayoutBinding;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyUserViewHolder> implements Filterable {
    private Context context;
    private final ArrayList<User> users;
    private final ArrayList<User> allUsers;
    private final setOnUserClickListener listener;

    public UserAdapter(ArrayList<User> users, setOnUserClickListener listener) {
        this.users = users;
        this.allUsers = new ArrayList<>(users);
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyUserViewHolder(UserLayoutBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyUserViewHolder holder, int position) {
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up));
        User user = users.get(position);
        try {
            Glide.with(context).load(user.getProfilePic()).placeholder(R.drawable.ic_user_circle).into(holder.binding.uProfilePic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.binding.uName.setText(user.getName());
        holder.binding.uName.setSelected(true);
        holder.binding.totalPosts.setText(String.format(Locale.getDefault(), "Total Posts: %d", user.getPostCount()));
        holder.binding.totalRating.setRating((float) user.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0));
        holder.itemView.setOnClickListener(v -> listener.OnUserClickListener(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<User> filteredPosts = new ArrayList<>();
                if (constraint == null || constraint.toString().trim().isEmpty()) {
                    filteredPosts.addAll(allUsers);
                } else {
                    filteredPosts.addAll(allUsers.stream().filter(user -> user.getName().toLowerCase(Locale.ROOT).contains(constraint.toString().trim().toLowerCase(Locale.ROOT))).collect(Collectors.toList()));
                }
                filteredPosts.sort((o1, o2) -> {
                    float r1 = (float) o1.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                    float r2 = (float) o2.getRatings().values().stream().mapToDouble(d -> d).average().orElse(0);
                    return (int) (r2-r1);
                });
                FilterResults results = new FilterResults();
                results.values = filteredPosts;
                return results;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                users.clear();
                users.addAll((Collection<? extends User>) results.values);
                listener.OnReceivedSearchCount(users.size());
                notifyDataSetChanged();
            }
        };
    }

    public interface setOnUserClickListener{
        void OnUserClickListener(User user);
        void OnReceivedSearchCount(int count);
    }

    public static class MyUserViewHolder extends RecyclerView.ViewHolder {
        UserLayoutBinding binding;
        public MyUserViewHolder(@NonNull UserLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
