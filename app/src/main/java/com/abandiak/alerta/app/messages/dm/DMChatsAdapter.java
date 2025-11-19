package com.abandiak.alerta.app.messages.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DMChatsAdapter extends RecyclerView.Adapter<DMChatsAdapter.ChatHolder> {

    public interface OnChatClick {
        void onClick(DMChatEntry chat);
    }

    private List<DMChatEntry> items = new ArrayList<>();
    private final OnChatClick listener;

    public DMChatsAdapter(OnChatClick listener) {
        this.listener = listener;
    }

    public void submitList(List<DMChatEntry> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dm_chat, parent, false);
        return new ChatHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHolder h, int pos) {
        DMChatEntry entry = items.get(pos);

        h.name.setText(entry.getOtherUserName());
        h.lastMessage.setText(entry.getLastMessage());

        Glide.with(h.avatar.getContext())
                .load(entry.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(h.avatar);

        h.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onClick(entry);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChatHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name, lastMessage;

        ChatHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.imageAvatar);
            name = v.findViewById(R.id.textName);
            lastMessage = v.findViewById(R.id.textLastMessage);
        }
    }
}
