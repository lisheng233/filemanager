package com.example.filemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    
    private File[] files;
    private OnFileClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    public interface OnFileClickListener {
        void onFileClick(File file);
    }
    
    public FileAdapter(File[] files, OnFileClickListener listener) {
        this.files = files;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = files[position];
        holder.bind(file, listener);
    }
    
    @Override
    public int getItemCount() {
        return files != null ? files.length : 0;
    }
    
    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvSize;
        TextView tvDate;
        
        FileViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvSize = itemView.findViewById(R.id.tvSize);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
        
        void bind(File file, OnFileClickListener listener) {
            tvName.setText(file.getName());
            
            if (file.isDirectory()) {
                ivIcon.setImageResource(R.drawable.ic_folder);
                tvSize.setText("文件夹");
            } else {
                ivIcon.setImageResource(R.drawable.ic_file);
                tvSize.setText(formatFileSize(file.length()));
            }
            
            tvDate.setText(formatDate(file.lastModified()));
            
            itemView.setOnClickListener(v -> listener.onFileClick(file));
        }
        
        private String formatFileSize(long size) {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.2f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", size / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
            }
        }
        
        private String formatDate(long timestamp) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
        }
    }
}