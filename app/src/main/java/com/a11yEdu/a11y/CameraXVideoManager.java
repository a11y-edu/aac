package com.a11yEdu.a11y;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraXVideoManager {
    private ProcessCameraProvider cameraProvider;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private Preview preview;
    private Camera camera;
    private boolean isInitialized = false;
    private final Object lock = new Object();
    private Context context;
    private boolean isTransitioning = false;
    private static final long MIN_TOGGLE_INTERVAL = 500; // milliseconds
    private long lastToggleTime = 0;

    public CameraXVideoManager(Context context) {
        this.context = context;
    }

    public void initCamera(Activity activity, PreviewView previewView, int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(activity);

        processCameraProvider.addListener(() -> {
            try {
                synchronized (lock) {
                    cameraProvider = processCameraProvider.get();

                    // Create preview use case
                    preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    // Create video capture use case
                    Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build();
                    videoCapture = VideoCapture.withOutput(recorder);

                    // Unbind any previous use cases
                    cameraProvider.unbindAll();

                    // Create camera selector
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

                    // Bind use cases
                    camera = cameraProvider.bindToLifecycle((LifecycleOwner) activity, cameraSelector, preview, videoCapture);

                    isInitialized = true;
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXVideoManager", "Error initializing camera", e);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    public void toggleRecording(Activity activity, File outputDirectory, ErrorCallback onError) {
        synchronized (lock) {
            // Check if we're in transition or if toggle is too rapid
            long currentTime = System.currentTimeMillis();
            if (isTransitioning || (currentTime - lastToggleTime) < MIN_TOGGLE_INTERVAL) {
                onError.onError("Please wait before toggling recording");
                return;
            }

            if (!isInitialized || videoCapture == null) {
                onError.onError("Camera not initialized");
                return;
            }

            isTransitioning = true;
            lastToggleTime = currentTime;

            Recording currentRecording = recording;
            if (currentRecording != null) {
                // Stop recording
                try {
                    currentRecording.stop();
                    recording = null;
                } finally {
                    isTransitioning = false;
                }
                return;
            }

            // Start new recording
            try {
                String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",
                        Locale.getDefault()).format(System.currentTimeMillis()) + ".mp4";
                File videoFile = new File(outputDirectory, fileName);
                FileOutputOptions options = new FileOutputOptions.Builder(videoFile).build();


                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    onError.onError("Record audio permission not granted");
                    isTransitioning = false;
                    return;
                }

                recording = videoCapture.getOutput()
                        .prepareRecording(activity, options)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(activity), videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                isTransitioning = false;
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                VideoRecordEvent.Finalize finalizeEvent =
                                        (VideoRecordEvent.Finalize) videoRecordEvent;
                                if (finalizeEvent.hasError()) {
                                    recording = null;
                                    onError.onError("Error: " + finalizeEvent.getError());
                                }
                                isTransitioning = false;
                            }
                        });
            } catch (Exception e) {
                isTransitioning = false;
                onError.onError("Failed to start recording: " + e.getMessage());
            }
        }
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    public interface ErrorCallback {
        void onError(String error);
    }

    public void release() {
        synchronized (lock) {
            if (isInitialized && cameraProvider != null) {
                if (recording != null) {
                    recording.stop();
                    recording = null;
                }
                cameraProvider.unbindAll();
                isInitialized = false;
            }
        }
    }

    public boolean isRecording() {
        synchronized (lock) {
            return recording != null;
        }
    }

    public boolean isInitialized() {
        synchronized (lock) {
            return isInitialized && cameraProvider != null && videoCapture != null && camera != null;
        }
    }
}
