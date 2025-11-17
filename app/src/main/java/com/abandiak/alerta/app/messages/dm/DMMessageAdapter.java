package com.abandiak.alerta.app.messages.dm;

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

public class DMMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private String currentUid;
    private List<ChatMessage> messages = new ArrayList<>();

    public DMMessageAdapter(String currentUid) {
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

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        ChatMessage msg = messages.get(position);

        if (holder instanceof SentHolder) {
            ((SentHolder) holder).txt.setText(msg.getText());
        } else {
            ((ReceivedHolder) holder).txt.setText(msg.getText());
        }
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView txt;
        SentHolder(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.textMessage);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView txt;
        ReceivedHolder(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.textMessage);
        }
    }
}
