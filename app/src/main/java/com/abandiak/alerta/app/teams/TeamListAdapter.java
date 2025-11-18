package com.abandiak.alerta.app.teams;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
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

        h.textName.setText(t.getName());

        h.textName.setSelected(true);
        h.textName.setHorizontalFadingEdgeEnabled(true);
        h.textName.setFadingEdgeLength(40);

        String code = t.getCode() == null ? "—" : t.getCode();
        String info = "Code: " + code;

        if (t.getCreatedAt() != null) {
            long ms = t.getCreatedAt().toDate().getTime();
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(ms));
            info += " • " + date;
        }
        h.textMeta.setText(info);

        int teamColor = (t.getColor() != 0)
                ? t.getColor()
                : h.itemView.getContext().getColor(R.color.alerta_primary);

        ImageViewCompat.setImageTintList(h.icon, ColorStateList.valueOf(teamColor));

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
            icon = v.findViewById(R.id.iconTeam);
            textName = v.findViewById(R.id.textTeamName);
            textMeta = v.findViewById(R.id.textTeamSecondary);
        }
    }
}
