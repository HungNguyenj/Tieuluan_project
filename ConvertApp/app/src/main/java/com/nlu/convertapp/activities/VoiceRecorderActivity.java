package com.nlu.convertapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.nlu.convertapp.R;
import com.nlu.convertapp.api.ApiKeys;
import com.nlu.convertapp.api.ViettelAsrApi;
import com.nlu.convertapp.models.ViettelSpeechToTextResponse;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.RandomAccessFile;

public class VoiceRecorderActivity extends AppCompatActivity {

    private static final String TAG = "VoiceRecorderActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int SETTINGS_REQUEST_CODE = 201;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final String PHONE_NUMBER = "5556";
    private static final String VIETTEL_API_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;
    private static final int SAMPLE_RATE = 16000; // 16kHz for voice
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentRecordingFile;
    private ImageButton recordButton;
    private TextView statusText;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isCallInProgress = false;
    private ViettelAsrApi viettelAsrApi;
    private AudioRecord audioRecord;
    private Thread recordingThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recorder);

        // Initialize telephony manager
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recordButton = findViewById(R.id.recordButton);
        statusText = findViewById(R.id.statusText);

        setupViettelApi();
        setupPhoneStateListener();
        setupRecordButton();
    }

    private void setupPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // Call is established
                        isCallInProgress = true;
                        startRecording();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Call is finished
                        if (isCallInProgress) {
                            isCallInProgress = false;
                            if (isRecording) {
                                stopRecording();
                            }
                        }
                        break;
                }
            }
        };

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void setupViettelApi() {
        // Create a custom TrustManager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        };

        // Create an SSLContext with custom TrustManager
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (Exception e) {
            Log.e(TAG, "Error setting up SSL context", e);
            return;
        }

        // Create an SSLSocketFactory with our all-trusting manager
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        viettelAsrApi = retrofit.create(ViettelAsrApi.class);
    }

    private void setupRecordButton() {
        recordButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                if (!isRecording) {
                    startRecordingAndCall();
                } else {
                    stopRecording();
                }
            } else {
                requestPermissions();
            }
        });
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    private void startRecordingAndCall() {
        try {
            // Make the phone call first
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + PHONE_NUMBER));
            startActivity(intent);

            // Recording will start automatically when call is connected (via PhoneStateListener)
            statusText.setText("Đang kết nối cuộc gọi...");
            recordButton.setImageResource(R.drawable.ic_fa_microphone);

        } catch (Exception e) {
            Log.e(TAG, "Error making phone call", e);
            Toast.makeText(this, "Lỗi khi thực hiện cuộc gọi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            // Ghi trực tiếp thành file WAV
            currentRecordingFile = new File(downloadDir, "call_recording_" + timestamp + ".wav").getAbsolutePath();

            // Tính buffer size
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                minBufferSize = SAMPLE_RATE * 2; // 2 bytes per short
            }

            // Khởi tạo AudioRecord
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord không thể khởi tạo!");
            }

            // Bắt đầu ghi âm
            audioRecord.startRecording();
            isRecording = true;

            // Tạo file WAV và ghi header
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(currentRecordingFile));
            // Viết WAV header trống, sẽ update sau
            writeWavHeader(dos, CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_MONO ? 1 : 2, SAMPLE_RATE);

            // Bắt đầu thread ghi âm
            int finalMinBufferSize = minBufferSize;
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[finalMinBufferSize];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        try {
                            dos.write(buffer, 0, read);
                        } catch (IOException e) {
                            Log.e(TAG, "Error writing audio data", e);
                            break;
                        }
                    }
                }

                // Đóng file và cập nhật WAV header
                try {
                    dos.close();
                    updateWavHeader(currentRecordingFile);
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output file", e);
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
            recordButton.setImageResource(R.drawable.ic_fa_microphone);
            statusText.setText("Đang ghi âm cuộc gọi...");
            Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            stopRecording();
        }
    }

    private void stopRecording() {
        try {
            isRecording = false;
            
            if (recordingThread != null) {
                recordingThread.join();
                recordingThread = null;
            }

            if (audioRecord != null) {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }

            recordButton.setImageResource(R.drawable.ic_fa_microphone);
            statusText.setText("Đã dừng ghi âm. Đang chuyển đổi thành văn bản...");

            // Gửi file WAV trực tiếp lên API
            convertAudioToText();

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            Toast.makeText(this, "Lỗi khi dừng ghi âm: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void writeWavHeader(DataOutputStream dos, int channels, int sampleRate) throws IOException {
        // RIFF header
        dos.writeBytes("RIFF"); // ChunkID
        dos.writeInt(0); // ChunkSize (will be updated later)
        dos.writeBytes("WAVE"); // Format
        
        // fmt subchunk
        dos.writeBytes("fmt "); // Subchunk1ID
        dos.writeInt(Integer.reverseBytes(16)); // Subchunk1Size
        dos.writeShort(Short.reverseBytes((short) 1)); // AudioFormat (PCM = 1)
        dos.writeShort(Short.reverseBytes((short) channels)); // NumChannels
        dos.writeInt(Integer.reverseBytes(sampleRate)); // SampleRate
        dos.writeInt(Integer.reverseBytes(sampleRate * channels * 2)); // ByteRate
        dos.writeShort(Short.reverseBytes((short) (channels * 2))); // BlockAlign
        dos.writeShort(Short.reverseBytes((short) 16)); // BitsPerSample
        
        // data subchunk
        dos.writeBytes("data"); // Subchunk2ID
        dos.writeInt(0); // Subchunk2Size (will be updated later)
    }

    private void updateWavHeader(String filePath) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
        long fileSize = raf.length();
        
        // Update ChunkSize
        raf.seek(4);
        raf.writeInt(Integer.reverseBytes((int) (fileSize - 8)));
        
        // Update Subchunk2Size
        raf.seek(40);
        raf.writeInt(Integer.reverseBytes((int) (fileSize - 44)));
        
        raf.close();
    }

    private void convertAudioToText() {
        File audioFile = new File(currentRecordingFile);
        
        // Kiểm tra file tồn tại và kích thước
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: " + currentRecordingFile);
            statusText.setText("Lỗi: File ghi âm không tồn tại");
            return;
        }
        
        if (audioFile.length() == 0) {
            Log.e(TAG, "Audio file is empty: " + currentRecordingFile);
            statusText.setText("Lỗi: File ghi âm trống");
            return;
        }

        Log.d(TAG, "Converting audio file: " + audioFile.getAbsolutePath());
        Log.d(TAG, "File size: " + audioFile.length() + " bytes");

        // Gửi file WAV trực tiếp lên API
        RequestBody fileBody = RequestBody.create(MediaType.parse("audio/wav"), audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", 
                audioFile.getName(), fileBody);
        RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), VIETTEL_TOKEN);

        // Log request details
        Log.d(TAG, "Request details:");
        Log.d(TAG, "File name: " + audioFile.getName());
        Log.d(TAG, "Content type: audio/wav");
        Log.d(TAG, "Token: " + VIETTEL_TOKEN);

        Call<ViettelSpeechToTextResponse> call = viettelAsrApi.convertSpeechToText(filePart, tokenBody);
        call.enqueue(new Callback<ViettelSpeechToTextResponse>() {
            @Override
            public void onResponse(Call<ViettelSpeechToTextResponse> call, 
                    Response<ViettelSpeechToTextResponse> response) {
                
                // Log response details
                Log.d(TAG, "Response code: " + response.code());
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error response: " + errorBody);
                        statusText.setText("Lỗi API (" + response.code() + "): " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                        statusText.setText("Lỗi API: " + response.code());
                    }
                    return;
                }

                ViettelSpeechToTextResponse sttResponse = response.body();
                if (sttResponse != null) {
                    Log.d(TAG, "API Response: " + new Gson().toJson(sttResponse));
                    
                    if (sttResponse.getResponse() != null && 
                            sttResponse.getResponse().getResult() != null && 
                            !sttResponse.getResponse().getResult().isEmpty()) {
                        
                        ViettelSpeechToTextResponse.TranscriptResult result = 
                                sttResponse.getResponse().getResult().get(0);
                        String transcript = result.getTranscript();
                        double confidence = result.getConfidence();

                        Log.d(TAG, "Transcript: " + transcript);
                        Log.d(TAG, "Confidence: " + confidence);

                        String displayText = "Nội dung cuộc gọi:\n" + transcript + 
                                "\nĐộ tin cậy: " + String.format("%.2f%%", confidence * 100) +
                                "\n\nFile ghi âm đã được lưu tại: " + currentRecordingFile;
                        statusText.setText(displayText);
                        
                        // Thông báo thành công
                        Toast.makeText(VoiceRecorderActivity.this, 
                            "Đã chuyển đổi thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        statusText.setText("Không nhận dạng được nội dung âm thanh");
                    }
                } else {
                    statusText.setText("Không nhận được phản hồi từ server");
                }
            }

            @Override
            public void onFailure(Call<ViettelSpeechToTextResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                statusText.setText("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : results) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Đã cấp tất cả quyền cần thiết", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần cấp tất cả quyền để ghi âm", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (checkPermissions()) {
                Toast.makeText(this, "Đã được cấp đủ quyền", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vui lòng cấp đủ quyền để sử dụng tính năng ghi âm cuộc gọi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (phoneStateListener != null) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }
        if (isRecording) {
            stopRecording();
        }
    }
} 