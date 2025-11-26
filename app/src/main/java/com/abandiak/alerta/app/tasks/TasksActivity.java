package com.abandiak.alerta.app.tasks;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.messages.MessagesActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.abandiak.alerta.data.model.Task;
import com.abandiak.alerta.data.repository.TaskRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class TasksActivity extends BaseActivity {

    private TaskAdapter adapter;
    private TaskRepository repo;
    private RecyclerView recycler;
    private ListenerRegistration taskListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        SystemBars.apply(this);
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
                } else if (id == R.id.nav_messages) {
                    startActivity(new Intent(this, MessagesActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
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
        adapter.setOnTaskStatusChangedListener(() ->
                repo.getTasksForToday(new TaskRepository.OnTasksLoadedListener() {
                    @Override
                    public void onSuccess(List<Task> tasks) {
                        updateHeader(tasks);
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                })
        );


        FloatingActionButton fabAdd = findViewById(R.id.fabAddTask);
        fabAdd.setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
    }

    private void loadTasks() {
        if (taskListener != null) taskListener.remove();

        taskListener = repo.listenForTodayTasks(new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                updateHeader(tasks);
                if (tasks == null || tasks.isEmpty()) {
                    adapter.updateData(new ArrayList<>());
                    recycler.setVisibility(View.GONE);
                    findViewById(R.id.emptyState).setVisibility(View.VISIBLE);
                } else {
                    adapter.updateData(tasks);
                    recycler.setVisibility(View.VISIBLE);
                    findViewById(R.id.emptyState).setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                ToastUtils.show(TasksActivity.this, "Error loading tasks.");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskListener.remove();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getBooleanExtra("open_create_task", false)) {
            getIntent().removeExtra("open_create_task");
            showAddTaskDialog();
        }
    }

    private void updateHeader(List<Task> tasks) {
        TextView subtitle = findViewById(R.id.textHeaderSubtitle);

        if (tasks == null || tasks.isEmpty()) {
            subtitle.setText("0 in progress • 0 completed");
            return;
        }

        int inProgress = 0, done = 0;
        for (Task t : tasks) {
            if (t.isCompleted()) done++;
            else inProgress++;
        }

        subtitle.setText(String.format(Locale.getDefault(),
                "%d in progress • %d completed", inProgress, done));
    }

    private void showAddTaskDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_task, null);

        TextInputEditText inputTitle = dialogView.findViewById(R.id.inputTaskTitle);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.inputTaskDescription);
        AutoCompleteTextView inputPriority = dialogView.findViewById(R.id.inputTaskPriority);
        TextInputEditText inputStartDate = dialogView.findViewById(R.id.inputTaskStartDate);
        TextInputEditText inputEndDate = dialogView.findViewById(R.id.inputTaskEndDate);
        AutoCompleteTextView inputType = dialogView.findViewById(R.id.inputTaskType);
        AutoCompleteTextView inputTeam = dialogView.findViewById(R.id.inputTaskTeam);
        TextInputLayout layoutSelectTeam = dialogView.findViewById(R.id.layoutSelectTeam);

        inputTeam.setDropDownBackgroundResource(R.drawable.bg_dropdown_alerta);

        String[] priorities = getResources().getStringArray(R.array.task_priorities);

        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                priorities
        );

        inputPriority.setAdapter(priorityAdapter);
        inputPriority.setDropDownBackgroundResource(R.drawable.bg_dropdown_alerta);
        inputPriority.setOnClickListener(v -> inputPriority.showDropDown());


        String[] types = {"NORMAL", "TEAM"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                types
        );
        inputType.setAdapter(typeAdapter);
        inputType.setDropDownBackgroundResource(R.drawable.bg_dropdown_alerta);
        inputType.setOnClickListener(v -> inputType.showDropDown());

        inputType.setOnItemClickListener((parent, view, position, id) -> {
            if ("TEAM".equals(types[position])) {
                layoutSelectTeam.setVisibility(View.VISIBLE);
                loadTeamsForUser(inputTeam);
            } else {
                layoutSelectTeam.setVisibility(View.GONE);
                inputTeam.setText("");
            }
        });

        inputTeam.setOnClickListener(v -> inputTeam.showDropDown());


        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());
        inputStartDate.setText(today);

        inputEndDate.setOnClickListener(v -> {

            String start = Objects.requireNonNull(inputStartDate.getText()).toString().trim();
            if (start.isEmpty()) {
                ToastUtils.show(this, "Select start date first");
                return;
            }

            Calendar startCal = Calendar.getInstance();
            try {
                String[] parts = start.split("-");
                startCal.set(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2]),
                        0, 0, 0
                );
            } catch (Exception e) {
                ToastUtils.show(this, "Invalid start date");
                return;
            }

            Calendar now = Calendar.getInstance();

            DatePickerDialog dp = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                year, month + 1, day);
                        inputEndDate.setText(date);
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );

            dp.getDatePicker().setMinDate(startCal.getTimeInMillis());

            dp.show();
        });


        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        dialogView.findViewById(R.id.btnCreate).setOnClickListener(v -> {

            String title = Objects.requireNonNull(inputTitle.getText()).toString().trim();
            String desc = Objects.requireNonNull(inputDesc.getText()).toString().trim();
            String priority = inputPriority.getText().toString().trim();
            String startDate = Objects.requireNonNull(inputStartDate.getText()).toString().trim();
            String endDate = Objects.requireNonNull(inputEndDate.getText()).toString().trim();
            if (!endDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    long startMillis = Objects.requireNonNull(sdf.parse(startDate)).getTime();
                    long endMillis = Objects.requireNonNull(sdf.parse(endDate)).getTime();

                    if (endMillis < startMillis) {
                        ToastUtils.show(this, "End date cannot be earlier than start date");
                        return;
                    }

                } catch (Exception ignored) {
                    ToastUtils.show(this, "Invalid date format");
                    return;
                }
            }

            String type = inputType.getText().toString().trim();
            String teamName = inputTeam.getText().toString().trim();

            if (title.isEmpty()) {
                inputTitle.setError("Title required");
                return;
            }

            if (type.isEmpty()) {
                ToastUtils.show(this, "Select task type");
                return;
            }

            if (type.equals("TEAM") && teamName.isEmpty()) {
                ToastUtils.show(this, "Select a team");
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
            task.setType(type);

            if (type.equals("TEAM")) {
                assignTeamToTask(task, teamName, dialog);
            } else {
                saveTask(task, dialog);
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }



    private void assignTeamToTask(Task task, String teamName, AlertDialog dialog) {
        FirebaseFirestore.getInstance()
                .collection("teams")
                .whereEqualTo("name", teamName)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        ToastUtils.show(this, "Team not found");
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);

                    task.setTeamId(doc.getId());
                    Long c = doc.getLong("color");
                    task.setTeamColor(c != null ? c.intValue() : null);

                    saveTask(task, dialog);
                })
                .addOnFailureListener(e -> ToastUtils.show(this, "Error loading team"));
    }


    private void saveTask(Task task, AlertDialog dialog) {
        repo.addTask(task, new TaskRepository.OnTaskAddedListener() {
            @Override
            public void onSuccess() {
                ToastUtils.show(TasksActivity.this, "Task added.");
                dialog.dismiss();
            }

            @Override
            public void onError(Exception e) {
                ToastUtils.show(TasksActivity.this, "Error saving task.");
            }
        });
    }


    private void loadTeamsForUser(AutoCompleteTextView inputTeam) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("teams")
                .whereArrayContains("membersIndex", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot d : snap) {
                        String name = d.getString("name");
                        if (name != null) names.add(name);
                    }
                    ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(
                            this,
                            R.layout.list_item_dropdown,
                            names
                    );
                    inputTeam.setAdapter(teamAdapter);
                });
    }

    @SuppressLint("SetTextI18n")
    private void showEditTaskDialog(Task task) {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_task, null);

        TextInputEditText inputTitle = dialogView.findViewById(R.id.inputTaskTitle);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.inputTaskDescription);
        AutoCompleteTextView inputPriority = dialogView.findViewById(R.id.inputTaskPriority);
        TextInputEditText inputStartDate = dialogView.findViewById(R.id.inputTaskStartDate);
        TextInputEditText inputEndDate = dialogView.findViewById(R.id.inputTaskEndDate);
        AutoCompleteTextView inputType = dialogView.findViewById(R.id.inputTaskType);
        AutoCompleteTextView inputTeam = dialogView.findViewById(R.id.inputTaskTeam);
        TextInputLayout layoutTeam = dialogView.findViewById(R.id.layoutSelectTeam);

        inputTeam.setDropDownBackgroundResource(R.drawable.bg_dropdown_alerta);

        inputTitle.setText(task.getTitle());
        inputDesc.setText(task.getDescription());
        inputPriority.setText(task.getPriority());
        inputStartDate.setText(task.getDate());
        inputEndDate.setText(task.getEndDate());
        inputType.setText(task.getType());

        String[] priorities = getResources().getStringArray(R.array.task_priorities);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                priorities
        );
        inputPriority.setAdapter(priorityAdapter);
        inputPriority.setDropDownBackgroundResource(R.drawable.bg_dropdown_alerta);
        inputPriority.setOnClickListener(v -> inputPriority.showDropDown());

        String[] types = {"NORMAL", "TEAM"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                types
        );
        inputType.setAdapter(typeAdapter);
        inputType.setDropDownBackgroundResource(R.drawable.bg_dropdown_alerta);
        inputType.setOnClickListener(v -> inputType.showDropDown());

        if ("TEAM".equals(task.getType())) {
            layoutTeam.setVisibility(View.VISIBLE);

            loadTeamsForUser(inputTeam);
            inputTeam.setText(getTeamNameById(task.getTeamId()));
        }

        inputType.setOnItemClickListener((parent, view, pos, id) -> {
            if (types[pos].equals("TEAM")) {
                layoutTeam.setVisibility(View.VISIBLE);
                loadTeamsForUser(inputTeam);
            } else {
                layoutTeam.setVisibility(View.GONE);
                inputTeam.setText("");
                task.setTeamId(null);
                task.setTeamColor(null);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        dialogView.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String newTitle = inputTitle.getText().toString().trim();
            String newDesc = inputDesc.getText().toString().trim();
            String newPriority = inputPriority.getText().toString().trim();
            String newStart = inputStartDate.getText().toString().trim();
            String newEnd = inputEndDate.getText().toString().trim();
            String newType = inputType.getText().toString().trim();
            String newTeam = inputTeam.getText().toString().trim();

            if (newTitle.isEmpty()) {
                inputTitle.setError("Title required");
                return;
            }

            task.setTitle(newTitle);
            task.setDescription(newDesc);
            task.setPriority(newPriority);
            task.setDate(newStart);
            task.setEndDate(newEnd);
            task.setType(newType);

            if (newType.equals("TEAM")) {
                assignTeamToTask(task, newTeam, dialog);
            } else {
                task.setTeamId(null);
                task.setTeamColor(null);
                repo.updateTask(task, success -> dialog.dismiss());
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private String getTeamNameById(String teamId) {
        return "";
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
        TextView type = dialogView.findViewById(R.id.textTaskType);

        type.setText("Type: " + task.getType());
        type.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        title.setText(task.getTitle());
        desc.setText(task.getDescription() == null ? "No description" : task.getDescription());
        priority.setText("Priority: " + (task.getPriority() == null ? "Normal" : task.getPriority()));

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.US);

        try {
            start.setText("Start date: " + out.format(Objects.requireNonNull(input.parse(task.getDate()))));
        } catch (Exception ignored) {}
        try {
            end.setText("End date: " + out.format(Objects.requireNonNull(input.parse(task.getEndDate()))));
        } catch (Exception ignored) {}

        created.setText("Created at: " + task.getTime());

        if (task.isCompleted()) {
            status.setText("Status: Completed");
            status.setTextColor(ContextCompat.getColor(this, R.color.status_online_bg));
        } else {
            status.setText("Status: In progress");
            status.setTextColor(ContextCompat.getColor(this, R.color.alerta_primary));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            dialog.dismiss();
            showEditTaskDialog(task);
        });

        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {

            View deleteView = getLayoutInflater().inflate(R.layout.dialog_delete_task, null);

            AlertDialog deleteDialog = new AlertDialog.Builder(this)
                    .setView(deleteView)
                    .setCancelable(true)
                    .create();

            deleteView.findViewById(R.id.btnCancel).setOnClickListener(x -> deleteDialog.dismiss());

            deleteView.findViewById(R.id.btnConfirm).setOnClickListener(x -> repo.deleteTask(task.getId(), success -> {
                ToastUtils.show(this, "Task deleted");
                deleteDialog.dismiss();
                dialog.dismiss();
            }));

            deleteDialog.show();
        });

        dialog.show();
    }

}
