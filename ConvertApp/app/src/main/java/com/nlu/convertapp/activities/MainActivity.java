package com.nlu.convertapp.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.nlu.convertapp.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //button
        MaterialButton textToSpeechButton = findViewById(R.id.textToSpeechButton);
        MaterialButton speechToTextButton = findViewById(R.id.speechToTextButton);
        MaterialButton textStorageButton = findViewById(R.id.textStorageButton);
        MaterialButton readBankTransactionButton = findViewById(R.id.readBankTransactionButton);
        MaterialButton phoneCallRecorderButton = findViewById(R.id.voiceRecorderButton);

        textToSpeechButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TextToSpeechActivity.class);
            startActivity(intent);
        });

        speechToTextButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SpeechToTextActivity.class);
            startActivity(intent);
        });

        textStorageButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TextStorageActivity.class);
            startActivity(intent);
        });

        readBankTransactionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BankTransactionActivity.class);
            startActivity(intent);
        });

        phoneCallRecorderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PhoneCallRecorderActivity.class);
            startActivity(intent);
        });
    }
}