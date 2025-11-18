package com.abandiak.alerta.app.messages.teams;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.ChatMessage;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TeamMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private String currentUid;
    private List<ChatMessage> messages = new ArrayList<>();

    private String currentUserAvatar = null;
    private String currentUserName = "You";

    public TeamMessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    public void setCurrentUserAvatar(String url) {
        this.currentUserAvatar = url;
        notifyDataSetChanged();
    }

    public void setCurrentUserName(String name) {
        this.currentUserName = name;
        notifyDataSetChanged();
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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ChatMessage msg = messages.get(position);

        if (holder instanceof SentHolder) {
            SentHolder h = (SentHolder) holder;

            h.text.setText(msg.getText());
            h.name.setText(currentUserName);

            Glide.with(h.avatar.getContext())
                    .load(currentUserAvatar)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(h.avatar);

        } else {
            ReceivedHolder h = (ReceivedHolder) holder;

            h.text.setText(msg.getText());
            h.name.setText(msg.getSenderName());

            Glide.with(h.avatar.getContext())
                    .load(msg.getSenderAvatar())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(h.avatar);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView text, name;
        ImageView avatar;

        public SentHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
            name = itemView.findViewById(R.id.textName);
            avatar = itemView.findViewById(R.id.avatar);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView text, name;
        ImageView avatar;

        public ReceivedHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
            name = itemView.findViewById(R.id.textName);
            avatar = itemView.findViewById(R.id.avatar);
        }
    }
}
