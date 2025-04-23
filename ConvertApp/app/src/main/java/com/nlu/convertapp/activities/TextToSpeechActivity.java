package com.nlu.convertapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.nlu.convertapp.R;

public class TextToSpeechActivity extends AppCompatActivity {

    Toolbar toolbar;
    private Spinner languageSpinner;
    private EditText textArea;

    // Bottom buttons
    private ImageButton fileButton;
    private ImageButton copyButton;
    private ImageButton starButton;
    private MaterialButton convertButton;

    // Audio player controls
    private ImageButton playPauseButton;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar audioSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_text_to_speech);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();

        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kết thúc activity hiện tại để quay về MainActivity
                finish();
            }
        });

        // Xử lý sự kiện cho các nút
        setupButtonListeners();

        // Xử lý sự kiện cho audio seekbar
        setupAudioSeekBar();
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Language spinner
        languageSpinner = findViewById(R.id.languageSpinner);

        // Text area
        textArea = findViewById(R.id.textArea);

        // Bottom buttons
        fileButton = findViewById(R.id.fileButton);
        copyButton = findViewById(R.id.copyButton);
        starButton = findViewById(R.id.starButton);
        convertButton = findViewById(R.id.convertButton);

        // Audio player controls
        playPauseButton = findViewById(R.id.playPauseButton);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        audioSeekBar = findViewById(R.id.audioSeekBar);
    }

    private void setupButtonListeners() {
        // File button click listener
        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện upload file
            }
        });

        // Copy button click listener
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện copy text
            }
        });

        // Star button click listener
        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện đánh dấu yêu thích
            }
        });

        // Convert button click listener
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện chuyển đổi text thành speech
            }
        });

        // Play/Pause button click listener
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện play/pause audio
            }
        });
    }

    private void setupAudioSeekBar() {
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Xử lý khi người dùng kéo seekbar
                if (fromUser) {
                    // Cập nhật vị trí phát audio
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Xử lý khi người dùng bắt đầu chạm vào seekbar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Xử lý khi người dùng thả seekbar
            }
        });
    }
}