package com.nlu.convertapp.activities;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.nlu.convertapp.R;
import com.nlu.convertapp.api.ElevenLabsApi;
import com.nlu.convertapp.models.TextToSpeechRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class TextToSpeechActivity extends AppCompatActivity {

    private static final String API_KEY = "sk_2bb0829640a3f35d2a97f863e7b5534fa1a13b1dd9039ebf"; // Replace with your actual API key
    private static final String BASE_URL = "https://api.elevenlabs.io/";
    private static final String VOICE_ID = "JBFqnCBsd6RMkjVDRZzb";
    private static final String OUTPUT_FORMAT = "mp3_44100_128";
    private static final String MODEL_ID = "eleven_multilingual_v2";

    Toolbar toolbar;
    private Spinner languageSpinner;
    private EditText textArea;

    // Bottom buttons
    private ImageButton fileButton;
    private ImageButton copyButton;
    private ImageButton starButton;
    private MaterialButton convertButton;

    // Audio player controls
    private ImageButton playPauseButton;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar audioSeekBar;

    private MediaPlayer mediaPlayer;
    private File audioFile;
    private boolean isPlaying = false;
    private ElevenLabsApi elevenLabsApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_text_to_speech);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRetrofit();

        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kết thúc activity hiện tại để quay về MainActivity
                finish();
            }
        });

        // Xử lý sự kiện cho các nút
        setupButtonListeners();

        // Xử lý sự kiện cho audio seekbar
        setupAudioSeekBar();
    }

    private void setupRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        elevenLabsApi = retrofit.create(ElevenLabsApi.class);
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Language spinner
        languageSpinner = findViewById(R.id.languageSpinner);

        // Text area
        textArea = findViewById(R.id.textArea);

        // Bottom buttons
        fileButton = findViewById(R.id.fileButton);
        copyButton = findViewById(R.id.copyButton);
        starButton = findViewById(R.id.starButton);
        convertButton = findViewById(R.id.convertButton);

        // Audio player controls
        playPauseButton = findViewById(R.id.playPauseButton);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        audioSeekBar = findViewById(R.id.audioSeekBar);
    }

    private void setupButtonListeners() {
        // File button click listener
        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện upload file
            }
        });

        // Copy button click listener
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện copy text
            }
        });

        // Star button click listener
        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện đánh dấu yêu thích
            }
        });

        // Convert button click listener
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertTextToSpeech();
            }
        });

        // Play/Pause button click listener
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });
    }

    private void convertTextToSpeech() {
        String text = textArea.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter text to convert", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading message
        Toast.makeText(this, "Converting text to speech...", Toast.LENGTH_SHORT).show();
        
        // Create request body
        TextToSpeechRequest requestData = new TextToSpeechRequest(text, MODEL_ID);
        String jsonBody = new Gson().toJson(requestData);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), jsonBody);

        // Make API call
        Call<ResponseBody> call = elevenLabsApi.convertTextToSpeech(
                VOICE_ID, 
                OUTPUT_FORMAT, 
                API_KEY, 
                requestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Save audio to a temporary file
                        saveAndPlayAudio(response.body().byteStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(TextToSpeechActivity.this, 
                                "Error saving audio: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TextToSpeechActivity.this, 
                            "Error: " + response.code() + " " + response.message(), 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(TextToSpeechActivity.this, 
                        "Network error: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
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
            
            // Update total duration
            int duration = mediaPlayer.getDuration();
            totalTime.setText(formatTime(duration));
            
            // Set up seek bar
            audioSeekBar.setMax(duration);
            
            // Start playing
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            
            // Update progress
            updateProgressRunnable.run();
            
            // Set up completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                audioSeekBar.setProgress(0);
                currentTime.setText("00:00");
                // Reset the player so it can be played again
                mediaPlayer.seekTo(0);
                mediaPlayer.pause();
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                audioSeekBar.setProgress(currentPosition);
                currentTime.setText(formatTime(currentPosition));
                audioSeekBar.postDelayed(this, 100);
            }
        }
    };

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            return;
        }

        if (isPlaying) {
            mediaPlayer.pause();
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        } else {
            // Ensure we can play from any state
            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            updateProgressRunnable.run();
        }
        isPlaying = !isPlaying;
    }

    private void setupAudioSeekBar() {
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Remove callbacks to prevent position conflicts
                audioSeekBar.removeCallbacks(updateProgressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && isPlaying) {
                    audioSeekBar.post(updateProgressRunnable);
                }
            }
        });
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (audioFile != null && audioFile.exists()) {
            audioFile.delete();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }
}