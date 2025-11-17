package com.abandiak.alerta.app.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DMChatsAdapter extends RecyclerView.Adapter<DMChatsAdapter.DMViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(DMChatEntry chat);
    }

    private final List<DMChatEntry> items = new ArrayList<>();
    private OnChatClickListener listener;

    public void setOnChatClickListener(OnChatClickListener l) {
        this.listener = l;
    }

    public void submit(List<DMChatEntry> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DMViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_dm, parent, false);
        return new DMViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DMViewHolder h, int position) {
        DMChatEntry entry = items.get(position);

        h.textName.setText(entry.getDisplayName());
        h.textLastMessage.setText(entry.getLastMessage());
        h.textTime.setText(entry.getFormattedTime());

        // Avatar
        if (entry.getAvatarUrl() != null && !entry.getAvatarUrl().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(entry.getAvatarUrl())
                    .centerCrop()
                    .into(h.imageAvatar);
        } else {
            h.imageAvatar.setImageResource(R.drawable.ic_person_placeholder);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(entry);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DMViewHolder extends RecyclerView.ViewHolder {

        TextView textName, textLastMessage, textTime;
        ImageView imageAvatar;

        public DMViewHolder(@NonNull View v) {
            super(v);
            textName = v.findViewById(R.id.textName);
            textLastMessage = v.findViewById(R.id.textLastMessage);
            textTime = v.findViewById(R.id.textTime);
            imageAvatar = v.findViewById(R.id.imageAvatar);
        }
    }
}
