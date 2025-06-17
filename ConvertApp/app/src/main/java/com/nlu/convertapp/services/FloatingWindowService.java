package com.nlu.convertapp.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.nlu.convertapp.R;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private TextView transcriptionText;
    private WindowManager.LayoutParams params;
    private static final String ACTION_UPDATE_TEXT = "com.nlu.convertapp.action.UPDATE_TEXT";
    private static final String EXTRA_TEXT = "com.nlu.convertapp.extra.TEXT";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize WindowManager
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Inflate the floating view layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null);
        
        // Get reference to the transcription TextView
        transcriptionText = floatingView.findViewById(R.id.transcriptionText);
        
        // Set up window parameters
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        
        // Position the window at the top of the screen
        params.gravity = Gravity.TOP;
        
        // Add the view to the window
        windowManager.addView(floatingView, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_UPDATE_TEXT.equals(action)) {
                String text = intent.getStringExtra(EXTRA_TEXT);
                updateTranscription(text);
            }
        }
        return START_STICKY;
    }

    public void updateTranscription(String text) {
        if (transcriptionText != null && text != null) {
            transcriptionText.setText(text);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Helper method to create an intent for updating text
    public static Intent createUpdateTextIntent(String text) {
        Intent intent = new Intent(ACTION_UPDATE_TEXT);
        intent.putExtra(EXTRA_TEXT, text);
        return intent;
    }
} 