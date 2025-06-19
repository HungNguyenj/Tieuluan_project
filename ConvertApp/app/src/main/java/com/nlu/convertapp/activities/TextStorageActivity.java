package com.nlu.convertapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nlu.convertapp.R;
import com.nlu.convertapp.adapters.TextStorageAdapter;
import com.nlu.convertapp.models.TextStorageItem;
import com.nlu.convertapp.repository.TextStorageRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TextStorageActivity extends AppCompatActivity implements TextStorageAdapter.OnItemClickListener {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private RecyclerView recyclerView;
    private TextStorageAdapter adapter;
    private List<TextStorageItem> textItems;
    private TextStorageRepository repository;
    private Toolbar toolbar;

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

        //repository
        repository = new TextStorageRepository(this);
        repository.open();
        
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        checkAndRequestPermissions();

        loadTextItems();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // For Android 12 (S) and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền lưu trữ", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Quyền lưu trữ bị từ chối. Một số tính năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadTextItems() {
        textItems = repository.getAllTexts();
        adapter = new TextStorageAdapter(textItems, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.close();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onStarClick(int position) {
        TextStorageItem item = textItems.get(position);
        String oldDate = item.getDate();
        item.setStarred(!item.isStarred());
        
        // Update in database
        repository.updateText(item, oldDate);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onDownloadClick(int position) {
        TextStorageItem item = textItems.get(position);
        Toast.makeText(this, "Tải xuống nội dung: " + item.getContent(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemLongClick(int position) {
        TextStorageItem item = textItems.get(position);
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from database
                    repository.deleteText(item.getDate());
                    // Remove from list and update UI
                    textItems.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Đã xóa item", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}