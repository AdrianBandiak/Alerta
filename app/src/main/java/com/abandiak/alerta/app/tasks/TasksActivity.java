package com.abandiak.alerta.app.tasks;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.data.model.Task;
import com.abandiak.alerta.data.repository.TaskRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class TasksActivity extends AppCompatActivity {

    private TaskAdapter adapter;
    private TaskRepository repo;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_gray));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.status_bar_gray));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_tasks);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_tasks);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_teams) {
                    startActivity(new Intent(this, TeamsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                overridePendingTransition(0, 0);
                return id == R.id.nav_tasks;
            });
        }

        repo = new TaskRepository();
        recycler = findViewById(R.id.recyclerTasks);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>());
        recycler.setAdapter(adapter);
        recycler.setVisibility(View.GONE);

        adapter.setOnTaskClickListener(this::showTaskDetailsDialog);

        FloatingActionButton fabAdd = findViewById(R.id.fabAddTask);
        fabAdd.setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
    }

    private void loadTasks() {
        repo.getTasksForToday(new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                if (tasks == null || tasks.isEmpty()) {
                    adapter.updateData(new ArrayList<>());
                    recycler.setVisibility(View.GONE);
                } else {
                    adapter.updateData(tasks);
                    recycler.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(TasksActivity.this, "Error loading tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTaskDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_task, null);

        TextInputEditText inputTitle = dialogView.findViewById(R.id.inputTaskTitle);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.inputTaskDescription);
        AutoCompleteTextView inputPriority = dialogView.findViewById(R.id.inputTaskPriority);
        TextInputEditText inputStartDate = dialogView.findViewById(R.id.inputTaskStartDate);
        TextInputEditText inputEndDate = dialogView.findViewById(R.id.inputTaskEndDate);

        String[] priorities = getResources().getStringArray(R.array.task_priorities);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_dropdown_priority,
                priorities
        );
        inputPriority.setAdapter(priorityAdapter);
        inputPriority.setOnClickListener(v -> inputPriority.showDropDown());

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());
        inputStartDate.setText(today);

        inputEndDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                inputEndDate.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String title = inputTitle.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            String priority = inputPriority.getText().toString().trim();
            String startDate = inputStartDate.getText().toString().trim();
            String endDate = inputEndDate.getText().toString().trim();

            if (title.isEmpty()) {
                inputTitle.setError("Title required");
                return;
            }

            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new java.util.Date());

            Task task = new Task(
                    UUID.randomUUID().toString(),
                    title,
                    repo.getCurrentUserId(),
                    currentTime,
                    false,
                    startDate
            );
            task.setDescription(desc);
            task.setPriority(priority);
            task.setEndDate(endDate);

            repo.addTask(task, new TaskRepository.OnTaskAddedListener() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                    loadTasks();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(TasksActivity.this, "Error saving task", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void showTaskDetailsDialog(Task task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_task_details, null);

        TextView title = dialogView.findViewById(R.id.textTaskTitle);
        TextView desc = dialogView.findViewById(R.id.textTaskDescription);
        TextView priority = dialogView.findViewById(R.id.textPriority);
        TextView start = dialogView.findViewById(R.id.textStartDate);
        TextView end = dialogView.findViewById(R.id.textEndDate);
        TextView created = dialogView.findViewById(R.id.textCreatedAt);
        TextView status = dialogView.findViewById(R.id.textStatus);


        title.setText(task.getTitle());
        desc.setText(task.getDescription() == null || task.getDescription().isEmpty()
                ? "No description provided."
                : task.getDescription());
        priority.setText(String.format(Locale.getDefault(),
                "Priority: %s",
                (task.getPriority() == null || task.getPriority().isEmpty()) ? "Normal" : task.getPriority()));

        java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

        String formattedStart = "-";
        String formattedEnd = "-";
        try {
            if (task.getDate() != null && !task.getDate().isEmpty())
                formattedStart = outputFormat.format(Objects.requireNonNull(inputFormat.parse(task.getDate())));
            if (task.getEndDate() != null && !task.getEndDate().isEmpty())
                formattedEnd = outputFormat.format(Objects.requireNonNull(inputFormat.parse(task.getEndDate())));
        } catch (Exception ignored) { }

        start.setText(String.format(Locale.getDefault(), "Start date: %s", formattedStart));
        end.setText(String.format(Locale.getDefault(), "End date: %s", formattedEnd));

        created.setText(String.format(Locale.getDefault(), "Created at: %s", task.getTime()));

        if (task.isCompleted()) {
            status.setText("Status: Completed");
            status.setTextColor(ContextCompat.getColor(this, R.color.status_online_bg));
        } else {
            status.setText("Status: In progress");
            status.setTextColor(ContextCompat.getColor(this, R.color.alerta_primary));
        }


        AlertDialog dialog = new AlertDialog.Builder(this, com.google.android.material.R.style.Theme_Material3_Light_Dialog_Alert)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


}
