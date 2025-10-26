package com.abandiak.alerta.app.teams;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.Team;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeamListAdapter extends RecyclerView.Adapter<TeamListAdapter.TeamVH> {

    public interface OnTeamClick {
        void onTeamClick(Team team);
    }

    private final List<Team> items = new ArrayList<>();
    private OnTeamClick clickListener;

    public void setOnTeamClick(OnTeamClick listener) {
        this.clickListener = listener;
    }

    public void submit(List<Team> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeamVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team, parent, false);
        return new TeamVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamVH h, int position) {
        Team t = items.get(position);

        h.textName.setText(t.getName() == null ? "Unnamed team" : t.getName());

        String code = t.getCode() == null ? "—" : t.getCode();
        String meta = "Code: " + code;
        if (t.getCreatedAt() > 0) {
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(t.getCreatedAt()));
            meta = meta + " • " + date;
        }
        h.textMeta.setText(meta);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTeamClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TeamVH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView textName, textMeta;

        TeamVH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            textName = v.findViewById(R.id.textName);
            textMeta = v.findViewById(R.id.textMeta);
        }
    }
}
