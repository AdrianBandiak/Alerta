package com.abandiak.alerta.app.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TasksActivity extends AppCompatActivity {

    private TaskAdapter adapter;
    private TaskRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_tasks);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_teams) {
                    startActivity(new Intent(this, TeamsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                overridePendingTransition(0, 0);
                return id == R.id.nav_tasks;
            });
        }

        repo = new TaskRepository();
        RecyclerView recycler = findViewById(R.id.recyclerTasks);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>());
        recycler.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fabAddTask);
        fabAdd.setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
    }


    private void loadTasks() {
        repo.getTasksForToday(new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                adapter.updateData(tasks);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(TasksActivity.this, "Error loading tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTaskDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Task title");

        new AlertDialog.Builder(this)
                .setTitle("New Task")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) return;

                    String currentTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date());

                    Task t = new Task(
                            UUID.randomUUID().toString(),
                            title,
                            repo.getCurrentUserId(),
                            currentTime,
                            false,
                            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    .format(new java.util.Date())
                    );
                    repo.addTask(t, new TaskRepository.OnTaskAddedListener() {
                        @Override public void onSuccess() { loadTasks(); }
                        @Override public void onError(Exception e) {
                            Toast.makeText(TasksActivity.this, "Error saving task", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(R.id.nav_tasks);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new android.content.Intent(this, com.abandiak.alerta.app.home.HomeActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_map) {
                startActivity(new android.content.Intent(this, com.abandiak.alerta.app.map.MapActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_tasks) {
                return true;

            } else if (id == R.id.nav_teams) {
                startActivity(new android.content.Intent(this, com.abandiak.alerta.app.teams.TeamsActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_more) {
                startActivity(new android.content.Intent(this, com.abandiak.alerta.app.more.MoreActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

}
