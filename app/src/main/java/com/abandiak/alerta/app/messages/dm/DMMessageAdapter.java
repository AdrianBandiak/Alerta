package com.abandiak.alerta.app.messages.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.ChatMessage;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DMMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int SENT = 1;
    private static final int RECEIVED = 2;

    private String currentUid;
    private String otherUserAvatar;

    private List<ChatMessage> messages = new ArrayList<>();

    public DMMessageAdapter() {}

    public void setCurrentUid(String uid) {
        this.currentUid = uid;
    }

    public void setOtherUserAvatar(String url) {
        this.otherUserAvatar = url;
        notifyDataSetChanged();
    }

    public void submitList(List<ChatMessage> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return messages.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldItemPos, int newItemPos) {
                return messages.get(oldItemPos).getId()
                        .equals(newList.get(newItemPos).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPos, int newItemPos) {
                ChatMessage m1 = messages.get(oldItemPos);
                ChatMessage m2 = newList.get(newItemPos);

                return m1.getText().equals(m2.getText()) &&
                        m1.getCreatedAt() == m2.getCreatedAt();
            }
        });

        messages = new ArrayList<>(newList);
        diff.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int pos) {
        return messages.get(pos).getSenderId().equals(currentUid) ? SENT : RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == SENT) {
            return new SentHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_team_message_sent, parent, false));

        } else {
            return new ReceivedHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_team_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {

        ChatMessage msg = messages.get(pos);

        if (h instanceof SentHolder) {
            SentHolder sh = (SentHolder) h;
            sh.name.setVisibility(View.GONE);
            sh.avatar.setVisibility(View.GONE);
            sh.text.setText(msg.getText());

        } else {
            ReceivedHolder rh = (ReceivedHolder) h;
            rh.name.setVisibility(View.GONE);
            rh.text.setText(msg.getText());

            Glide.with(rh.avatar.getContext())
                    .load(otherUserAvatar)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(rh.avatar);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView text, name;
        ImageView avatar;
        public SentHolder(View v) {
            super(v);
            text = v.findViewById(R.id.textMessage);
            name = v.findViewById(R.id.textName);
            avatar = v.findViewById(R.id.avatar);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView text, name;
        ImageView avatar;
        public ReceivedHolder(View v) {
            super(v);
            text = v.findViewById(R.id.textMessage);
            name = v.findViewById(R.id.textName);
            avatar = v.findViewById(R.id.avatar);
        }
    }
}
