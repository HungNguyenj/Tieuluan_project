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
                "NGUYEN VAN A", 
                "1023456789", 
                2000000, 
                "Chuyen tien hoc phi thang 5", 
                new Date(System.currentTimeMillis() - 3600000), 
                "Vietcombank"));
        
        sampleTransactions.add(new TransactionMessage(
                "TRAN THI B", 
                "0987654321", 
                1500000, 
                "Chuyen khoan tien mua hang", 
                new Date(System.currentTimeMillis() - 86400000), 
                "BIDV"));
        
        sampleTransactions.add(new TransactionMessage(
                "LE VAN C", 
                "5432167890", 
                3500000, 
                "Thanh toan dich vu", 
                new Date(System.currentTimeMillis() - 172800000), 
                "Techcombank"));
        
        sampleTransactions.add(new TransactionMessage(
                "PHAM THI D", 
                "6781234509", 
                500000, 
                "Chuyen tien an trua", 
                new Date(System.currentTimeMillis() - 259200000), 
                "VPBank"));
        
        sampleTransactions.add(new TransactionMessage(
                "HOANG VAN E", 
                "8901234567", 
                10000000, 
                "Thanh toan hop dong thang 5", 
                new Date(System.currentTimeMillis() - 345600000), 
                "MBBank"));
        
        // Update the adapter with sample data
        transactionAdapter.updateData(sampleTransactions);
    }
} 