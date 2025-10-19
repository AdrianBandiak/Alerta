package com.abandiak.alerta.data.repository;

import com.abandiak.alerta.data.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Date;
import java.text.SimpleDateFormat;

public class TaskRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference tasksRef = db.collection("tasks");
    private final String currentUserId = FirebaseAuth.getInstance().getUid();

    public void addTask(Task task, OnTaskAddedListener listener) {
        tasksRef.document(task.getId()).set(task)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void getTasksForToday(OnTasksLoadedListener listener) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        tasksRef.whereEqualTo("createdBy", currentUserId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snapshots -> listener.onSuccess(snapshots.toObjects(Task.class)))
                .addOnFailureListener(listener::onError);
    }

    public interface OnTaskAddedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnTasksLoadedListener {
        void onSuccess(java.util.List<Task> tasks);
        void onError(Exception e);
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

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
                        listener.onSuccess(snapshots.toObjects(Task.class));
                    }
                });
    }

}
