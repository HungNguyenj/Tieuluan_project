package com.nlu.convertapp.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.nlu.convertapp.R;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private TextView transcriptionText;
    private WindowManager.LayoutParams params;
    private StringBuilder transcriptionHistory;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        transcriptionHistory = new StringBuilder();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Initialize the floating window layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null);
        transcriptionText = floatingView.findViewById(R.id.transcriptionText);
        
        // Set window parameters
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        
        // Initial position
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        
        // Add touch listener for dragging
        setupTouchListener();
        
        // Add the view to the window
        windowManager.addView(floatingView, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("transcription")) {
            String newText = intent.getStringExtra("transcription");
            updateTranscription(newText);
        }
        return START_STICKY;
    }

    private void setupTouchListener() {
        final float[] initialX = new float[1];
        final float[] initialY = new float[1];
        final float[] initialTouchX = new float[1];
        final float[] initialTouchY = new float[1];

        floatingView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX[0] = params.x;
                    initialY[0] = params.y;
                    initialTouchX[0] = event.getRawX();
                    initialTouchY[0] = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (initialX[0] + (event.getRawX() - initialTouchX[0]));
                    params.y = (int) (initialY[0] + (event.getRawY() - initialTouchY[0]));
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });
    }

    private void updateTranscription(String newText) {
        if (transcriptionText != null && newText != null && !newText.trim().isEmpty()) {
            // Append new text with timestamp
            String timestamp = android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()).toString();
            transcriptionHistory.append("[").append(timestamp).append("] ").append(newText).append("\n\n");
            
            // Update the TextView
            transcriptionText.setText(transcriptionHistory.toString());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }
} 