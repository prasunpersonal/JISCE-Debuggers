package com.prasunpersonal.jiscedebuggers.Adapters;

import static com.prasunpersonal.jiscedebuggers.App.PROGRAMING_LANGUAGES;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.FilterItemLayoutBinding;

public class PostFilterAdapter extends RecyclerView.Adapter<PostFilterAdapter.PostFilterViewHolder> {
    Context context;
    ArrayList<String> filters;
    setOnSelectFilterListener listener;

    public PostFilterAdapter(ArrayList<String> filters, setOnSelectFilterListener listener) {
        this.filters = filters;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostFilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        return new PostFilterViewHolder(FilterItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PostFilterViewHolder holder, int position) {
        holder.binding.filterItemText.setText(PROGRAMING_LANGUAGES.get(position));
        if (filters.contains(PROGRAMING_LANGUAGES.get(position))) {
            holder.binding.filterItem.setStrokeColor(context.getColor(R.color.deep_sky_blue));
        } else {
            holder.binding.filterItem.setStrokeColor(context.getColor(R.color.gray));
        }
        holder.binding.filterItemText.setOnClickListener(v -> {
            if (filters.contains(PROGRAMING_LANGUAGES.get(position))) {
                filters.remove(PROGRAMING_LANGUAGES.get(position));
            } else {
                filters.add(PROGRAMING_LANGUAGES.get(position));
            }
            listener.OnChangeFilterListener();
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return PROGRAMING_LANGUAGES.size();
    }

    public interface setOnSelectFilterListener{
        void OnChangeFilterListener();
    }

    public static class PostFilterViewHolder extends RecyclerView.ViewHolder {
        FilterItemLayoutBinding binding;
        public PostFilterViewHolder(@NonNull FilterItemLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
