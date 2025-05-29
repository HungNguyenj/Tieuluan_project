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
import android.util.Log;
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
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.logging.HttpLoggingInterceptor;
import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import android.media.MediaMetadataRetriever;

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

    // Thêm hằng số cho phép kiểm tra định dạng file
    private static final String[] SUPPORTED_AUDIO_FORMATS = {
        "audio/wav", "audio/x-wav", "audio/mp3", "audio/mpeg"
    };

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
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        Retrofit viettelRetrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_API_URL)
                .client(client)
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
            intent.putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_AUDIO_FORMATS);
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Convert speech to text
    private void convertSpeechToText(Uri audioFileUri) {
        try {
            // Kiểm tra mime type của file
            String mimeType = getContentResolver().getType(audioFileUri);
            Log.d("ViettelASR", "File MIME type: " + mimeType);
            
            // Kiểm tra kích thước file trước khi xử lý
            long fileSize = getFileSizeFromUri(audioFileUri);
            Log.d("ViettelASR", "Original file size: " + fileSize + " bytes");
            
            if (fileSize <= 0) {
                recognizedText.setText("Lỗi: Không thể đọc file hoặc file rỗng");
                return;
            }
            
            // Show loading state
            recognizedText.setText("Đang xử lý file âm thanh...");
            convertButton.setEnabled(false);

            // Tạo temporary file
            File audioFile = createTempFileFromUri(audioFileUri);
            
            // Kiểm tra file tạm sau khi tạo
            if (!audioFile.exists() || audioFile.length() == 0) {
                recognizedText.setText("Lỗi: File tạm không hợp lệ");
                return;
            }
            
            // Log thông tin file tạm
            Log.d("ViettelASR", "Temp file created: " + audioFile.getAbsolutePath());
            Log.d("ViettelASR", "Temp file size: " + audioFile.length() + " bytes");
            
            // Kiểm tra nội dung file có đọc được không
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(audioFile.getAbsolutePath());
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Log.d("ViettelASR", "Audio duration: " + duration + " ms");
                retriever.release();
            } catch (Exception e) {
                Log.e("ViettelASR", "Error reading audio metadata", e);
                recognizedText.setText("Lỗi: File âm thanh không hợp lệ");
                return;
            }
            
            // Chọn API phù hợp
            if (currentLanguage == LANGUAGE_ENGLISH) {
                convertWithElevenLabs(audioFile);
            } else {
                convertWithViettelAsr(audioFile);
            }
            
        } catch (IOException e) {
            Log.e("ViettelASR", "Error processing file", e);
            convertButton.setEnabled(true);
            recognizedText.setText("Lỗi xử lý file: " + e.getMessage());
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
        // Log thông tin file trước khi gửi
        Log.d("ViettelASR", String.format("Sending file: %s (size: %d bytes)", 
            audioFile.getName(), audioFile.length()));
        
        // Xác định MediaType dựa trên extension
        String contentType = "audio/wav"; // default
        if (audioFile.getName().toLowerCase().endsWith(".mp3")) {
            contentType = "audio/mpeg";
        }
        
        // Tạo request body với content type phù hợp
        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", 
            audioFile.getName(), fileBody);
        
        // Tạo token part
        RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), VIETTEL_TOKEN);
        
        // Tạo OkHttpClient với timeout và logging
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Log.d("ViettelASR", "Sending request to: " + original.url());
                    Log.d("ViettelASR", "Request headers: " + original.headers());
                    return chain.proceed(original);
                })
                .addInterceptor(new HttpLoggingInterceptor(message -> 
                    Log.d("ViettelASR", "OkHttp: " + message))
                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        
        // Tạo Retrofit instance
        Retrofit viettelRetrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        viettelAsrApi = viettelRetrofit.create(ViettelAsrApi.class);
        
        // Gửi request
        Call<ViettelSpeechToTextResponse> call = viettelAsrApi.convertSpeechToText(filePart, tokenBody);
        
        call.enqueue(new Callback<ViettelSpeechToTextResponse>() {
            @Override
            public void onResponse(Call<ViettelSpeechToTextResponse> call, Response<ViettelSpeechToTextResponse> response) {
                convertButton.setEnabled(true);
                
                // Log chi tiết response
                Log.d("ViettelASR", "HTTP Status Code: " + response.code());
                if (response.body() != null) {
                    Log.d("ViettelASR", "Full Response: " + new Gson().toJson(response.body()));
                }
                
                if (response.isSuccessful()) {
                    ViettelSpeechToTextResponse viettelResponse = response.body();
                    if (viettelResponse != null) {
                        // Log chi tiết từng trường
                        Log.d("ViettelASR", "API Code: " + viettelResponse.getCode());
                        Log.d("ViettelASR", "API Message: " + viettelResponse.getMessage());
                        
                        if (viettelResponse.getResponse() != null && 
                            viettelResponse.getResponse().getResult() != null && 
                            !viettelResponse.getResponse().getResult().isEmpty()) {
                            
                            ViettelSpeechToTextResponse.TranscriptResult result = viettelResponse.getResponse().getResult().get(0);
                            String transcript = result.getTranscript();
                            double confidence = result.getConfidence();
                            
                            // Log kết quả
                            Log.d("ViettelASR", "Transcript: " + transcript);
                            Log.d("ViettelASR", "Confidence: " + confidence);
                            
                            if (transcript != null && !transcript.isEmpty()) {
                                // Hiển thị kết quả với độ tin cậy
                                String displayText = transcript + "\n" +
                                                  "Độ tin cậy: " + String.format("%.2f%%", confidence * 100);
                                recognizedText.setText(displayText);
                            } else {
                                recognizedText.setText("Không nhận dạng được nội dung âm thanh");
                            }
                        } else {
                            recognizedText.setText("Không có kết quả nhận dạng");
                        }
                    } else {
                        recognizedText.setText("Không nhận được phản hồi từ server");
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        errorBody = "Could not read error body";
                    }
                    recognizedText.setText("Lỗi kết nối: " + response.code() + 
                                         "\nChi tiết: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<ViettelSpeechToTextResponse> call, Throwable t) {
                convertButton.setEnabled(true);
                Log.e("ViettelASR", "Network Error", t);
                recognizedText.setText("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // Thêm phương thức lấy kích thước file
    private long getFileSizeFromUri(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                cursor.moveToFirst();
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
        } catch (Exception e) {
            Log.e("ViettelASR", "Error getting file size", e);
        }
        
        // Nếu không lấy được qua cursor, thử đọc trực tiếp
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                return inputStream.available();
            }
        } catch (IOException e) {
            Log.e("ViettelASR", "Error reading file size from stream", e);
        }
        
        return -1;
    }

    // Sửa lại phương thức tạo file tạm
    private File createTempFileFromUri(Uri uri) throws IOException {
        String fileName = getFileName(uri);
        String extension = getFileExtension(fileName);
        
        // Tạo file tạm với extension gốc
        File tempFile = File.createTempFile("audio_", extension, getCacheDir());
        Log.d("ViettelASR", "Creating temp file: " + tempFile.getAbsolutePath());
        
        // Đọc và ghi file với buffer lớn hơn
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }
            
            byte[] buffer = new byte[16384]; // 16KB buffer
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                Log.d("ViettelASR", "Bytes written: " + totalBytes);
            }
            
            outputStream.flush();
            
            // Verify file was written correctly
            if (tempFile.length() != totalBytes) {
                throw new IOException("File size mismatch. Expected: " + totalBytes + ", Actual: " + tempFile.length());
            }
            
            return tempFile;
        }
    }

    // Thêm phương thức lấy extension của file
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot != -1) {
            return fileName.substring(lastDot);
        }
        return ".tmp";
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