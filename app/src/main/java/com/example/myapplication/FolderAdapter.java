package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private Context context;
    private List<Folder> folders;
    private List<Boolean> expandedStates;
    private static final int GRID_SPAN_COUNT = 2;

    public FolderAdapter(Context context, List<Folder> folders) {
        this.context = context;
        this.folders = folders != null ? folders : new ArrayList<>();
        this.expandedStates = new ArrayList<>();
        initializeExpandedStates();
    }

    public void updateFolders(List<Folder> newFolders) {
        this.folders = newFolders != null ? newFolders : new ArrayList<>();
        initializeExpandedStates();
        notifyDataSetChanged();
    }

    private void initializeExpandedStates() {
        expandedStates.clear();
        for (int i = 0; i < folders.size(); i++) {
            expandedStates.add(false); // Initialize all folders as collapsed
        }
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        if (folders.isEmpty()) {
            return; // Prevent binding if folders is empty
        }
        Folder folder = folders.get(position);
        holder.folderNameTextView.setText(folder.getFoldername());
        boolean isExpanded = position < expandedStates.size() ? expandedStates.get(position) : false;
        holder.imageRecyclerView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.dropdownIcon.setRotation(isExpanded ? 180f : 0f);

        // Pass OnItemClickListener to ImageAdapter constructor
        ImageAdapter imageAdapter = new ImageAdapter(context, folder.getImages(), imageData -> {
            Intent intent = new Intent(context, ImageDetailsActivity.class);
            intent.putExtra("image_url", imageData.getImageUrl());
            intent.putExtra("filename", imageData.getFilename());
            intent.putExtra("folder_name", imageData.getFoldername());
            context.startActivity(intent);
        });
        holder.imageRecyclerView.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_COUNT));
        holder.imageRecyclerView.setAdapter(imageAdapter);

        holder.itemView.setOnClickListener(v -> {
            expandedStates.set(position, !isExpanded);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return folders == null ? 0 : folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderNameTextView;
        RecyclerView imageRecyclerView;
        ImageView dropdownIcon;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folderNameTextView);
            imageRecyclerView = itemView.findViewById(R.id.imageRecyclerView);
            dropdownIcon = itemView.findViewById(R.id.dropdownIcon);
        }
    }
}