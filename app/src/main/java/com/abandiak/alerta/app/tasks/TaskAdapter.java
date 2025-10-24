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

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
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

        if (t.isCompleted()) {
            h.icon.setColorFilter(
                    h.itemView.getContext().getColor(R.color.status_online_bg),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        } else {
            h.icon.setColorFilter(
                    h.itemView.getContext().getColor(R.color.alerta_primary),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }

        h.checkDone.setOnCheckedChangeListener(null);
        h.checkDone.setChecked(t.isCompleted());

        if (t.isCompleted()) {
            h.itemView.setAlpha(0.5f);
            h.textTitle.setAlpha(0.6f);
            h.textMeta.setAlpha(0.6f);
        } else {
            h.itemView.setAlpha(1f);
            h.textTitle.setAlpha(1f);
            h.textMeta.setAlpha(1f);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });

        h.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (t.isCompleted() != isChecked) {
                t.setCompleted(isChecked);
                repo.updateTaskCompletion(t.getId(), isChecked);
                notifyItemChanged(h.getAdapterPosition());
            }
        });
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
