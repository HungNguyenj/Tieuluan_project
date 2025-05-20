package com.nlu.convertapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nlu.convertapp.R;

public class PhoneDialerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CALL_PHONE_PERMISSION = 100;

    private EditText phoneNumberEditText;
    private ImageButton callButton;
    private ImageButton backspaceButton;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_dialer);

        initializeViews();
        setupButtonListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        callButton = findViewById(R.id.callButton);
        backspaceButton = findViewById(R.id.backspaceButton);

        // Setup number buttons
        int[] buttonIds = {
                R.id.button0, R.id.button1, R.id.button2, R.id.button3,
                R.id.button4, R.id.button5, R.id.button6, R.id.button7,
                R.id.button8, R.id.button9, R.id.buttonStar, R.id.buttonHash
        };

        for (int id : buttonIds) {
            Button button = findViewById(id);
            button.setOnClickListener(this);
        }
    }

    private void setupButtonListeners() {
        callButton.setOnClickListener(v -> {
            String phoneNumber = phoneNumberEditText.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                if (checkCallPermission()) {
                    // Instead of making a direct call, start our custom call activity
                    Intent intent = new Intent(PhoneDialerActivity.this, PhoneCallActivity.class);
                    intent.putExtra("PHONE_NUMBER", phoneNumber);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            }
        });

        backspaceButton.setOnClickListener(v -> {
            String currentText = phoneNumberEditText.getText().toString();
            if (!currentText.isEmpty()) {
                phoneNumberEditText.setText(currentText.substring(0, currentText.length() - 1));
            }
        });

        backspaceButton.setOnLongClickListener(v -> {
            phoneNumberEditText.setText("");
            return true;
        });

        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Enable/disable call button based on if there's text
                callButton.setEnabled(!s.toString().isEmpty());
            }
        });
    }

    @Override
    public void onClick(View v) {
        // Handle number pad button clicks
        if (v instanceof Button) {
            Button button = (Button) v;
            String digit = button.getText().toString();
            phoneNumberEditText.append(digit);
        }
    }

    private boolean checkCallPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied - Cannot make calls", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 