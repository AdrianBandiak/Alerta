package com.abandiak.alerta.app.messages.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class DMChatsAdapter extends RecyclerView.Adapter<DMChatsAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(DMChatEntry chat);
    }

    private List<DMChatEntry> chats = new ArrayList<>();
    private OnChatClickListener listener;

    public DMChatsAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DMChatEntry> newList) {
        this.chats = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DMChatsAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dm_chat, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DMChatsAdapter.ViewHolder holder,
            int position) {

        DMChatEntry chat = chats.get(position);

        holder.textName.setText(chat.getOtherUserName());
        holder.textLastMessage.setText(chat.getLastMessage());

        if (chat.getAvatarUrl() != null && !chat.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chat.getAvatarUrl())
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(holder.imageAvatar);
        } else {
            holder.imageAvatar.setImageResource(R.drawable.ic_person_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(chat);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ShapeableImageView imageAvatar;
        TextView textName, textLastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textName = itemView.findViewById(R.id.textName);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
        }
    }
}
