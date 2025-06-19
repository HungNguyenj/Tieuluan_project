package com.nlu.convertapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nlu.convertapp.R;
import com.nlu.convertapp.api.ApiKeys;
import com.nlu.convertapp.api.ViettelAsrApi;
import com.nlu.convertapp.models.ViettelSpeechToTextResponse;
import com.nlu.convertapp.services.FloatingWindowService;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import android.media.AudioFormat;
import android.media.AudioRecord;

import java.io.RandomAccessFile;

public class PhoneCallRecorderActivity extends AppCompatActivity {

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
    private static final String VIETTEL_API_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;
    private static final int SEGMENT_DURATION = 4000;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentRecordingFile;
    private ImageButton callButton;
    private EditText phoneNumberInput;
    private TextView statusText;
    private PhoneStateListener phoneStateListener;
    private boolean isCallInProgress = false;
    private ViettelAsrApi viettelAsrApi;
    private AudioRecord audioRecord;
    private Thread recordingThread = null;
    private long recordingStartTime;
    private File recordingDir;
    private int totalBytesRead = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recorder);

        // Initialize recording directory
        recordingDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CallRecordings");
        if (!recordingDir.exists()) {
            recordingDir.mkdirs();
        }

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        callButton = findViewById(R.id.callButton);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        statusText = findViewById(R.id.statusText);

        setupViettelApi();
        setupPhoneStateListener();
        setupCallButton();
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
                        // Start floating window service
                        startService(new Intent(PhoneCallRecorderActivity.this, FloatingWindowService.class));
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Call is finished
                        if (isCallInProgress) {
                            isCallInProgress = false;
                            if (isRecording) {
                                stopRecording();
                            }
                            // Stop floating window service
                            stopService(new Intent(PhoneCallRecorderActivity.this, FloatingWindowService.class));
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

    private void setupCallButton() {
        callButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    String phoneNumber = phoneNumberInput.getText().toString().trim();
                    if (TextUtils.isEmpty(phoneNumber)) {
                        Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startRecordingAndCall(phoneNumber);
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

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        Toast.makeText(this, "Vui lòng cấp quyền hiển thị trên ứng dụng khác", Toast.LENGTH_LONG).show();
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
    }

    private void startRecordingAndCall(String phoneNumber) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
                return;
            }

            // Make the phone call
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);

            // Recording will start automatically when call is connected (via PhoneStateListener)
            statusText.setText("Đang kết nối cuộc gọi...");
            callButton.setEnabled(false);

        } catch (Exception e) {
            Log.e(TAG, "Error making phone call", e);
            Toast.makeText(this, "Lỗi khi thực hiện cuộc gọi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        try {
            // Tính buffer size
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                minBufferSize = SAMPLE_RATE * 2;
            }

            // Khởi tạo AudioRecord
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, 8192);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord không thể khởi tạo!");
            }

            // Bắt đầu ghi âm
            audioRecord.startRecording();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // Tạo file WAV đầu tiên
            startNewRecordingSegment();

            // Bắt đầu thread ghi âm
            int finalMinBufferSize = minBufferSize;
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[finalMinBufferSize];
                DataOutputStream dos = null;
                try {
                    dos = new DataOutputStream(new FileOutputStream(currentRecordingFile));
                    writeWavHeader(dos, CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_MONO ? 1 : 2, SAMPLE_RATE);
                    totalBytesRead = 0;

                    while (isRecording) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            dos.write(buffer, 0, read);
                            totalBytesRead += read;

                            // Kiểm tra thời gian ghi âm
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - recordingStartTime >= SEGMENT_DURATION) {
                                // Đóng file hiện tại và cập nhật header
                                finishCurrentSegment(dos);

                                // Chuyển đổi đoạn vừa ghi
                                final String completedFile = currentRecordingFile;
                                convertAudioSegment(new File(completedFile));

                                // Bắt đầu segment mới
                                dos = startNewRecordingSegment();
                                totalBytesRead = 0;
                                recordingStartTime = System.currentTimeMillis();
                            }
                        }
                    }

                    // Kết thúc segment cuối cùng
                    if (dos != null) {
                        finishCurrentSegment(dos);
                        convertAudioSegment(new File(currentRecordingFile));
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error writing audio data", e);
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
            callButton.setEnabled(false);
            statusText.setText("Đang ghi âm cuộc gọi...");
            Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            stopRecording();
        }
    }

    private DataOutputStream startNewRecordingSegment() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        currentRecordingFile = new File(recordingDir, "call_" + timestamp + ".wav").getAbsolutePath();
        
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(currentRecordingFile));
        writeWavHeader(dos, CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_MONO ? 1 : 2, SAMPLE_RATE);
        return dos;
    }

    private void finishCurrentSegment(DataOutputStream dos) throws IOException {
        dos.close();
        updateWavHeader(currentRecordingFile, totalBytesRead);
    }

    private void updateWavHeader(String filePath, int audioDataLength) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
        
        // Update ChunkSize
        raf.seek(4);
        raf.writeInt(Integer.reverseBytes(36 + audioDataLength));
        
        // Update Subchunk2Size
        raf.seek(40);
        raf.writeInt(Integer.reverseBytes(audioDataLength));
        
        raf.close();
    }

    private void convertAudioSegment(final File audioFile) {
        if (!audioFile.exists() || audioFile.length() == 0) {
            Log.e(TAG, "Invalid audio file: " + audioFile.getAbsolutePath());
            return;
        }

        RequestBody fileBody = RequestBody.create(MediaType.parse("audio/wav"), audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", 
                audioFile.getName(), fileBody);
        RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), VIETTEL_TOKEN);

        Call<ViettelSpeechToTextResponse> call = viettelAsrApi.convertSpeechToText(filePart, tokenBody);
        call.enqueue(new Callback<ViettelSpeechToTextResponse>() {
            @Override
            public void onResponse(Call<ViettelSpeechToTextResponse> call, 
                    Response<ViettelSpeechToTextResponse> response) {
                
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API error: " + response.code());
                    return;
                }

                ViettelSpeechToTextResponse sttResponse = response.body();
                if (sttResponse != null && sttResponse.getResponse() != null && 
                        sttResponse.getResponse().getResult() != null && 
                        !sttResponse.getResponse().getResult().isEmpty()) {
                    
                    ViettelSpeechToTextResponse.TranscriptResult result = 
                            sttResponse.getResponse().getResult().get(0);
                    String transcript = result.getTranscript();
                    Log.d("PhoneCallRecorderActivity", transcript);
                    
                    if (transcript != null && !transcript.trim().isEmpty()) {
                        handleTranscriptionResult(transcript);
                    }
                }
            }

            @Override
            public void onFailure(Call<ViettelSpeechToTextResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            try {
                recordingThread.join();
                recordingThread = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping recording thread", e);
            }
        }

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
        }

        callButton.setEnabled(true);
        statusText.setText("Ghi âm đã kết thúc");
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
        } else if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Cần cấp quyền hiển thị trên ứng dụng khác để hiển thị văn bản chuyển đổi", Toast.LENGTH_LONG).show();
                } else {
                    // Quyền đã được cấp, tiếp tục thực hiện cuộc gọi
                    String phoneNumber = phoneNumberInput.getText().toString().trim();
                    if (TextUtils.isEmpty(phoneNumber)) {
                        Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startRecordingAndCall(phoneNumber);
                }
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
        // Make sure to stop the floating window service
        stopService(new Intent(this, FloatingWindowService.class));
    }

    private void handleTranscriptionResult(String transcribedText) {
        // Create intent with the transcribed text
        Intent updateIntent = FloatingWindowService.createUpdateTextIntent(transcribedText);
        updateIntent.setClass(this, FloatingWindowService.class);
        startService(updateIntent);
    }
} 