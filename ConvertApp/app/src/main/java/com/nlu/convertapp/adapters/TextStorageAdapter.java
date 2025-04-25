// app/src/main/java/com/nlu/convertapp/adapters/TextStorageAdapter.java
package com.nlu.convertapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nlu.convertapp.R;
import com.nlu.convertapp.models.TextStorageItem;

import java.util.List;

public class TextStorageAdapter extends RecyclerView.Adapter<TextStorageAdapter.ViewHolder> {

    private List<TextStorageItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onStarClick(int position);
        void onDownloadClick(int position);
        void onItemLongClick(int position); // Add long click listener method
    }

    public TextStorageAdapter(List<TextStorageItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_storage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextStorageItem item = items.get(position);

        holder.tvDate.setText(item.getDate());
        holder.tvContent.setText(item.getContent());

        // Set star icon based on starred status
        holder.ivStar.setImageResource(item.isStarred()
                ? R.drawable.ic_baseline_star_solid
                : R.drawable.ic_baseline_star_regular);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvContent;
        ImageView ivStar;
        ImageView ivDownload;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDate = itemView.findViewById(R.id.tvDate);
            tvContent = itemView.findViewById(R.id.tvContent);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivDownload = itemView.findViewById(R.id.ivDownload);

            ivStar.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onStarClick(position);
                }
            });

            ivDownload.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDownloadClick(position);
                }
            });

            // Set long click listener on the whole item
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemLongClick(position);
                    return true; // Consume the long click
                }
                return false;
            });
        }
    }

    public void updateData(List<TextStorageItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }
}