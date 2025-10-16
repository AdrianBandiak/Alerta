package com.abandiak.alerta.app.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> items;

    public TaskAdapter(List<Task> items) {
        this.items = items;
    }

    public void updateData(List<Task> newData) {
        this.items = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_home, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder h, int i) {
        Task t = items.get(i);

        String idShort = t.getId() != null && t.getId().length() > 4
                ? t.getId().substring(0, 4).toUpperCase()
                : t.getId();
        h.textTitle.setText("[#" + idShort + "] " + t.getTitle());
        h.textMeta.setText(t.getTime());

        h.checkDone.setChecked(t.isCompleted());
        h.icon.setImageResource(t.isCompleted() ? R.drawable.ic_check_circle : R.drawable.ic_check);
        h.icon.setAlpha(t.isCompleted() ? 0.5f : 1f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView textTitle, textMeta;
        CheckBox checkDone;

        TaskViewHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            textTitle = v.findViewById(R.id.textTitle);
            textMeta = v.findViewById(R.id.textMeta);
            checkDone = v.findViewById(R.id.checkDone);
        }
    }
}
