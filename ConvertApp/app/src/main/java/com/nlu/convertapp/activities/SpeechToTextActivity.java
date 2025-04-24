package com.nlu.convertapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.nlu.convertapp.R;

public class SpeechToTextActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner languageSpinner;
    private ImageButton micButton;
    private MaterialButton addFileButton;
    private MaterialButton convertButton;
    private TextView recognizedText;
    private ImageButton copyButton;
    private ImageButton starButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_text);
        
        initializeViews();
        setSupportActionBar(toolbar);
        
        toolbar.setNavigationOnClickListener(v -> finish());
        
        setupButtonListeners();
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        
        // Language spinner
        languageSpinner = findViewById(R.id.languageSpinner);
        
        // Speech input controls
        micButton = findViewById(R.id.micButton);
        
        // Buttons
        addFileButton = findViewById(R.id.addFileButton);
        convertButton = findViewById(R.id.convertButton);
        
        // Text display and action buttons
        recognizedText = findViewById(R.id.recognizedText);
        copyButton = findViewById(R.id.copyButton);
        starButton = findViewById(R.id.starButton);
    }

    private void setupButtonListeners() {
        micButton.setOnClickListener(v -> {
            // Handle speech input
        });

        addFileButton.setOnClickListener(v -> {
            // Handle file upload
        });

        convertButton.setOnClickListener(v -> {
            // Handle conversion
        });

        copyButton.setOnClickListener(v -> {
            // Handle copy text
        });

        starButton.setOnClickListener(v -> {
            // Handle favorite
        });
    }
}