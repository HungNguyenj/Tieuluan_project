package com.nlu.convertapp.activities;

import android.annotation.SuppressLint;
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
import com.nlu.convertapp.repository.TextStorageRepository;
import com.nlu.convertapp.models.TextStorageItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMetadataRetriever;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;

public class SpeechToTextActivity extends AppCompatActivity {

    //elevenlabs
    private static final String ELEVENLABS_API_URL = "https://api.elevenlabs.io/";
    private static final String ELEVENLABS_API_KEY = ApiKeys.ELEVENLABS_API_KEY;
    private static final String ELEVENLABS_MODEL_ID = ApiKeys.ELEVENLABS_MODEL_ID;
    
    //viettel
    private static final String VIETTEL_API_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;

    private static final int LANGUAGE_ENGLISH = 0;
    private static final int LANGUAGE_VIETNAMESE = 1;
    private int currentLanguage = LANGUAGE_ENGLISH;

    private static final String[] SUPPORTED_AUDIO_FORMATS = {
        "audio/wav", "audio/x-wav", "audio/mp3", "audio/mpeg"
    };

    private Uri selectedAudioFileUri;
    private ElevenLabsApi elevenLabsApi;
    private ViettelAsrApi viettelAsrApi;

    //UI
    private Toolbar toolbar;
    private Spinner languageSpinner;
    private ImageButton micButton;
    private MaterialButton addFileButton;
    private MaterialButton convertButton;
    private TextView recognizedText;
    private ImageButton copyButton;
    private ImageButton starButton;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    //record
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread = null;
    private String audioFilePath;

    private TextStorageRepository textStorageRepository;
    private boolean isStarred = false;

    // Add file from phone
    private final ActivityResultLauncher<Intent> filePickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            selectedAudioFileUri = uri;
                            String fileName = getFileName(uri);
                            addFileButton.setText(fileName);
                            addFileButton.setIcon(getResources().getDrawable(R.drawable.ic_baseline_file_upload_24, getTheme()));
                            Toast.makeText(this, "Thêm File thành công", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            selectedAudioFileUri = null;
                            Toast.makeText(this, "Lỗi khi thêm File: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Lỗi: Không thể lấy được dữ liệu File", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() != Activity.RESULT_CANCELED) {
                    // cancel select file
                    Toast.makeText(this, "Lỗi khi chọn File", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_text);
        
        //repository
        textStorageRepository = new TextStorageRepository(this);
        textStorageRepository.open();
        
        initializeViews();
        setSupportActionBar(toolbar);
        
        toolbar.setNavigationOnClickListener(v -> finish());
        
        setupRetrofit();
        setupButtonListeners();
        setupLanguageSpinner();
        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allPermissionsGranted = true;
            //check
            for (String permission : REQUIRED_PERMISSIONS) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Cần có quyền để ghi âm", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRetrofit() {
        // elevenLabs API
        Retrofit elevenLabsRetrofit = new Retrofit.Builder()
                .baseUrl(ELEVENLABS_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        elevenLabsApi = elevenLabsRetrofit.create(ElevenLabsApi.class);

        // viettel API
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

    // language spinner
    private void setupLanguageSpinner() {
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLanguage = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentLanguage = LANGUAGE_ENGLISH;
            }
        });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        languageSpinner = findViewById(R.id.languageSpinner);
        micButton = findViewById(R.id.micButton);
        addFileButton = findViewById(R.id.addFileButton);
        convertButton = findViewById(R.id.convertButton);
        recognizedText = findViewById(R.id.recognizedText);
        copyButton = findViewById(R.id.copyButton);
        starButton = findViewById(R.id.starButton);
    }

    private void setupButtonListeners() {
        micButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        addFileButton.setOnClickListener(v -> {
            openFilePicker();
        });

        convertButton.setOnClickListener(v -> {
            if (selectedAudioFileUri != null) {
                convertSpeechToText(selectedAudioFileUri);
            } else {
                Toast.makeText(this, "Vui lòng chọn một tập tin âm thanh trước", Toast.LENGTH_SHORT).show();
            }
        });

        copyButton.setOnClickListener(v -> {
            copyTextToClipboard(recognizedText.getText().toString());
        });

        starButton.setOnClickListener(v -> {
            String text = recognizedText.getText().toString().trim();
            if (!text.isEmpty()) {
                isStarred = !isStarred;
                if (isStarred) {
                    // save to db
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
                    String currentDate = sdf.format(new Date());
                    
                    textStorageRepository.insertText(new TextStorageItem(currentDate, text, true));
                    starButton.setImageResource(R.drawable.ic_baseline_star_solid);
                    Toast.makeText(this, "Văn bản đã được lưu vào bộ nhớ", Toast.LENGTH_SHORT).show();
                } else {
                    starButton.setImageResource(R.drawable.ic_baseline_star_regular);
                }
            } else {
                Toast.makeText(this, "Không có văn bản nào có sẵn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //file picker
    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_AUDIO_FORMATS);
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi mở trình chọn tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // convert STT
    private void convertSpeechToText(Uri audioFileUri) {
        try {
            //check type
            String mimeType = getContentResolver().getType(audioFileUri);

            //check size
            long fileSize = getFileSizeFromUri(audioFileUri);
            if (fileSize <= 0) {
                recognizedText.setText("Lỗi: Không thể đọc file hoặc file rỗng");
                return;
            }
            
            //loading state
            recognizedText.setText("Đang xử lý file âm thanh...");
            convertButton.setEnabled(false);

            File audioFile = createTempFileFromUri(audioFileUri);
            
            //check
            if (!audioFile.exists() || audioFile.length() == 0) {
                recognizedText.setText("Lỗi: File tạm không hợp lệ");
                return;
            }

            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(audioFile.getAbsolutePath());
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Log.d("STT", "Audio duration: " + duration + " ms");
                retriever.release();
            } catch (Exception e) {
                Log.e("STT", "Error reading audio metadata", e);
                recognizedText.setText("Lỗi: File âm thanh không hợp lệ");
                return;
            }
            
            //convert with language
            if (currentLanguage == LANGUAGE_ENGLISH) {
                convertWithElevenLabs(audioFile);
            } else {
                convertWithViettelAsr(audioFile);
            }
            
        } catch (IOException e) {
            Log.e("STT", "Error processing file", e);
            convertButton.setEnabled(true);
            recognizedText.setText("Lỗi xử lý file: " + e.getMessage());
        }
    }
    
    // Convert using ElevenLabs
    private void convertWithElevenLabs(File audioFile) {
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
        //media type
        String contentType = "audio/wav"; // default
        if (audioFile.getName().toLowerCase().endsWith(".mp3")) {
            contentType = "audio/mpeg";
        }
        
        //request body
        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", 
            audioFile.getName(), fileBody);
        
        //token part
        RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), VIETTEL_TOKEN);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    return chain.proceed(original);
                })
                .addInterceptor(new HttpLoggingInterceptor(message -> 
                    Log.d("STT", "OkHttp: " + message))
                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        Retrofit viettelRetrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        viettelAsrApi = viettelRetrofit.create(ViettelAsrApi.class);

        Call<ViettelSpeechToTextResponse> call = viettelAsrApi.convertSpeechToText(filePart, tokenBody);
        
        call.enqueue(new Callback<ViettelSpeechToTextResponse>() {
            @Override
            public void onResponse(Call<ViettelSpeechToTextResponse> call, Response<ViettelSpeechToTextResponse> response) {
                convertButton.setEnabled(true);

                if (response.isSuccessful()) {
                    ViettelSpeechToTextResponse viettelResponse = response.body();
                    if (viettelResponse != null) {
                        if (viettelResponse.getResponse() != null && 
                            viettelResponse.getResponse().getResult() != null && 
                            !viettelResponse.getResponse().getResult().isEmpty()) {
                            
                            ViettelSpeechToTextResponse.TranscriptResult result = viettelResponse.getResponse().getResult().get(0);

                            String transcript = result.getTranscript();
                            double confidence = result.getConfidence();
                            if (transcript != null && !transcript.isEmpty()) {

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
                recognizedText.setText("Lỗi: " + t.getMessage());
            }
        });
    }

    //get file size
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
            Log.e("STT", "Error getting file size", e);
        }
        
        // Nếu không lấy được qua cursor, thử đọc trực tiếp
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                return inputStream.available();
            }
        } catch (IOException e) {
            Log.e("STT", "Error reading file size from stream", e);
        }
        
        return -1;
    }
    
    //create temp file
    private File createTempFileFromUri(Uri uri) throws IOException {
        String fileName = getFileName(uri);
        String extension = getFileExtension(fileName);

        File tempFile = File.createTempFile("audio_", extension, getCacheDir());
        Log.d("STT", "Creating temp file: " + tempFile.getAbsolutePath());

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

            if (tempFile.length() != totalBytes) {
                throw new IOException("File size mismatch. Expected: " + totalBytes + ", Actual: " + tempFile.length());
            }
            return tempFile;
        }
    }

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

    //copy
    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Recognized Text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Văn bản đã được sao chép vào clipboard", Toast.LENGTH_SHORT).show();
    }

    private void resetFileSelection() {
        selectedAudioFileUri = null;
        addFileButton.setText(R.string.add_file);
        addFileButton.setIcon(getResources().getDrawable(R.drawable.ic_baseline_file_upload_24, getTheme()));
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        try {
            File outputDir = new File(getExternalFilesDir(null), "AudioRecordings");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            //output file
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            audioFilePath = new File(outputDir, "AUDIO_" + timestamp + ".wav").getAbsolutePath();

            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

            //start recording
            audioRecord.startRecording();
            isRecording = true;

            micButton.setImageResource(R.drawable.ic_stop);
            Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show();

            //start thread
            recordingThread = new Thread(() -> {
                writeAudioDataToFile(minBufferSize);
            }, "AudioRecorder Thread");
            recordingThread.start();

        } catch (Exception e) {
            Log.e("STT", "Error starting recording", e);
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void writeAudioDataToFile(int bufferSize) {
        byte[] audioData = new byte[bufferSize];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioFilePath);
            writeWavHeader(os, CHANNEL_CONFIG, SAMPLE_RATE, AUDIO_FORMAT);

            while (isRecording) {
                int read = audioRecord.read(audioData, 0, bufferSize);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    os.write(audioData, 0, read);
                }
            }

            updateWavHeader(audioFilePath);

        } catch (IOException e) {
            Log.e("STT", "Error writing audio file", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e("STT", "Error closing output stream", e);
                }
            }
        }
    }

    private void writeWavHeader(FileOutputStream out, int channelConfig,
                              int sampleRate, int audioFormat) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(buffer);

        // RIFF header
        data.writeBytes("RIFF"); // ChunkID Resource Interchange File Format
        data.writeInt(0); // ChunkSize (will be updated later)
        data.writeBytes("WAVE"); //format
        
        // fmt subchunk
        data.writeBytes("fmt "); // Subchunk1ID
        data.writeInt(Integer.reverseBytes(16)); // Subchunk1Size
        data.writeShort(Short.reverseBytes((short) 1)); // AudioFormat (PCM = 1)
        data.writeShort(Short.reverseBytes((short) 1)); // NumChannels (Mono = 1)
        data.writeInt(Integer.reverseBytes(sampleRate)); // SampleRate
        data.writeInt(Integer.reverseBytes(sampleRate * 2)); // ByteRate
        data.writeShort(Short.reverseBytes((short) 2)); // BlockAlign
        data.writeShort(Short.reverseBytes((short) 16)); // BitsPerSample
        
        // data subchunk
        data.writeBytes("data"); // Subchunk2ID
        data.writeInt(0); // Subchunk2Size (will be updated later)

        // Write header
        out.write(buffer.toByteArray());
    }

    private void updateWavHeader(String filePath) {
        try {
            File file = new File(filePath);
            long fileSize = file.length();
            long dataSize = fileSize - 44; // Total size minus header size

            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            // ChunkSize
            raf.seek(4);
            raf.writeInt(Integer.reverseBytes((int) (fileSize - 8)));
            // Subchunk2Size
            raf.seek(40);
            raf.writeInt(Integer.reverseBytes((int) dataSize));
            raf.close();
        } catch (IOException e) {
            Log.e("STT", "Error updating WAV header", e);
        }
    }

    private void stopRecording() {
        if (audioRecord != null) {
            try {
                isRecording = false;
                
                // Wait for recording thread to finish
                if (recordingThread != null) {
                    recordingThread.join();
                    recordingThread = null;
                }

                //stop and release audioRecord
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

                micButton.setImageResource(R.drawable.ic_fa_microphone);
                Toast.makeText(this, "Dừng ghi âm", Toast.LENGTH_SHORT).show();

                //convert the record file
                Uri audioUri = Uri.fromFile(new File(audioFilePath));
                selectedAudioFileUri = audioUri;
                convertSpeechToText(audioUri);

            } catch (Exception e) {
                Log.e("STT", "Error stopping recording", e);
                Toast.makeText(this, "Lỗi khi dừng ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (textStorageRepository != null) {
            textStorageRepository.close();
        }
    }
}