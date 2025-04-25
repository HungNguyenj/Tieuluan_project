package com.nlu.convertapp.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nlu.convertapp.R;
import com.nlu.convertapp.adapters.TextStorageAdapter;
import com.nlu.convertapp.models.TextStorageItem;

import java.util.ArrayList;
import java.util.List;

public class TextStorageActivity extends AppCompatActivity implements TextStorageAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private TextStorageAdapter adapter;
    private List<TextStorageItem> textItems;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_text_storage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        textItems = generateSampleData();
        adapter = new TextStorageAdapter(textItems, this);
        recyclerView.setAdapter(adapter);

    }

    private List<TextStorageItem> generateSampleData() {
        List<TextStorageItem> items = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            items.add(new TextStorageItem("2025/01/01", "this is content to save in storage", false));
        }
        return items;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onStarClick(int position) {
        TextStorageItem item = textItems.get(position);
        item.setStarred(!item.isStarred());
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onDownloadClick(int position) {
        Toast.makeText(this, "Downloading content: " + textItems.get(position).getContent(), Toast.LENGTH_SHORT).show();
        // Implement actual download functionality here
    }

    @Override
    public void onItemLongClick(int position) {
        // Show confirmation dialog before deleting
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the item
                    adapter.removeItem(position);
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}