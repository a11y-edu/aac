package com.a11yEdu.a11y;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private final List<CardData> cardDataList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public CardAdapter(List<CardData> cardDataList, OnItemClickListener listener) {
        this.cardDataList = cardDataList;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CardData cardData = cardDataList.get(position);
        holder.gestureTextView.setText(cardData.getName());
        holder.textToSpeakTextView.setText(cardData.getTextToSpeak());
    }

    @Override
    public int getItemCount() {
        return cardDataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView gestureTextView;
        public TextView textToSpeakTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            gestureTextView = itemView.findViewById(R.id.nameTextView);
            textToSpeakTextView = itemView.findViewById(R.id.textToSpeakTextView);

            itemView.setOnClickListener(v -> listener.onItemClick(v, getAdapterPosition()));
        }
    }
}
