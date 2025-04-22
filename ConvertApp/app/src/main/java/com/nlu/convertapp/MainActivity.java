package com.nlu.convertapp;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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


        // Khai báo các nút
        Button textToSpeechButton = findViewById(R.id.textToSpeechButton);
        Button speechToTextButton = findViewById(R.id.speechToTextButton);
        Button textStorageButton = findViewById(R.id.textStorageButton);
        Button readBankTransactionButton = findViewById(R.id.readBankTransactionButton);
        Button phoneCallButton = findViewById(R.id.phoneCallButton);

        // Sự kiện onClick cho từng nút
        textToSpeechButton.setOnClickListener(v -> {
            // TODO: Tích hợp API Text to Speech tại đây
        });

        speechToTextButton.setOnClickListener(v -> {
            // TODO: Tích hợp API Speech to Text tại đây
        });

        textStorageButton.setOnClickListener(v -> {
            // TODO: Xử lý lưu trữ văn bản tại đây
        });

        readBankTransactionButton.setOnClickListener(v -> {
            // TODO: Xử lý đọc giao dịch ngân hàng tại đây
        });

        phoneCallButton.setOnClickListener(v -> {
            // TODO: Xử lý gọi điện tại đây
        });
    }
}