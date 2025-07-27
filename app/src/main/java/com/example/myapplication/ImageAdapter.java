package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private Context context;
    private List<ImageData> images;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ImageData imageData);
    }

    public ImageAdapter(Context context, List<ImageData> images, OnItemClickListener listener) {
        this.context = context;
        this.images = images != null ? images : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageData imageData = images.get(position);
        Glide.with(context)
                .load(imageData.getImageUrl())
                .thumbnail(0.25f)
                .centerCrop()
                .into(holder.imageView);

        holder.filenameTextView.setText(imageData.getFilename() != null ? imageData.getFilename() : "Unnamed");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(imageData);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView filenameTextView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            filenameTextView = itemView.findViewById(R.id.filenameTextView);
        }
    }
}