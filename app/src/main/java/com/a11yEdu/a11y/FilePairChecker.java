package com.a11yEdu.a11y;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.HashSet;

public class FilePairChecker {

    public static void deleteUnmatchedFiles(Context context) {
        File documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDir == null) {
            return; // External storage not available
        }

        File rawDataDir = new File(documentsDir, "rawData");
        File rawVideosDir = new File(documentsDir, "rawVideos");

        if (!rawDataDir.exists() || !rawVideosDir.exists()) {
            return; // Directories do not exist
        }

        // Get file names (without extensions) from both directories
        HashSet<String> rawDataFiles = new HashSet<>();
        HashSet<String> rawVideoFiles = new HashSet<>();

        // Collect file names from rawData
        for (File file : rawDataDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".csv")) {
                rawDataFiles.add(getFileNameWithoutExtension(file.getName()));
            }
        }

        // Collect file names from rawVideos
        for (File file : rawVideosDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".mp4")) {
                rawVideoFiles.add(getFileNameWithoutExtension(file.getName()));
            }
        }

        // Identify and delete unmatched files in rawData
        for (File file : rawDataDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".csv")) {
                String fileNameWithoutExt = getFileNameWithoutExtension(file.getName());
                if (!rawVideoFiles.contains(fileNameWithoutExt)) {
                    file.delete();
                }
            }
        }

        // Identify and delete unmatched files in rawVideos
        for (File file : rawVideosDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".mp4")) {
                String fileNameWithoutExt = getFileNameWithoutExtension(file.getName());
                if (!rawDataFiles.contains(fileNameWithoutExt)) {
                    file.delete();
                }
            }
        }
    }

    private static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
