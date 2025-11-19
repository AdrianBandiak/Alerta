package com.abandiak.alerta.app.messages.teams;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TeamMembersAdapter extends RecyclerView.Adapter<TeamMembersAdapter.Holder> {

    public interface OnMemberDmClick {
        void onDmClick(TeamMemberEntry member);
    }

    private final List<TeamMemberEntry> list = new ArrayList<>();
    private final OnMemberDmClick listener;

    public TeamMembersAdapter(OnMemberDmClick click) {
        this.listener = click;
    }

    public void submit(List<TeamMemberEntry> members) {
        list.clear();
        list.addAll(members);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        TeamMemberEntry m = list.get(pos);

        h.name.setText(m.getFullName());

        Glide.with(h.avatar.getContext())
                .load(m.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(h.avatar);

        h.btnDm.setOnClickListener(v -> listener.onDmClick(m));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        ImageButton btnDm;

        Holder(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            name = v.findViewById(R.id.textMemberName);
            btnDm = v.findViewById(R.id.btnDmMember);
        }
    }
}

