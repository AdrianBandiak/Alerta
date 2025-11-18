package com.abandiak.alerta.app.messages.teams;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;

import java.util.ArrayList;
import java.util.List;

public class TeamChatsAdapter extends RecyclerView.Adapter<TeamChatsAdapter.TeamViewHolder> {

    public interface OnTeamChatClickListener {
        void onTeamChatClick(TeamChatEntry chat);
    }

    private final List<TeamChatEntry> items = new ArrayList<>();
    private OnTeamChatClickListener listener;

    public void setOnTeamChatClickListener(OnTeamChatClickListener l) {
        this.listener = l;
    }

    public void submit(List<TeamChatEntry> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_team, parent, false);
        return new TeamViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder h, int position) {
        TeamChatEntry entry = items.get(position);

        h.textTeamName.setText(entry.getTeamName());
        h.textLastMessage.setText(entry.getLastMessage());
        h.textTime.setText(entry.getFormattedTime());

        h.imageTeamIcon.setImageResource(R.drawable.ic_group);
        h.imageTeamIcon.setColorFilter(entry.getTeamColor());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTeamChatClick(entry);
        });
    }



    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {

        TextView textTeamName, textLastMessage, textTime;
        FrameLayout iconBackground;
        ImageView imageTeamIcon;

        TeamViewHolder(@NonNull View v) {
            super(v);
            textTeamName = v.findViewById(R.id.textTeamName);
            textLastMessage = v.findViewById(R.id.textLastMessage);
            textTime = v.findViewById(R.id.textTime);
            iconBackground = v.findViewById(R.id.iconBackground);
            imageTeamIcon = v.findViewById(R.id.iconTeam);
        }

    }
}
