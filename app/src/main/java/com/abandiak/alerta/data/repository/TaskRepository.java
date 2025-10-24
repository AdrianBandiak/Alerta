package com.abandiak.alerta.data.repository;

import android.util.Log;

import com.abandiak.alerta.data.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TaskRepository {

    private static final String TAG = "TaskRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference tasksRef = db.collection("tasks");
    private final String currentUserId = FirebaseAuth.getInstance().getUid();


    public void addTask(Task task, OnTaskAddedListener listener) {
        tasksRef.document(task.getId())
                .set(task)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add task", e);
                    listener.onError(e);
                });
    }


    public void getTasksForToday(OnTasksLoadedListener listener) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        tasksRef.whereEqualTo("createdBy", currentUserId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snapshots ->
                        listener.onSuccess(snapshots.toObjects(Task.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load tasks for today", e);
                    listener.onError(e);
                });
    }

    public ListenerRegistration listenForTodayTasks(OnTasksLoadedListener listener) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        return tasksRef.whereEqualTo("createdBy", currentUserId)
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Snapshot listener error", e);
                        listener.onError(e);
                        return;
                    }
                    if (snapshots != null) {
                        listener.onSuccess(snapshots.toObjects(Task.class));
                    }
                });
    }


    public void updateTaskCompletion(String taskId, boolean completed) {
        tasksRef.document(taskId)
                .update("completed", completed)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update completion", e));
    }

    public void updateTask(Task task, OnTaskUpdatedListener listener) {
        tasksRef.document(task.getId())
                .set(task, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onUpdated(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task", e);
                    listener.onUpdated(false);
                });
    }


    public void deleteTask(String id, OnTaskDeletedListener listener) {
        Log.d("TASK_DELETE", "Trying to delete task: " + id);
        tasksRef.document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("TASK_DELETE", "Deleted successfully: " + id);
                    listener.onDeleted(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("TASK_DELETE", "Delete failed: " + id, e);
                    listener.onDeleted(false);
                });
    }



    public String getCurrentUserId() {
        return currentUserId;
    }


    public interface OnTaskAddedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnTasksLoadedListener {
        void onSuccess(List<Task> tasks);
        void onError(Exception e);
    }

    public interface OnTaskUpdatedListener {
        void onUpdated(boolean success);
    }

    public interface OnTaskDeletedListener {
        void onDeleted(boolean success);
    }
}
