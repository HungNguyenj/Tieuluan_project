package com.nlu.convertapp.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nlu.convertapp.R;
import com.nlu.convertapp.services.CallRecordingService;

import java.util.ArrayList;
import java.util.Locale;

public class PhoneCallActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_CALL_PHONE_PERMISSION = 100;
    private static final int REQUEST_OVERLAY_PERMISSION = 300;

    // UI components
    private TextView callerNameText;
    private TextView phoneNumberText;
    private TextView callStatusText;
    private ImageButton muteButton;
    private ImageButton endCallButton;
    private ImageButton speakerButton;
    private TextView sttResultText;

    // Audio manager for speaker control
    private AudioManager audioManager;
    private boolean isSpeakerOn = false;

    // STT components
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_call);

        // Get the phone number from the intent
        phoneNumber = getIntent().getStringExtra("PHONE_NUMBER");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initializeViews();
        
        // Set the phone number
        phoneNumberText.setText(phoneNumber);
        
        // Try to get a contact name for this number (simplified)
        callerNameText.setText("Calling...");

        // Request permissions
        checkPermissions();

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }

        // Setup button listeners
        setupButtonListeners();
        
        // Start the call
        makePhoneCall(phoneNumber);
    }

    private void initializeViews() {
        callerNameText = findViewById(R.id.callerNameText);
        phoneNumberText = findViewById(R.id.phoneNumberText);
        callStatusText = findViewById(R.id.callStatusText);
        muteButton = findViewById(R.id.muteButton);
        endCallButton = findViewById(R.id.endCallButton);
        speakerButton = findViewById(R.id.speakerButton);
        sttResultText = findViewById(R.id.sttResultText);
    }

    private void setupButtonListeners() {
        muteButton.setOnClickListener(v -> {
            // Toggle mute functionality would go here
            Toast.makeText(this, "Mute toggled", Toast.LENGTH_SHORT).show();
        });

        endCallButton.setOnClickListener(v -> {
            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
            // Stop recording service
            stopService(new Intent(this, CallRecordingService.class));
            finish();
        });

        speakerButton.setOnClickListener(v -> {
            toggleSpeaker();
        });
    }

    private void toggleSpeaker() {
        if (audioManager != null) {
            isSpeakerOn = !isSpeakerOn;
            audioManager.setSpeakerphoneOn(isSpeakerOn);
            
            // Update the speaker button appearance
            if (isSpeakerOn) {
                speakerButton.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                speakerButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, R.color.teal_700));
                Toast.makeText(this, "Speaker on", Toast.LENGTH_SHORT).show();
            } else {
                speakerButton.setImageResource(android.R.drawable.ic_lock_silent_mode);
                speakerButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                Toast.makeText(this, "Speaker off", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makePhoneCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // Start the recording service
            if (Settings.canDrawOverlays(this)) {
                startService(new Intent(this, CallRecordingService.class));
            }

            // Make the actual phone call
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
            
            callStatusText.setText("Call in progress...");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE
            };
            
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
                    break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION || requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
                    makePhoneCall(phoneNumber);
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, proceed with call
                makePhoneCall(phoneNumber);
            } else {
                Toast.makeText(this, "Overlay permission is required for transcription display", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Reset audio settings
        if (audioManager != null && isSpeakerOn) {
            audioManager.setSpeakerphoneOn(false);
        }
        
        // Stop recording service
        stopService(new Intent(this, CallRecordingService.class));
    }
} 