package com.a11yEdu.a11y;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<Video> videoList;
    private Drawable placeholder;
    private OnItemClickListener listener;
    private OnEditClickListener editListener;
    private OnPlayClickListener playListener;

    public interface OnItemClickListener {
        void onItemClick(Video video);
    }

    public interface OnEditClickListener {
        void onEditClick(Video video);
    }

    public interface OnPlayClickListener {
        void onPlayClick(Video video);
    }

    public VideoAdapter(List<Video> videoList, Drawable placeholder, OnItemClickListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    public VideoAdapter(List<Video> videoList, Drawable placeholder, OnItemClickListener listener, OnEditClickListener editListener, OnPlayClickListener playListener) {
        this.videoList = videoList;
        this.placeholder = placeholder;
        this.listener = listener;
        this.editListener = editListener;
        this.playListener = playListener;
    }

    public VideoAdapter(List<Video> videoList, Drawable placeholder, OnEditClickListener editListener, OnPlayClickListener playListener) {
        this.videoList = videoList;
        this.placeholder = placeholder;
        this.listener = listener;
        this.editListener = editListener;
        this.playListener = playListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        if (video.getShowEditButton()) {
            holder.editButton.setVisibility(View.VISIBLE);
        } else {
            holder.editButton.setVisibility(View.GONE);
        }
        if (video.getShowPlayButton()) {
            holder.playButton.setVisibility(View.VISIBLE);
        } else {
            holder.playButton.setVisibility(View.GONE);
        }
        if (video.getThumbnail() != null) {
            holder.thumbnailImageView.setImageBitmap(video.getThumbnail());
        } else {
            holder.thumbnailImageView.setImageDrawable(placeholder);
        }
        // Bind title to TextView
        holder.titleTextView.setText(video.getTitle());
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView titleTextView;

        ImageView editButton;
        ImageView playButton;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.image_thumbnail);
            titleTextView = itemView.findViewById(R.id.text_title);
            editButton = itemView.findViewById(R.id.edit_button);
            playButton = itemView.findViewById(R.id.play_button);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Video video = videoList.get(position);
                    if (listener != null) {
                        listener.onItemClick(video);
                    }
                }
            });

            editButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Video video = videoList.get(position);
                    if (editListener != null) {
                        editListener.onEditClick(video);
                    }
                }
            });

            playButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Video video = videoList.get(position);
                    if (playListener != null) {
                        playListener.onPlayClick(video);
                    }
                }
            });
        }
    }

    public void updateVideos(List<Video> newVideos) {
        this.videoList = newVideos;
        notifyDataSetChanged();
    }
}
