package com.nlu.convertapp.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nlu.convertapp.R;
import com.nlu.convertapp.services.NotificationListenerService;

public class BankTransactionActivity extends AppCompatActivity {

    private Spinner bankSpinner;
    private EditText accountNumberEditText;
    private Button confirmButton;
    private Button viewTransactionsButton;
    private Button enableNotificationsButton;

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
        bankSpinner = findViewById(R.id.bankSpinner);
        accountNumberEditText = findViewById(R.id.accountNumberEditText);
        confirmButton = findViewById(R.id.confirmButton);
        viewTransactionsButton = findViewById(R.id.viewTransactionsButton);
        enableNotificationsButton = findViewById(R.id.enableNotificationsButton);

        // Set up bank spinner with sample banks
        setupBankSpinner();

        // Set up confirm button click listener
        confirmButton.setOnClickListener(v -> {
            String selectedBank = bankSpinner.getSelectedItem().toString();
            String accountNumber = accountNumberEditText.getText().toString();

            if (accountNumber.isEmpty()) {
                Toast.makeText(this, "Please enter account number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save bank and account details
            Toast.makeText(this, "Reading transaction for " + selectedBank + 
                    " account: " + accountNumber, Toast.LENGTH_LONG).show();
        });
        
        // Set up view transactions button click listener
        viewTransactionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(BankTransactionActivity.this, TransactionListActivity.class);
            startActivity(intent);
        });
        
        // Set up enable notifications button click listener
        enableNotificationsButton.setOnClickListener(v -> {
            if (!isNotificationServiceEnabled()) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Please enable notification access for the app", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Notification access already enabled", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean isNotificationServiceEnabled() {
        String enabledNotificationListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        
        if (!TextUtils.isEmpty(enabledNotificationListeners)) {
            ComponentName componentName = new ComponentName(this, NotificationListenerService.class);
            String flatComponentName = componentName.flattenToString();
            return enabledNotificationListeners.contains(flatComponentName);
        }
        
        return false;
    }

    private void setupBankSpinner() {
        // Sample list of banks
        String[] banks = {
                "Vietcombank", 
                "BIDV", 
                "Techcombank", 
                "VPBank", 
                "Agribank", 
                "MBBank", 
                "TPBank",
                "Momo"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, 
                android.R.layout.simple_spinner_item, 
                banks
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankSpinner.setAdapter(adapter);
    }
} 