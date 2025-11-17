package com.abandiak.alerta.app.messages.teams;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class TeamMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private String currentUid;
    private List<ChatMessage> messages = new ArrayList<>();

    public TeamMessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    public void submitList(List<ChatMessage> newList) {
        this.messages = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        return msg.getSenderId().equals(currentUid) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_team_message_sent, parent, false);
            return new SentHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_team_message_received, parent, false);
            return new ReceivedHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        ChatMessage msg = messages.get(position);

        if (h instanceof SentHolder) {
            ((SentHolder) h).text.setText(msg.getText());
        } else {
            ((ReceivedHolder) h).text.setText(msg.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView text;

        public SentHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView text;

        public ReceivedHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
        }
    }
}
