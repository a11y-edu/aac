package com.a11y.aac;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout recordDataCardView, accessibleGesturesCardView, annotateGestureCardView, visualizeGestureCardView, rawVideoActivityCardView, getHelpCardView, bluetoothCardView;
    ProgressBar progressBar;
    View overlay;
    ConstraintLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recordDataCardView = findViewById(R.id.recordDataCardView);
        accessibleGesturesCardView = findViewById(R.id.accessibleGesturesCardView);
        annotateGestureCardView = findViewById(R.id.annotateGestureCardView);
        visualizeGestureCardView = findViewById(R.id.visualizeGestureCardView);
        rawVideoActivityCardView = findViewById(R.id.rawVideoCardView);
        getHelpCardView = findViewById(R.id.getHelpCardView);
        bluetoothCardView = findViewById(R.id.bluetoothCardView);

        progressBar = findViewById(R.id.progress);
        overlay = findViewById(R.id.overlay);
        content = findViewById(R.id.homeLayout);

        recordDataCardView.setOnClickListener(this);
        accessibleGesturesCardView.setOnClickListener(this);
        annotateGestureCardView.setOnClickListener(this);
        visualizeGestureCardView.setOnClickListener(this);
        rawVideoActivityCardView.setOnClickListener(this);
        getHelpCardView.setOnClickListener(this);
        bluetoothCardView.setOnClickListener(this);

        createDirectories();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            FilePairChecker.deleteUnmatchedFiles(this);

            handler.post(() -> {
                System.out.println("Unmatched files have been deleted.");
            });
        });
    }

    public void createDirectories() {
        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File rawData = new File(directory, "rawData");
        File trimmedData = new File(directory, "trimmedData");
        File models = new File(directory, "models");
        File rawVideos = new File(directory, "rawVideos");

        if (!rawData.exists()) {
            rawData.mkdirs();
        }

        if (!trimmedData.exists()) {
            trimmedData.mkdirs();
        }

        if (!models.exists()) {
            models.mkdirs();
        }

        if (!rawVideos.exists()) {
            rawVideos.mkdirs();
        }
    }

    @Override
    public void onClick(View v) {
        progressBar.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
        Intent i;
        if (v.getId() == R.id.recordDataCardView) {
            i = new Intent(this, RecordActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.accessibleGesturesCardView) {
            i = new Intent(this, AccessibleActivity.class);
            startActivity(i);
        }  else if (v.getId() == R.id.annotateGestureCardView) {
            i = new Intent(this, GestureCategoryActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.visualizeGestureCardView) {
            i = new Intent(this, VisualizationActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.rawVideoCardView) {
            i = new Intent(this, RawVideoActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.getHelpCardView) {
            i = new Intent(this, HelpActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.bluetoothCardView) {
            i = new Intent(this, BluetoothActivity.class);
            startActivity(i);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }
}