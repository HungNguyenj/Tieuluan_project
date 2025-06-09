package com.nlu.convertapp.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.nlu.convertapp.R;
import com.nlu.convertapp.adapters.TransactionAdapter;
import com.nlu.convertapp.models.TransactionMessage;
import com.nlu.convertapp.services.NotificationListenerService;

import java.util.ArrayList;
import java.util.List;

public class BankTransactionActivity extends AppCompatActivity {
    private static final String TAG = "BankTransaction";
    public static final String ACTION_NOTIFICATION_LISTENER = "com.nlu.convertapp.NOTIFICATION_LISTENER";
    private static final long PERMISSION_CHECK_INTERVAL = 5000; // 5 seconds

    private RecyclerView transactionRecyclerView;
    private TransactionAdapter transactionAdapter;
    private List<TransactionMessage> transactionList;
    private NotificationReceiver notificationReceiver;
    private MaterialButton permissionStatusButton;
    private Handler permissionCheckHandler;
    private boolean isPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bank_transaction);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeViews();
        setupRecyclerView();
        setupPermissionCheck();
        registerNotificationReceiver();
        
        // Add some sample notifications for testing
        addSampleNotifications();
        
        // Check permission immediately
        checkNotificationAccess();
    }

    private void initializeViews() {
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
        permissionStatusButton = findViewById(R.id.permissionStatusButton);
        
        permissionStatusButton.setOnClickListener(v -> {
            if (!isPermissionGranted) {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
            } else {
                // If permission is granted, try restarting the service
                toggleNotificationListenerService();
            }
        });
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionRecyclerView.setAdapter(transactionAdapter);
    }

    private void setupPermissionCheck() {
        permissionCheckHandler = new Handler(Looper.getMainLooper());
        startPermissionCheck();
    }

    private void startPermissionCheck() {
        permissionCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkNotificationAccess();
                permissionCheckHandler.postDelayed(this, PERMISSION_CHECK_INTERVAL);
            }
        }, PERMISSION_CHECK_INTERVAL);
    }

    private void checkNotificationAccess() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean hasAccess = enabledListeners != null && enabledListeners.contains(getPackageName());
        
        if (hasAccess != isPermissionGranted) {
            isPermissionGranted = hasAccess;
            updatePermissionStatus();
            
            if (isPermissionGranted) {
                // Permission was just granted, restart the service
                toggleNotificationListenerService();
            }
        }
    }

    private void toggleNotificationListenerService() {
        Log.d(TAG, "Toggling NotificationListenerService");
        ComponentName thisComponent = new ComponentName(this, NotificationListenerService.class);
        PackageManager pm = getPackageManager();
        
        pm.setComponentEnabledSetting(thisComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        
        pm.setComponentEnabledSetting(thisComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        
        Toast.makeText(this, "Restarting notification service...", Toast.LENGTH_SHORT).show();
    }

    private void updatePermissionStatus() {
        if (isPermissionGranted) {
            permissionStatusButton.setText("Notification Access Granted (Tap to Restart)");
            permissionStatusButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
        } else {
            permissionStatusButton.setText("Enable Notification Access");
            permissionStatusButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark));
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerNotificationReceiver() {
        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFICATION_LISTENER);
        registerReceiver(notificationReceiver, filter);
    }

    private void addSampleNotifications() {
        // Add some sample notifications
        String[] sampleMessages = {
            "[12:30:45] Vietcombank - Thông báo: Tài khoản của bạn vừa nhận được 1,000,000 VND",
            "[12:35:20] MBBank - Biến động số dư: +500,000 VND từ nguồn NGUYEN VAN A",
            "[12:40:15] Momo - Giao dịch: Bạn vừa nhận được 200,000 VND từ LÊ THỊ B"
        };

        for (String message : sampleMessages) {
            TransactionMessage transaction = new TransactionMessage(message);
            transactionAdapter.addTransaction(transaction);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            unregisterReceiver(notificationReceiver);
        }
        if (permissionCheckHandler != null) {
            permissionCheckHandler.removeCallbacksAndMessages(null);
        }
    }

    private class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null && !message.isEmpty()) {
                Log.d(TAG, "Received notification message: " + message);
                TransactionMessage transaction = new TransactionMessage(message);
                transactionAdapter.addTransaction(transaction);
                transactionRecyclerView.smoothScrollToPosition(0);
            }
        }
    }
} 