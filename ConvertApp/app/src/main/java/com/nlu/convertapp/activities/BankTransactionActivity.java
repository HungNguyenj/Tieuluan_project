package com.nlu.convertapp.activities;

import android.content.Intent;
import android.os.Bundle;
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

public class BankTransactionActivity extends AppCompatActivity {

    private Spinner bankSpinner;
    private EditText accountNumberEditText;
    private Button confirmButton;
    private Button viewTransactionsButton;

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

            // TODO: Implement bank transaction reading logic
            Toast.makeText(this, "Reading transaction for " + selectedBank + 
                    " account: " + accountNumber, Toast.LENGTH_LONG).show();
        });
        
        // Set up view transactions button click listener
        viewTransactionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(BankTransactionActivity.this, TransactionListActivity.class);
            startActivity(intent);
        });
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
                "TPBank"
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