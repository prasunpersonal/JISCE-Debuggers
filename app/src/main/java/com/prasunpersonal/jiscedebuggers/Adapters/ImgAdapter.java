package com.prasunpersonal.jiscedebuggers.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.ImgRowBinding;

public class ImgAdapter extends RecyclerView.Adapter<ImgAdapter.MyCommentViewHolder> {
    private static final String TAG = ImgAdapter.class.getSimpleName();
    private Context context;
    private final ArrayList<String> uris;
    boolean cancelable;

    public ImgAdapter(ArrayList<String> uris, boolean cancelable) {
        this.uris = uris;
        this.cancelable = cancelable;
    }

    @NonNull
    @Override
    public MyCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyCommentViewHolder(ImgRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyCommentViewHolder holder, int position) {
        try {
            Glide.with(context.getApplicationContext()).load(uris.get(position)).placeholder(R.drawable.ic_image).into(holder.binding.rowImg).onLoadFailed(ContextCompat.getDrawable(context, R.drawable.ic_image));
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.binding.imgRemove.setVisibility((cancelable) ? View.VISIBLE : View.GONE);
        holder.binding.imgRemove.setOnClickListener(v -> {
            uris.remove(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return uris.size();
    }

    public static class MyCommentViewHolder extends RecyclerView.ViewHolder {
        ImgRowBinding binding;

        public MyCommentViewHolder(@NonNull ImgRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
