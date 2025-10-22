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
    private OnTaskClickListener listener;

    public TaskAdapter(List<Task> items) {
        this.items = items;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Task> newData) {
        this.items = newData;
        notifyDataSetChanged();
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
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

        if (t.isCompleted()) {
            h.icon.setImageResource(R.drawable.ic_check_circle);
            h.icon.setColorFilter(
                    h.itemView.getContext().getColor(R.color.status_online_bg),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
            h.icon.setAlpha(1f);
        } else {
            h.icon.setImageResource(R.drawable.ic_task);
            h.icon.setColorFilter(
                    h.itemView.getContext().getColor(R.color.alerta_primary),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
            h.icon.setAlpha(1f);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
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
