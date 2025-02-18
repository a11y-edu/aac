package com.a11yEdu.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

public class VideoListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private VideoAdapter videoAdapter;
    private List<Video> videoList;
    private File directory;
    private String gestureName;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        recyclerView = findViewById(R.id.recycler_view);
        emptyTextView = findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        Intent intent = getIntent();
        gestureName = intent.getStringExtra("gestureCategoryName");

        File trimmedVideosDir = new File(directory, "trimmedVideos");
        if (!trimmedVideosDir.exists()) {
            trimmedVideosDir.mkdirs();
        }

        // Initialize thread handling
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

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
                            videos.add(new Video(file.getPath(), null, title, false, false));
                        }
                    }

                    // Update UI with the list immediately
                    mainHandler.post(() -> {
                        if (videos.isEmpty()) {
                            updateEmptyView(true);
                        } else {
                            updateEmptyView(false);

                            videoAdapter = new VideoAdapter(videos, ContextCompat.getDrawable(this, R.drawable.placeholder), video -> startTrimActivity(video.getPath()));
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

    private boolean isVideoFile(File file) {
        return file.getName().toLowerCase().endsWith(".mp4");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void startTrimActivity(String videoPath) {
        Intent intent = new Intent(VideoListActivity.this, TrimVideoActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("gestureCategoryName", gestureName);
        startActivity(intent);
        finish();
    }

    public void backButtonClicked(View view) {
        executorService.shutdown();
        finish();
    }
}