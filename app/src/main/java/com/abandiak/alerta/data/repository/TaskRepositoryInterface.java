package com.abandiak.alerta.data.repository;

import com.abandiak.alerta.data.model.Task;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public interface TaskRepositoryInterface {

    void getTasksForToday(OnTasksLoadedListener listener);

    ListenerRegistration listenForTodayTasks(OnTasksLoadedListener listener);

    void addTask(Task task, OnTaskAddedListener listener);

    void deleteTask(String id, OnTaskDeletedListener listener);

    void updateTaskCompletion(String id, boolean completed);

    void updateTask(Task task, OnTaskUpdatedListener listener);

    String getCurrentUserId();

    interface OnTaskAddedListener {
        void onSuccess();
        void onError(Exception e);
    }

    interface OnTasksLoadedListener {
        void onSuccess(List<Task> tasks);
        void onError(Exception e);
    }

    interface OnTaskDeletedListener {
        void onDeleted(boolean success);
    }

    interface OnTaskUpdatedListener {
        void onUpdated(boolean success);
    }
}
