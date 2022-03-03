package com.prasunpersonal.jiscedebuggers.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.Models.Report;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.ImgRowBinding;
import com.prasunpersonal.jiscedebuggers.databinding.ReportLayoutBinding;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.MyCommentViewHolder> {
    private static final String TAG = ReportAdapter.class.getSimpleName();
    private Context context;
    private final ArrayList<Report> reports;

    public ReportAdapter(ArrayList<Report> reports) {
        this.reports = reports;
    }

    @NonNull
    @Override
    public MyCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        return new MyCommentViewHolder(ReportLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyCommentViewHolder holder, int position) {
        holder.binding.errorName.setText(reports.get(position).getPageName());
        holder.binding.errorDescription.setText(reports.get(position).getErrorDescription());
        holder.binding.errorImages.setLayoutManager(new GridLayoutManager(context, 4));
        holder.binding.errorImages.setAdapter(new ImgAdapter(reports.get(position).getErrorImages(), false));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public static class MyCommentViewHolder extends RecyclerView.ViewHolder {
        ReportLayoutBinding binding;

        public MyCommentViewHolder(@NonNull ReportLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
