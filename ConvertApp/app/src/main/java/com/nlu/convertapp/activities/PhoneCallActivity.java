package com.nlu.convertapp.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Locale;

public class PhoneCallActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_CALL_PHONE_PERMISSION = 100;

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

        // Initialize speech recognizer
        initializeSpeechRecognizer();

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
            // End call logic
            stopSpeechRecognition();
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
            // For a real app, you'd use the system phone call function:
            // Intent intent = new Intent(Intent.ACTION_CALL);
            // intent.setData(Uri.parse("tel:" + phoneNumber));
            // startActivity(intent);
            
            // For this demo, we'll simulate a call
            callStatusText.setText("Call in progress...");
            
            // Start speech recognition automatically
            if (checkAudioPermission()) {
                startSpeechRecognition();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int error) {
                    // Restart listening in continuous mode
                    if (isListening) {
                        startSpeechRecognition();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String currentText = sttResultText.getText().toString();
                        if (currentText.equals("Speech-to-text results will appear here...")) {
                            currentText = "";
                        }
                        
                        // Append new text
                        String newText = currentText.isEmpty() ? 
                                matches.get(0) : currentText + "\n" + matches.get(0);
                        sttResultText.setText(newText);
                    }
                    
                    // Restart listening in continuous mode
                    if (isListening) {
                        startSpeechRecognition();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        } else {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSpeechRecognition() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        try {
            isListening = true;
            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error starting speech recognition: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isListening = false;
        }
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        isListening = false;
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
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
                if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
                    startSpeechRecognition();
                } else if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
                    makePhoneCall(phoneNumber);
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        // Reset audio settings
        if (audioManager != null && isSpeakerOn) {
            audioManager.setSpeakerphoneOn(false);
        }
    }
} 