package com.nlu.convertapp.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.nlu.convertapp.api.ApiKeys;
import com.nlu.convertapp.api.ViettelAsrApi;
import com.nlu.convertapp.models.ViettelSpeechToTextResponse;

import java.io.File;
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

public class CallRecordingService extends Service {
    private static final String TAG = "CallRecordingService";
    private static final String VIETTEL_API_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;
    private static final long TRANSCRIPTION_INTERVAL = 5000; // 5 seconds

    private MediaRecorder mediaRecorder;
    private String currentRecordingFile;
    private Handler transcriptionHandler;
    private ViettelAsrApi viettelAsrApi;
    private boolean isRecording = false;
    private Intent floatingWindowIntent;
    private File recordingDir;

    @Override
    public void onCreate() {
        super.onCreate();
        setupViettelApi();
        transcriptionHandler = new Handler();
        
        // Create recording directory
        recordingDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings");
        if (!recordingDir.exists()) {
            recordingDir.mkdirs();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !isRecording) {
            startFloatingWindow();
            startRecording();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupViettelApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        viettelAsrApi = retrofit.create(ViettelAsrApi.class);
    }

    private void startFloatingWindow() {
        floatingWindowIntent = new Intent(this, FloatingWindowService.class);
        startService(floatingWindowIntent);
    }

    private void startRecording() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentRecordingFile = new File(recordingDir, "call_" + timestamp + ".wav").getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(currentRecordingFile);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            startTranscriptionLoop();

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            stopSelf();
        }
    }

    private void startTranscriptionLoop() {
        transcriptionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    transcribeCurrentSegment();
                    transcriptionHandler.postDelayed(this, TRANSCRIPTION_INTERVAL);
                }
            }
        }, TRANSCRIPTION_INTERVAL);
    }

    private void transcribeCurrentSegment() {
        try {
            // Stop and save current recording
            mediaRecorder.stop();
            mediaRecorder.reset();

            // Send the file for transcription
            File audioFile = new File(currentRecordingFile);
            RequestBody fileBody = RequestBody.create(MediaType.parse("audio/wav"), audioFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", audioFile.getName(), fileBody);
            RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), VIETTEL_TOKEN);

            Call<ViettelSpeechToTextResponse> call = viettelAsrApi.convertSpeechToText(filePart, tokenBody);
            call.enqueue(new Callback<ViettelSpeechToTextResponse>() {
                @Override
                public void onResponse(Call<ViettelSpeechToTextResponse> call, Response<ViettelSpeechToTextResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ViettelSpeechToTextResponse sttResponse = response.body();
                        if (sttResponse.getResponse() != null && 
                            sttResponse.getResponse().getResult() != null && 
                            !sttResponse.getResponse().getResult().isEmpty()) {
                            
                            String transcript = sttResponse.getResponse().getResult().get(0).getTranscript();
                            if (transcript != null && !transcript.trim().isEmpty()) {
                                updateFloatingWindow(transcript);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<ViettelSpeechToTextResponse> call, Throwable t) {
                    Log.e(TAG, "Transcription failed", t);
                }
            });

            // Start new recording segment
            startNewRecordingSegment();

        } catch (Exception e) {
            Log.e(TAG, "Error during transcription", e);
            // Try to recover by starting a new recording segment
            try {
                startNewRecordingSegment();
            } catch (Exception ex) {
                Log.e(TAG, "Could not recover from error", ex);
                stopSelf();
            }
        }
    }

    private void startNewRecordingSegment() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentRecordingFile = new File(recordingDir, "call_" + timestamp + ".wav").getAbsolutePath();

            mediaRecorder.setOutputFile(currentRecordingFile);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting new recording segment", e);
            stopSelf();
        }
    }

    private void updateFloatingWindow(String text) {
        Intent updateIntent = new Intent(this, FloatingWindowService.class);
        updateIntent.putExtra("transcription", text);
        startService(updateIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRecording = false;
        
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing mediaRecorder", e);
            }
            mediaRecorder = null;
        }
        
        if (transcriptionHandler != null) {
            transcriptionHandler.removeCallbacksAndMessages(null);
        }
        
        if (floatingWindowIntent != null) {
            stopService(floatingWindowIntent);
        }
    }
} 