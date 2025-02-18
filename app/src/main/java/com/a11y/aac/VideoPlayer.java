package com.a11y.aac;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayer extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        String path = intent.getStringExtra("videoPath");

        VideoView videoPlayer = findViewById(R.id.instanceVideoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoPlayer);
        videoPlayer.setOnCompletionListener(this);

        videoPlayer.setMediaController(mediaController);
        videoPlayer.setVideoPath(path);
        videoPlayer.requestFocus();
        videoPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        finish();
    }

    public void backButtonClicked(View view) {
        finish();
    }
}