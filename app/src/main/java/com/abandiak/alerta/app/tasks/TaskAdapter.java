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
import com.abandiak.alerta.data.repository.TaskRepository;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> items;
    private OnTaskClickListener listener;
    private OnTaskStatusChangedListener statusListener;
    private final TaskRepository repo = new TaskRepository();

    public TaskAdapter(List<Task> items) {
        this.items = items;
    }

    public void updateData(List<Task> newData) {
        this.items = newData;
        notifyDataSetChanged();
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskStatusChangedListener {
        void onStatusChanged();
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setOnTaskStatusChangedListener(OnTaskStatusChangedListener listener) {
        this.statusListener = listener;
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

        if ("TEAM".equals(t.getType())) {
            h.icon.setImageResource(R.drawable.ic_shield);

            if (t.getTeamColor() != null) {
                h.icon.setColorFilter(t.getTeamColor(), android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                h.icon.setColorFilter(
                        h.itemView.getContext().getColor(R.color.alerta_primary),
                        android.graphics.PorterDuff.Mode.SRC_IN
                );
            }

        } else {
            h.icon.setImageResource(R.drawable.ic_task);
            h.icon.setColorFilter(
                    h.itemView.getContext().getColor(R.color.alerta_primary),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }

        applyTaskStyle(h, t.isCompleted());

        h.checkDone.setOnCheckedChangeListener(null);
        h.checkDone.setChecked(t.isCompleted());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });

        h.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (t.isCompleted() != isChecked) {
                t.setCompleted(isChecked);
                repo.updateTaskCompletion(t.getId(), isChecked);

                applyTaskStyle(h, isChecked);

                if (statusListener != null) statusListener.onStatusChanged();
            }
        });
    }

    private void applyTaskStyle(TaskViewHolder h, boolean completed) {
        float alpha = completed ? 0.5f : 1f;

        h.itemView.setAlpha(alpha);
        h.textTitle.setAlpha(alpha);
        h.textMeta.setAlpha(alpha);
        h.icon.setAlpha(alpha);
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
