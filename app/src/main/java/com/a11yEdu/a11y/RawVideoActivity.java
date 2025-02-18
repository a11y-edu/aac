package com.a11yEdu.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class RawVideoActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private VideoAdapter videoAdapter;
    private File directory;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_video);

        // Initialize components
        recyclerView = findViewById(R.id.recycler_view);
        emptyTextView = findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        // Initialize thread handling
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // Load videos asynchronously
        loadVideoList();
    }


    private void loadVideoList() {
        executorService.execute(() -> {
            try {
                File rawVideos = new File(directory, "rawVideos");
                File[] files = rawVideos.listFiles();
                List<Video> videos = new ArrayList<>();

                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName));

                    // First pass: Create Video objects with null thumbnails
                    for (File file : files) {
                        if (isVideoFile(file)) {
                            String title = file.getName().replaceFirst("[.][^.]+$", "");
                            videos.add(new Video(file.getPath(), null, title));
                        }
                    }

                    // Update UI with the list immediately
                    mainHandler.post(() -> {
                        if (videos.isEmpty()) {
                            updateEmptyView(true);
                        } else {
                            updateEmptyView(false);
                            videoAdapter = new VideoAdapter(videos,
                                    ContextCompat.getDrawable(this, R.drawable.placeholder),
                                    video -> editRawVideoDialog(video.getPath()),
                                    video -> playVideo(video.getPath()));
                            recyclerView.setAdapter(videoAdapter);

                            // Start loading thumbnails
                            loadThumbnails(videos);
                        }
                    });
                } else {
                    mainHandler.post(() -> updateEmptyView(true));
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> showError("Error loading videos: " + e.getMessage()));
            }
        });
    }

    private void loadThumbnails(List<Video> videos) {
        AtomicInteger loadedCount = new AtomicInteger(0);

        for (int i = 0; i < videos.size(); i++) {
            final int position = i;
            executorService.execute(() -> {
                Video video = videos.get(position);
                try {
                    Bitmap thumbnail = generateThumbnail(new File(video.getPath()));
                    mainHandler.post(() -> {
                        video.setThumbnail(thumbnail);
                        videoAdapter.notifyItemChanged(position);

                        // Track progress
                        int progress = loadedCount.incrementAndGet();
                        if (progress == videos.size()) {
                            // All thumbnails loaded
                            System.out.println("All videos loaded");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private Bitmap generateThumbnail(File videoFile) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFile.getPath());

            // Get a lower quality frame first
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;  // Reduce image size by factor of 4

            Bitmap originalThumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            if (originalThumbnail != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int targetWidth = screenWidth / 2;
                float aspectRatio = (float) originalThumbnail.getHeight() / originalThumbnail.getWidth();
                int targetHeight = (int) (targetWidth * aspectRatio);

                return Bitmap.createScaledBitmap(originalThumbnail, targetWidth, targetHeight, false);  // false for faster scaling
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void playVideo(String videoPath) {
        Intent intent = new Intent(this, VideoPlayer.class);
        intent.putExtra("videoPath", videoPath);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void editRawVideoDialog(String videoPath) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        // Find views in the custom layout
        TextView titleTextView = dialogView.findViewById(R.id.alertTitle);
        EditText fileNameEditText = dialogView.findViewById(R.id.fileNameEditText);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);
        Button renameButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        titleTextView.setText("Enter new video title");
        deleteButton.setText("Delete");
        renameButton.setText("Rename");

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        deleteButton.setOnClickListener(v -> {
            deleteData(videoPath);
            Intent intent = getIntent();
            finish();
            startActivity(intent);
            alertDialog.dismiss();
        });

        renameButton.setOnClickListener(v -> {
            String renamedFileName = fileNameEditText.getText().toString();

            if (renamedFileName.isEmpty()) {
                Toast.makeText(this, "Please enter a valid file name", Toast.LENGTH_SHORT).show();
                return;
            }

            renamedFileName = renamedFileName.replace(" ", "_");
            renameData(videoPath, renamedFileName);
            Intent intent = getIntent();
            finish();
            startActivity(intent);
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void deleteData(String videoPath) {
        String csvPath = videoPath.replace("rawVideos", "rawData").replace(".mp4", ".csv");
        File videoFile = new File(videoPath);
        File csvFile = new File(csvPath);

        if (!videoFile.exists() || !csvFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (videoFile.delete() && csvFile.delete()) {
            Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }

    private void renameData(String videoPath, String newFileName) {
        String csvPath = videoPath.replace("rawVideos", "rawData").replace(".mp4", ".csv");
        File videoFile = new File(videoPath);
        File csvFile = new File(csvPath);

        if (!videoFile.exists() || !csvFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File newVideoFile = new File(videoFile.getParent(), newFileName + ".mp4");
        File newCsvFile = new File(csvFile.getParent(), newFileName + ".csv");

        if (newVideoFile.exists() || newCsvFile.exists()) {
            Toast.makeText(this, "File with the same name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        if (videoFile.renameTo(newVideoFile) && csvFile.renameTo(newCsvFile)) {
            Toast.makeText(this, "File renamed successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isVideoFile(File file) {
        return file.getName().toLowerCase().endsWith(".mp4");
    }

    public void backButtonClicked(View view) {
        executorService.shutdown();
        finish();
    }

    public void deleteAllRawData(View view) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        // Find views in the custom layout
        TextView titleTextView = dialogView.findViewById(R.id.alertTitle);
        EditText fileNameEditText = dialogView.findViewById(R.id.fileNameEditText);
        Button renameButton = dialogView.findViewById(R.id.deleteButton);
        Button deleteButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        titleTextView.setText("Are you sure you want to delete all raw data?");
        titleTextView.setTextSize(22);
        titleTextView.setPadding(0, 0, 0, 100);
        deleteButton.setText("Delete");
        renameButton.setVisibility(View.GONE);
        fileNameEditText.setVisibility(View.GONE);
        deleteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red));

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        deleteButton.setOnClickListener(v -> {
            File rawVideos = new File(directory, "rawVideos");
            File rawData = new File(directory, "rawData");

            if (rawVideos.exists()) {
                deleteDirectory(rawVideos);
            }

            if (rawData.exists()) {
                deleteDirectory(rawData);
            }

            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}