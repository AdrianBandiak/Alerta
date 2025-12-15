package com.abandiak.alerta.data.repository;

import android.util.Log;

import com.abandiak.alerta.data.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskRepository implements TaskRepositoryInterface {

    private static final String TAG = "TaskRepository";

    private final FirebaseFirestore db;
    private final CollectionReference tasksRef;
    private final String currentUserId;

    public TaskRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance().getUid());
    }

    public TaskRepository(FirebaseFirestore db, String uid) {
        this.db = db;
        this.tasksRef = db.collection("tasks");
        this.currentUserId = uid;
    }

    @Override
    public void addTask(Task task, OnTaskAddedListener listener) {

        Log.d(TAG, "Adding task: id=" + task.getId()
                + ", type=" + task.getType()
                + ", teamId=" + task.getTeamId()
                + ", teamColor=" + task.getTeamColor());

        tasksRef.document(task.getId())
                .set(task, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add task", e);
                    listener.onError(e);
                });
    }

    @Override
    public void getTasksForToday(OnTasksLoadedListener listener) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        tasksRef.whereEqualTo("createdBy", currentUserId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null) {
                        listener.onError(new Exception("Null snapshot"));
                        return;
                    }
                    List<Task> list = snapshots.toObjects(Task.class);
                    listener.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load tasks for today", e);
                    listener.onError(e);
                });
    }

    @Override
    public ListenerRegistration listenForTodayTasks(OnTasksLoadedListener listener) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        return tasksRef.whereEqualTo("createdBy", currentUserId)
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (snapshots != null) {
                        List<Task> list = snapshots.toObjects(Task.class);
                        listener.onSuccess(list);
                    }
                });
    }

    @Override
    public void updateTaskCompletion(String taskId, boolean completed) {
        tasksRef.document(taskId)
                .update("completed", completed)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update completion", e));
    }

    public void updateTask(Task task, TaskRepositoryInterface.OnTaskUpdatedListener listener) {

        Log.d(TAG, "Updating task: id=" + task.getId()
                + ", type=" + task.getType()
                + ", teamId=" + task.getTeamId());

        tasksRef.document(task.getId())
                .set(task, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onUpdated(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task", e);
                    listener.onUpdated(false);
                });
    }

    @Override
    public void deleteTask(String id, OnTaskDeletedListener listener) {

        Log.d(TAG, "Trying to delete task: " + id);

        tasksRef.document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted successfully: " + id);
                    listener.onDeleted(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete failed: " + id, e);
                    listener.onDeleted(false);
                });
    }

    public ListenerRegistration listenForTodayTasksIncludingTeams(
            List<String> userTeams,
            OnTasksLoadedListener listener) {

        String uid = getCurrentUserId();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        return db.collection("tasks")
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshots, e) -> {

                    if (e != null) {
                        listener.onError(e);
                        return;
                    }

                    if (snapshots == null) {
                        listener.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Task> result = new ArrayList<>();

                    for (DocumentSnapshot d : snapshots) {
                        Task t = d.toObject(Task.class);
                        if (t == null) continue;

                        boolean isMine = uid.equals(t.getCreatedBy());
                        boolean isTeamTask = t.getTeamId() != null &&
                                userTeams.contains(t.getTeamId());
                        boolean isToday = today.equals(t.getDate());

                        if (isMine && isToday) {
                            result.add(t);
                        }

                        if (isTeamTask) {
                            if (t.getDate() == null) t.setDate(today);
                            result.add(t);
                        }
                    }

                    listener.onSuccess(result);
                });
    }

    @Override
    public String getCurrentUserId() {
        return currentUserId;
    }
}
