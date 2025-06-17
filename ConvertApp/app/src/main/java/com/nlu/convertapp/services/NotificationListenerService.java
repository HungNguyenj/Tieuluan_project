package com.nlu.convertapp.services;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.gson.Gson;
import com.nlu.convertapp.api.ApiKeys;
import com.nlu.convertapp.api.ViettelAiApi;
import com.nlu.convertapp.models.ViettelTtsRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    
    private static final String TAG = "NotificationListener";
    public static final String ACTION_NOTIFICATION_LISTENER = "com.nlu.convertapp.NOTIFICATION_LISTENER";
    
    // Viettel TTS constants
    private static final String VIETTEL_BASE_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;
    private static final String VIETTEL_VOICE = ApiKeys.VIETTEL_VOICE;
    private static final float VIETTEL_SPEED = 1.0f;
    private static final int VIETTEL_RETURN_OPTION = 3;
    private static final boolean VIETTEL_WITHOUT_FILTER = false;
    
    private ViettelAiApi viettelAiApi;
    private MediaPlayer mediaPlayer;
    private File cacheDir;
    
    // List of bank and payment app packages to monitor
    private final Map<String, String> monitoredPackages = new HashMap<>();
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Notification Listener Service created");
        
        // Initialize the packages to monitor
        monitoredPackages.put("com.vietcombank.vcbmobile", "Vietcombank");
        monitoredPackages.put("vn.com.bidv.smartbanking", "BIDV");
        monitoredPackages.put("vn.tpb.mb.gprsandroid", "TPBank");
        monitoredPackages.put("com.techcombank.mobileone", "Techcombank");
        monitoredPackages.put("com.VPB", "VPBank");
        monitoredPackages.put("com.vnpay.hdbank", "HDBank");
        monitoredPackages.put("com.mbmobile", "MBBank");
        monitoredPackages.put("com.vnpay.agribank", "Agribank");
        monitoredPackages.put("com.mservice.momotransfer", "Momo");
        monitoredPackages.put("com.samsung.android.messaging", "SMS");
        monitoredPackages.put("com.android.messaging", "SMS");
        monitoredPackages.put("com.google.android.apps.messaging", "SMS");
        
        // Initialize Viettel TTS API
        setupViettelApi();
        
        // Initialize cache directory
        cacheDir = new File(getCacheDir(), "tts_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // Initialize MediaPlayer
        mediaPlayer = new MediaPlayer();
        
        Log.d(TAG, "Monitoring packages: " + monitoredPackages.keySet());
    }
    
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "Notification Listener connected");
    }
    
    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "Notification Listener disconnected");
        // Try to reconnect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(new ComponentName(this, NotificationListenerService.class));
        }
    }
    
    private void setupViettelApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(VIETTEL_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        viettelAiApi = retrofit.create(ViettelAiApi.class);
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Log.d(TAG, "Notification received from package: " + packageName);
        
        // For debugging, log all notifications
        Notification notification = sbn.getNotification();
        if (notification != null && notification.extras != null) {
            String title = notification.extras.getString(Notification.EXTRA_TITLE);
            String text = notification.extras.getString(Notification.EXTRA_TEXT);
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Text: " + text);
        }
        
        // Check if this is a notification from a monitored app
        if (monitoredPackages.containsKey(packageName)) {
            try {
                String bankName = monitoredPackages.get(packageName);
                Log.d(TAG, "Processing notification from: " + bankName);
                
                if (notification.extras != null) {
                    String title = notification.extras.getString(Notification.EXTRA_TITLE);
                    String text = notification.extras.getString(Notification.EXTRA_TEXT);
                    
                    if (title != null && text != null) {
                        // Format the notification message
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                        String timestamp = sdf.format(new Date());
                        String formattedMessage = String.format("[%s] %s - %s: %s", timestamp, bankName, title, text);
                        
                        Log.d(TAG, "Broadcasting message: " + formattedMessage);
                        
                        // Broadcast the message
                        Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER);
                        intent.putExtra("message", formattedMessage);
                        sendBroadcast(intent);
                        
                        // Process for speech
                        processAndSpeakNotification(bankName, title, text);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing notification", e);
            }
        }
    }
    
    private void processAndSpeakNotification(String bankName, String title, String text) {
        String messageToSpeak = String.format("Thông báo từ %s. %s. %s", bankName, title, text);
        
        // Create Viettel TTS request
        ViettelTtsRequest ttsRequest = new ViettelTtsRequest(
            messageToSpeak,
            VIETTEL_VOICE,
            VIETTEL_SPEED,
            VIETTEL_RETURN_OPTION,
            VIETTEL_TOKEN,
            VIETTEL_WITHOUT_FILTER
        );
        
        String jsonBody = new Gson().toJson(ttsRequest);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
        
        // Make API call
        Call<ResponseBody> call = viettelAiApi.convertTextToSpeech(requestBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Save audio to a temporary file
                        File audioFile = new File(cacheDir, "notification_" + System.currentTimeMillis() + ".mp3");
                        saveAndPlayAudio(response.body(), audioFile);
                    } catch (IOException e) {
                        Log.e(TAG, "Error saving audio file", e);
                    }
                } else {
                    Log.e(TAG, "TTS API error: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "TTS API call failed", t);
            }
        });
    }
    
    private void saveAndPlayAudio(ResponseBody body, File audioFile) throws IOException {
        try (InputStream inputStream = body.byteStream();
             FileOutputStream outputStream = new FileOutputStream(audioFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            
            // Play the audio
            playAudio(audioFile);
        }
    }
    
    private void playAudio(File audioFile) {
        try {
            // Reset MediaPlayer
            mediaPlayer.reset();
            
            // Set the audio file as data source
            mediaPlayer.setDataSource(audioFile.getPath());
            
            // Prepare and start playback
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            // Delete the file after playback
            mediaPlayer.setOnCompletionListener(mp -> {
                audioFile.delete();
            });
        } catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
        }
    }
    
    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
} 