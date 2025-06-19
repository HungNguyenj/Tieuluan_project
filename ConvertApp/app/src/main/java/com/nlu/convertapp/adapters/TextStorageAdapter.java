// app/src/main/java/com/nlu/convertapp/adapters/TextStorageAdapter.java
package com.nlu.convertapp.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nlu.convertapp.R;
import com.nlu.convertapp.models.TextStorageItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TextStorageAdapter extends RecyclerView.Adapter<TextStorageAdapter.ViewHolder> {

    private List<TextStorageItem> items;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onStarClick(int position);
        void onDownloadClick(int position);
        void onItemLongClick(int position);
    }

    public TextStorageAdapter(List<TextStorageItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_text_storage, parent, false);
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

            // Click to copy text
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TextStorageItem item = items.get(position);
                    copyToClipboard(item.getContent());
                }
            });

            ivStar.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onStarClick(position);
                }
            });

            ivDownload.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TextStorageItem item = items.get(position);
                    exportToTxtFile(item);
                }
            });

            // Set long click listener on the whole item
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemLongClick(position);
                    return true;
                }
                return false;
            });
        }

        private void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Converted Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
        }

        private void exportToTxtFile(TextStorageItem item) {
            try {
                // Get Downloads directory
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                
                // Create ConvertApp folder inside Downloads
                File convertAppDir = new File(downloadsDir, "ConvertApp");
                if (!convertAppDir.exists()) {
                    convertAppDir.mkdirs();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String timestamp = sdf.format(new Date());
                String fileName = "text_" + timestamp + ".txt";
                File file = new File(convertAppDir, fileName);

                // Write content to file
                FileWriter writer = new FileWriter(file);
                writer.write("Date: " + item.getDate() + "\n\n");
                writer.write(item.getContent());
                writer.close();

                Toast.makeText(context, 
                    "File saved to Downloads/ConvertApp/" + fileName, 
                    Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, 
                    "Error saving file: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
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