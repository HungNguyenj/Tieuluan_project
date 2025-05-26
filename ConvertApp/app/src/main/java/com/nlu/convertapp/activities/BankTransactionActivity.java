package com.nlu.convertapp.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.nlu.convertapp.R;
import com.nlu.convertapp.api.ApiKeys;
import com.nlu.convertapp.api.SepayApi;
import com.nlu.convertapp.api.ViettelAiApi;
import com.nlu.convertapp.adapters.TransactionAdapter;
import com.nlu.convertapp.models.SepayResponse;
import com.nlu.convertapp.models.SepayTransaction;
import com.nlu.convertapp.models.TransactionMessage;
import com.nlu.convertapp.models.ViettelTtsRequest;
import com.nlu.convertapp.services.NotificationListenerService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

public class BankTransactionActivity extends AppCompatActivity {

    private static final String TAG = "BankTransaction";
    private static final String SEPAY_BASE_URL = "https://my.sepay.vn/";
    private static final long POLLING_INTERVAL = 3000; // Poll every 3 seconds

    // Add Viettel AI TTS constants
    private static final String VIETTEL_BASE_URL = "https://viettelai.vn/";
    private static final String VIETTEL_TOKEN = ApiKeys.VIETTEL_TOKEN;
    private static final String VIETTEL_VOICE = ApiKeys.VIETTEL_VOICE;
    private static final float VIETTEL_SPEED = 1.0f;
    private static final int VIETTEL_RETURN_OPTION = 3;
    private static final boolean VIETTEL_WITHOUT_FILTER = false;

    private Spinner bankSpinner;
    private EditText accountNumberEditText;
    private Button confirmButton;
    private Button enableNotificationsButton;
    private RecyclerView transactionRecyclerView;
    private TransactionAdapter transactionAdapter;
    private List<TransactionMessage> transactionList;

    private SepayApi sepayApi;
    private Handler pollingHandler;
    private ViettelAiApi viettelAiApi;
    private MediaPlayer mediaPlayer;
    private File audioFile;
    private String lastTransactionId;
    private boolean isPolling = false;

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPolling) {
                fetchTransactions();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        }
    };

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
        setupBankSpinner();
        setupRecyclerView();
        setupSepayApi();
        setupViettelAiApi();
        setupButtonListeners();

        // Initialize polling handler
        pollingHandler = new Handler(Looper.getMainLooper());
    }

    private void initializeViews() {
        bankSpinner = findViewById(R.id.bankSpinner);
        accountNumberEditText = findViewById(R.id.accountNumberEditText);
        confirmButton = findViewById(R.id.confirmButton);
        enableNotificationsButton = findViewById(R.id.enableNotificationsButton);
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionRecyclerView.setAdapter(transactionAdapter);
    }

    private void setupBankSpinner() {
        String[] banks = {
            "Vietcombank", "BIDV", "Techcombank", "VPBank", 
            "Agribank", "MBBank", "TPBank", "Momo"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            banks
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankSpinner.setAdapter(adapter);
    }

    private void setupSepayApi() {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(SEPAY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        sepayApi = retrofit.create(SepayApi.class);
    }

    private void setupViettelAiApi() {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

        Retrofit viettelRetrofit = new Retrofit.Builder()
            .baseUrl(VIETTEL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        viettelAiApi = viettelRetrofit.create(ViettelAiApi.class);
    }

    private void setupButtonListeners() {
        confirmButton.setOnClickListener(v -> {
            String selectedBank = bankSpinner.getSelectedItem().toString();
            String accountNumber = accountNumberEditText.getText().toString();

            if (accountNumber.isEmpty()) {
                Toast.makeText(this, "Please enter account number", 
                        Toast.LENGTH_SHORT).show();
                return;
            }

            startPolling();
            Toast.makeText(this, "Monitoring transactions for " + selectedBank + 
                    " account: " + accountNumber, Toast.LENGTH_LONG).show();
        });

        enableNotificationsButton.setOnClickListener(v -> {
            if (!isNotificationServiceEnabled()) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Please enable notification access for the app", 
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Notification access already enabled", 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPolling() {
        if (!isPolling) {
            isPolling = true;
            lastTransactionId = null;
            pollingHandler.post(pollingRunnable);
        }
    }

    private void stopPolling() {
        isPolling = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void fetchTransactions() {
        sepayApi.getTransactions("Bearer " + ApiKeys.SEPAY_TOKEN)
            .enqueue(new Callback<SepayResponse>() {
                @Override
                public void onResponse(Call<SepayResponse> call, Response<SepayResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SepayResponse sepayResponse = response.body();
                        if (sepayResponse.getStatus() == 200 && 
                            sepayResponse.getMessages().isSuccess() && 
                            sepayResponse.getTransactions() != null && 
                            !sepayResponse.getTransactions().isEmpty()) {
                            
                            // Get the most recent transaction
                            SepayTransaction latestTransaction = sepayResponse.getTransactions().get(0);
                            
                            // Check if this is a new transaction
                            if (lastTransactionId == null) {
                                lastTransactionId = latestTransaction.getId();
                                addTransactionToList(latestTransaction);
                            } else if (!lastTransactionId.equals(latestTransaction.getId())) {
                                // New transaction found
                                addTransactionToList(latestTransaction);
                                announceTransaction(latestTransaction);
                                lastTransactionId = latestTransaction.getId();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<SepayResponse> call, Throwable t) {
                    Log.e(TAG, "Error fetching transactions", t);
                }
            });
    }

    private void addTransactionToList(SepayTransaction transaction) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date transactionDate = inputFormat.parse(transaction.getTransactionDate());
            
            TransactionMessage message = new TransactionMessage(
                extractSenderName(transaction.getTransactionContent()),
                transaction.getAccountNumber(),
                Double.parseDouble(transaction.getAmountIn()),
                transaction.getTransactionContent(),
                transactionDate,
                transaction.getBankBrandName()
            );
            
            transactionList.add(0, message); // Add to the beginning of the list
            transactionAdapter.notifyItemInserted(0);
            transactionRecyclerView.smoothScrollToPosition(0);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing transaction date", e);
        }
    }

    private String extractSenderName(String content) {
        // Extract sender name from transaction content
        // This is a simple implementation, you might want to improve it
        if (content != null && content.contains("chuyen tien")) {
            return content.split("chuyen tien")[0].trim();
        }
        return "Unknown Sender";
    }

    private void announceTransaction(SepayTransaction transaction) {
        String amount = transaction.getAmountIn();
        String message = String.format("Đã nhận %s đồng", amount);
        
        // Create Viettel AI request body
        ViettelTtsRequest requestData = new ViettelTtsRequest(
            message, 
            VIETTEL_VOICE,
            VIETTEL_SPEED,
            VIETTEL_RETURN_OPTION,
            VIETTEL_TOKEN,
            VIETTEL_WITHOUT_FILTER
        );
        
        String jsonBody = new Gson().toJson(requestData);
        RequestBody requestBody = RequestBody.create(
            MediaType.parse("application/json"), jsonBody);

        // Make API call
        Call<ResponseBody> call = viettelAiApi.convertTextToSpeech(requestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Save and play the audio
                        saveAndPlayAudio(response.body().byteStream());
                    } catch (IOException e) {
                        Log.e(TAG, "Error saving audio", e);
                    }
                } else {
                    Log.e(TAG, "Error from Viettel AI TTS: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network error from Viettel AI TTS", t);
            }
        });
    }

    private void saveAndPlayAudio(InputStream inputStream) throws IOException {
        // Clean up any existing audio
        releaseMediaPlayer();

        // Create a temporary file to store the audio
        audioFile = File.createTempFile("tts_audio", ".mp3", getCacheDir());
        
        // Write the input stream to the file
        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
        }

        // Play the audio
        playAudio();
    }

    private void playAudio() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            // Set up completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                releaseMediaPlayer();
            });
            
        } catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private boolean isNotificationServiceEnabled() {
        String enabledNotificationListeners = Settings.Secure.getString(
            getContentResolver(), 
            "enabled_notification_listeners"
        );
        
        if (!TextUtils.isEmpty(enabledNotificationListeners)) {
            ComponentName componentName = new ComponentName(
                this, 
                NotificationListenerService.class
            );
            String flatComponentName = componentName.flattenToString();
            return enabledNotificationListeners.contains(flatComponentName);
        }
        
        return false;
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        releaseMediaPlayer();
        if (audioFile != null && audioFile.exists()) {
            audioFile.delete();
        }
        super.onDestroy();
    }
} 