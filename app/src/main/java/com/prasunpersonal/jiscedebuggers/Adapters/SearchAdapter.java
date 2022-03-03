package com.prasunpersonal.jiscedebuggers.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.R;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.MySearchViewHolder> {
    private final ArrayList<String> suggestions;
    private final setOnSuggestionsClickListener listener;

    public SearchAdapter(ArrayList<String> suggestions, setOnSuggestionsClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MySearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_row, parent, false);
        return new MySearchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MySearchViewHolder holder, int position) {
        holder.searchSuggestions.setText(suggestions.get(position));
        holder.itemView.setOnClickListener(v1 -> listener.OnSuggestionsClickListener(suggestions.get(position)));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public interface setOnSuggestionsClickListener{
        void OnSuggestionsClickListener(String choice);
    }

    public static class MySearchViewHolder extends RecyclerView.ViewHolder {
        TextView searchSuggestions;

        public MySearchViewHolder(@NonNull View v) {
            super(v);
            searchSuggestions = v.findViewById(R.id.suggestions);
        }
    }
}
