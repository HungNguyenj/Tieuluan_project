package com.nlu.convertapp.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nlu.convertapp.R;
import com.nlu.convertapp.api.ElevenLabsApi;
import com.nlu.convertapp.api.ViettelAsrApi;
import com.nlu.convertapp.models.SpeechToTextResponse;
import com.nlu.convertapp.models.ViettelSpeechToTextResponse;
import com.nlu.convertapp.api.ApiKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpeechToTextActivity extends AppCompatActivity {

    // ElevenLabs API constants
    private static final String ELEVENLABS_API_URL = "https://api.elevenlabs.io/";
    private static final String ELEVENLABS_API_KEY = ApiKeys.ELEVENLABS_API_KEY;
    private static final String ELEVENLABS_MODEL_ID = ApiKeys.ELEVENLABS_MODEL_ID;
    
    // Viettel AI ASR constants
    private static final String VIETTEL_API_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;
    
    // Language constants
    private static final int LANGUAGE_ENGLISH = 0;
    private static final int LANGUAGE_VIETNAMESE = 1;
    private int currentLanguage = LANGUAGE_ENGLISH;

    private Toolbar toolbar;
    private Spinner languageSpinner;
    private ImageButton micButton;
    private MaterialButton addFileButton;
    private MaterialButton convertButton;
    private TextView recognizedText;
    private ImageButton copyButton;
    private ImageButton starButton;
    
    private Uri selectedAudioFileUri;
    private ElevenLabsApi elevenLabsApi;
    private ViettelAsrApi viettelAsrApi;

    // Add file from phone
    private final ActivityResultLauncher<Intent> filePickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            selectedAudioFileUri = uri;
                            String fileName = getFileName(uri);
                            // Change the button text to show the selected filename
                            addFileButton.setText(fileName);
                            addFileButton.setIcon(getResources().getDrawable(R.drawable.ic_baseline_file_upload_24, getTheme()));
                            Toast.makeText(this, "File added successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            selectedAudioFileUri = null;
                            Toast.makeText(this, "Error adding file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error: Could not get file data", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() != Activity.RESULT_CANCELED) {
                    // Only show error if it wasn't a user cancellation
                    Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_text);
        
        initializeViews();
        setSupportActionBar(toolbar);
        
        toolbar.setNavigationOnClickListener(v -> finish());
        
        setupRetrofit();
        setupButtonListeners();
        setupLanguageSpinner();
    }

    private void setupRetrofit() {
        // Initialize ElevenLabs API
        Retrofit elevenLabsRetrofit = new Retrofit.Builder()
                .baseUrl(ELEVENLABS_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        elevenLabsApi = elevenLabsRetrofit.create(ElevenLabsApi.class);
        
        // Initialize Viettel ASR API
        Retrofit viettelRetrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        viettelAsrApi = viettelRetrofit.create(ViettelAsrApi.class);
    }

    // Setup language spinner
    private void setupLanguageSpinner() {
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLanguage = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentLanguage = LANGUAGE_ENGLISH; // Default to English
            }
        });
    }

    // Initialize views
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

    // Setup button listeners
    private void setupButtonListeners() {
        micButton.setOnClickListener(v -> {
            // Handle speech input (not implemented in this example)
            Toast.makeText(this, "Microphone recording not implemented in this example", Toast.LENGTH_SHORT).show();
        });

        addFileButton.setOnClickListener(v -> {
            openFilePicker();
        });

        convertButton.setOnClickListener(v -> {
            if (selectedAudioFileUri != null) {
                convertSpeechToText(selectedAudioFileUri);
            } else {
                Toast.makeText(this, "Please select an audio file first", Toast.LENGTH_SHORT).show();
            }
        });

        copyButton.setOnClickListener(v -> {
            copyTextToClipboard(recognizedText.getText().toString());
        });

        starButton.setOnClickListener(v -> {
            // Handle favorite (not implemented in this example)
            Toast.makeText(this, "Favorite function not implemented in this example", Toast.LENGTH_SHORT).show();
        });
    }

    // Open file picker
    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Convert speech to text
    private void convertSpeechToText(Uri audioFileUri) {
        try {
            // Show loading state
            recognizedText.setText("Converting speech to text...");
            convertButton.setEnabled(false);

            // Create temporary file from Uri
            File audioFile = createTempFileFromUri(audioFileUri);
            
            // Choose the appropriate API based on language selection
            if (currentLanguage == LANGUAGE_ENGLISH) {
                convertWithElevenLabs(audioFile);
            } else {
                convertWithViettelAsr(audioFile);
            }
            
        } catch (IOException e) {
            convertButton.setEnabled(true);
            recognizedText.setText("Error processing file: " + e.getMessage());
        }
    }
    
    // Convert using ElevenLabs
    private void convertWithElevenLabs(File audioFile) {
        // Prepare multipart request
        RequestBody modelIdBody = RequestBody.create(MediaType.parse("text/plain"), ELEVENLABS_MODEL_ID);
        RequestBody fileBody = RequestBody.create(MediaType.parse("audio/*"), audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file", 
                audioFile.getName(), 
                fileBody
        );
        
        // Make API call
        Call<SpeechToTextResponse> call = elevenLabsApi.convertSpeechToText(ELEVENLABS_API_KEY, modelIdBody, filePart);
        
        call.enqueue(new Callback<SpeechToTextResponse>() {
            @Override
            public void onResponse(Call<SpeechToTextResponse> call, Response<SpeechToTextResponse> response) {
                convertButton.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    SpeechToTextResponse sttResponse = response.body();
                    recognizedText.setText(sttResponse.getText());
                } else {
                    recognizedText.setText("Error: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<SpeechToTextResponse> call, Throwable t) {
                convertButton.setEnabled(true);
                recognizedText.setText("Error: " + t.getMessage());
            }
        });
    }
    
    // Convert using Viettel ASR
    private void convertWithViettelAsr(File audioFile) {
        // Prepare multipart request
        RequestBody fileBody = RequestBody.create(MediaType.parse("audio/*"), audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file", 
                audioFile.getName(), 
                fileBody
        );
        
        // Make API call
        Call<ViettelSpeechToTextResponse> call = viettelAsrApi.convertSpeechToText(filePart, VIETTEL_TOKEN);
        
        call.enqueue(new Callback<ViettelSpeechToTextResponse>() {
            @Override
            public void onResponse(Call<ViettelSpeechToTextResponse> call, Response<ViettelSpeechToTextResponse> response) {
                convertButton.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ViettelSpeechToTextResponse viettelResponse = response.body();
                    
                    if (viettelResponse.getCode() == 0 && viettelResponse.getResponse() != null) {
                        // Success
                        recognizedText.setText(viettelResponse.getResponse().getText());
                    } else {
                        // API error
                        recognizedText.setText("API Error: " + viettelResponse.getMessage());
                    }
                } else {
                    // HTTP error
                    recognizedText.setText("Error: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ViettelSpeechToTextResponse> call, Throwable t) {
                convertButton.setEnabled(true);
                recognizedText.setText("Network Error: " + t.getMessage());
            }
        });
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        String fileName = getFileName(uri);
        File tempFile = new File(getCacheDir(), fileName);
        
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            return tempFile;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Recognized Text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    // Reset the file selection
    private void resetFileSelection() {
        selectedAudioFileUri = null;
        addFileButton.setText(R.string.add_file);
        addFileButton.setIcon(getResources().getDrawable(R.drawable.ic_baseline_file_upload_24, getTheme()));
    }
}