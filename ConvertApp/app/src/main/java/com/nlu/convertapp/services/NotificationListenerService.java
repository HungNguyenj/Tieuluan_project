package com.nlu.convertapp.services;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    
    private static final String TAG = "NotificationListener";
    private TextToSpeech textToSpeech;
    private boolean isTTSInitialized = false;
    
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
        
        // Initialize Text-to-Speech
        initializeTextToSpeech();
    }
    
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set Vietnamese language
                int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Vietnamese language not supported");
                } else {
                    isTTSInitialized = true;
                    Log.d(TAG, "TTS initialized successfully");
                }
            } else {
                Log.e(TAG, "Failed to initialize TTS");
            }
        });
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        
        // Check if this is a notification from a monitored bank or payment app
        if (monitoredPackages.containsKey(packageName)) {
            try {
                Notification notification = sbn.getNotification();
                String bankName = monitoredPackages.get(packageName);
                
                if (notification.extras != null) {
                    String title = notification.extras.getString(Notification.EXTRA_TITLE);
                    String text = notification.extras.getString(Notification.EXTRA_TEXT);
                    
                    if (title != null && text != null) {
                        // Process the banking notification
                        processAndSpeakNotification(bankName, title, text);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing notification", e);
            }
        }
    }
    
    private void processAndSpeakNotification(String bankName, String title, String text) {
        Log.d(TAG, "Processing notification from " + bankName);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Text: " + text);
        
        String messageToSpeak = formatMessageForSpeech(bankName, title, text);
        speakMessage(messageToSpeak);
        
        // Also save the transaction for later viewing
        saveTransaction(bankName, title, text);
    }
    
    private String formatMessageForSpeech(String bankName, String title, String text) {
        // Format the notification into a more speech-friendly format
        // This could be customized per bank since each bank sends different notification formats
        
        StringBuilder speechText = new StringBuilder();
        speechText.append("Thông báo từ ").append(bankName).append(". ");
        
        // Extract amount if available
        String amount = extractAmount(text);
        if (amount != null && !amount.isEmpty()) {
            speechText.append("Số tiền ").append(amount).append(" đồng. ");
        }
        
        // Add the rest of the notification text
        speechText.append(text);
        
        return speechText.toString();
    }
    
    private String extractAmount(String text) {
        // Simple pattern to extract numerical amounts from text
        // This might need to be adjusted based on actual formats from banks
        Pattern pattern = Pattern.compile("\\+?([0-9.,]+)\\s*VND|([0-9.,]+)\\s*đồng");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String amount = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // Remove commas and dots (used as thousand separators in Vietnamese)
            return amount.replaceAll("[.,]", "");
        }
        
        return null;
    }
    
    private void speakMessage(String message) {
        if (isTTSInitialized) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "bank_notification");
            Log.d(TAG, "Speaking: " + message);
        } else {
            Log.e(TAG, "TTS not initialized, cannot speak message");
        }
    }
    
    private void saveTransaction(String bankName, String title, String text) {
        // TODO: Parse notification and save to a database or shared preferences
        // This is a placeholder for future implementation
    }
    
    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
} 