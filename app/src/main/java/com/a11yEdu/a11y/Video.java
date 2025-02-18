package com.a11yEdu.a11y;

import android.graphics.Bitmap;

public class Video {
    private String path;
    private Bitmap thumbnail;
    private String title;
    private boolean showEditButton;
    private boolean showPlayButton;

    public Video(String path, Bitmap thumbnail, String title) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.title = title;
        this.showEditButton = true;
        this.showPlayButton = true;
    }

    public Video(String path, Bitmap thumbnail, String title, boolean showEditButton, boolean showPlayButton) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.title = title;
        this.showEditButton = showEditButton;
        this.showPlayButton = showPlayButton;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPath() {
        return path;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public boolean getShowEditButton() {
        return showEditButton;
    }

    public boolean getShowPlayButton() {
        return showPlayButton;
    }
}
