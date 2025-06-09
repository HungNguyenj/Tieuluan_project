package com.nlu.convertapp.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nlu.convertapp.R;
import com.nlu.convertapp.adapters.TransactionAdapter;
import com.nlu.convertapp.models.TransactionMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionListActivity extends AppCompatActivity {

    private RecyclerView transactionRecyclerView;
    private TransactionAdapter transactionAdapter;
    private List<TransactionMessage> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
        
        // Set up RecyclerView
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionRecyclerView.setAdapter(transactionAdapter);
        
        // Load sample transaction data
        loadSampleTransactions();
    }
    
    private void loadSampleTransactions() {
        // Sample transaction data for demonstration
        List<TransactionMessage> sampleTransactions = new ArrayList<>();
        
        sampleTransactions.add(new TransactionMessage(
                "NGUYEN VAN A"));

        
        // Update the adapter with sample data
        transactionAdapter.updateData(sampleTransactions);
    }
} 