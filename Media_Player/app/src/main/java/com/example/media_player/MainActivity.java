package com.example.media_player;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 1;

    private VideoView videoView;
    private MediaPlayer audioPlayer;
    private MaterialTextView statusText;
    private MaterialTextView fileNameText;
    private MaterialButton openFileButton;
    private MaterialButton openUrlButton;
    private MaterialButton playButton;
    private MaterialButton pauseButton;
    private MaterialButton stopButton;
    private MaterialButton restartButton;
    private TextInputLayout urlInputLayout;
    private TextInputEditText urlEditText;
    private LinearProgressIndicator progressBar;

    private Uri currentMediaUri;
    private boolean isAudioMode = false;
    private boolean isPrepared = false;
    private boolean isUrlInputVisible = false;

    private final ActivityResultLauncher<Intent> audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        loadAudioFile(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupListeners();
        resetMediaPlayer();
    }

    private void initializeViews() {
        videoView = findViewById(R.id.videoView);
        statusText = findViewById(R.id.statusText);
        fileNameText = findViewById(R.id.fileNameText);
        openFileButton = findViewById(R.id.openFileButton);
        openUrlButton = findViewById(R.id.openUrlButton);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);
        restartButton = findViewById(R.id.restartButton);
        urlInputLayout = findViewById(R.id.urlInputLayout);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
    }

    private void setupListeners() {
        openFileButton.setOnClickListener(v -> openFilePicker());
        openUrlButton.setOnClickListener(v -> toggleUrlInput());
        playButton.setOnClickListener(v -> playMedia());
        pauseButton.setOnClickListener(v -> pauseMedia());
        stopButton.setOnClickListener(v -> stopMedia());
        restartButton.setOnClickListener(v -> restartMedia());

        videoView.setOnPreparedListener(mp -> {
            isPrepared = true;
            progressBar.setVisibility(View.GONE);
            updateStatus(getString(R.string.ready));
        });

        videoView.setOnCompletionListener(mp -> {
            updateStatus(getString(R.string.completed));
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            showError("Video playback error: " + what);
            return true;
        });
    }

    private void toggleUrlInput() {
        isUrlInputVisible = !isUrlInputVisible;
        if (isUrlInputVisible) {
            urlInputLayout.setVisibility(View.VISIBLE);
            urlInputLayout.setEndIconOnClickListener(v -> loadVideoFromUrl());
        } else {
            urlInputLayout.setVisibility(View.GONE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
        audioPickerLauncher.launch(intent);
    }

    private void loadAudioFile(Uri uri) {
        resetMediaPlayer();
        currentMediaUri = uri;
        isAudioMode = true;

        String fileName = getFileNameFromUri(uri);
        fileNameText.setText(fileName);

        videoView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            audioPlayer.setDataSource(this, uri);
            audioPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                progressBar.setVisibility(View.GONE);
                updateStatus(getString(R.string.ready));
                playMedia();
            });
            audioPlayer.setOnCompletionListener(mp -> updateStatus(getString(R.string.completed)));
            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                progressBar.setVisibility(View.GONE);
                showError("Audio playback error: " + what);
                return true;
            });
            audioPlayer.prepareAsync();
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            showError("Failed to load audio: " + e.getMessage());
        }
    }

    private void loadVideoFromUrl() {
        String url = urlEditText.getText() != null ? urlEditText.getText().toString().trim() : "";
        if (url.isEmpty()) {
            urlEditText.setError("Please enter a valid URL");
            return;
        }

        resetMediaPlayer();
        currentMediaUri = Uri.parse(url);
        isAudioMode = false;

        videoView.setVisibility(View.VISIBLE);
        fileNameText.setText(getString(R.string.streaming_from_url));
        progressBar.setVisibility(View.VISIBLE);

        videoView.setOnPreparedListener(mp -> {
            isPrepared = true;
            progressBar.setVisibility(View.GONE);
            updateStatus(getString(R.string.playing));
            mp.start();
        });

        videoView.setVideoURI(currentMediaUri);
        urlInputLayout.setVisibility(View.GONE);
        isUrlInputVisible = false;
    }

    private void playMedia() {
        if (currentMediaUri == null) {
            String url = urlEditText.getText() != null ? urlEditText.getText().toString().trim() : "";
            if (!url.isEmpty()) {
                loadVideoFromUrl();
            } else {
                showMessage("Please select a file or enter a URL first");
            }
            return;
        }

        if (!isPrepared) {
            return;
        }

        if (isAudioMode && audioPlayer != null) {
            if (!audioPlayer.isPlaying()) {
                audioPlayer.start();
                updateStatus(getString(R.string.playing));
            }
        } else {
            if (!videoView.isPlaying()) {
                videoView.start();
                updateStatus(getString(R.string.playing));
            }
        }
    }

    private void pauseMedia() {
        if (isAudioMode && audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                updateStatus(getString(R.string.paused));
            }
        } else if (videoView.isPlaying()) {
            videoView.pause();
            updateStatus(getString(R.string.paused));
        }
    }

    private void stopMedia() {
        if (isAudioMode && audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer.reset();
            isPrepared = false;
            try {
                audioPlayer.setDataSource(this, currentMediaUri);
                audioPlayer.prepareAsync();
            } catch (IOException e) {
                showError("Failed to reset audio: " + e.getMessage());
            }
        } else {
            videoView.stopPlayback();
            if (currentMediaUri != null) {
                videoView.setVideoURI(currentMediaUri);
            }
        }
        updateStatus(getString(R.string.stopped));
    }

    private void restartMedia() {
        if (currentMediaUri == null) {
            showMessage("No media loaded to restart");
            return;
        }

        if (isAudioMode && audioPlayer != null) {
            audioPlayer.seekTo(0);
            if (!audioPlayer.isPlaying()) {
                audioPlayer.start();
            }
        } else {
            videoView.seekTo(0);
            if (!videoView.isPlaying()) {
                videoView.start();
            }
        }
        updateStatus(getString(R.string.playing));
    }

    private void resetMediaPlayer() {
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
        videoView.stopPlayback();
        videoView.clearFocus();

        currentMediaUri = null;
        isAudioMode = false;
        isPrepared = false;
        videoView.setVisibility(View.VISIBLE);

        updateStatus(getString(R.string.stopped));
        fileNameText.setText(getString(R.string.no_file_selected));
        progressBar.setVisibility(View.GONE);
    }

    private void updateStatus(String status) {
        statusText.setText(status);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }

    private void showError(String error) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error))
                .setMessage(error)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "Unknown file";
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isAudioMode && audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();
            updateStatus(getString(R.string.paused));
        } else if (!isAudioMode && videoView.isPlaying()) {
            videoView.pause();
            updateStatus(getString(R.string.paused));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
        videoView.stopPlayback();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("url_input_visible", isUrlInputVisible);
        if (currentMediaUri != null) {
            outState.putString("media_uri", currentMediaUri.toString());
            outState.putBoolean("is_audio_mode", isAudioMode);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isUrlInputVisible = savedInstanceState.getBoolean("url_input_visible", false);
        if (isUrlInputVisible) {
            urlInputLayout.setVisibility(View.VISIBLE);
        }
        String uriString = savedInstanceState.getString("media_uri");
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            boolean wasAudio = savedInstanceState.getBoolean("is_audio_mode", false);
            if (wasAudio) {
                loadAudioFile(uri);
            }
        }
    }
}
