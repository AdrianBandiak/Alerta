package com.abandiak.alerta.util;

import com.abandiak.alerta.data.model.Task;
import com.abandiak.alerta.data.repository.TaskRepositoryInterface;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FakeTaskRepository implements TaskRepositoryInterface {

    private static List<Task> tasks = new ArrayList<>();

    public static void reset() {
        tasks.clear();
    }

    public static void setTasks(List<Task> list) {
        tasks = new ArrayList<>(list);
    }

    @Override
    public void getTasksForToday(OnTasksLoadedListener listener) {
        listener.onSuccess(new ArrayList<>(tasks));
    }

    @Override
    public ListenerRegistration listenForTodayTasks(OnTasksLoadedListener listener) {
        listener.onSuccess(new ArrayList<>(tasks));
        return null;
    }

    @Override
    public void addTask(Task task, OnTaskAddedListener listener) {
        tasks.add(task);
        listener.onSuccess();
    }

    @Override
    public void deleteTask(String id, OnTaskDeletedListener listener) {
        tasks.removeIf(t -> t.getId().equals(id));
        listener.onDeleted(true);
    }

    @Override
    public void updateTaskCompletion(String id, boolean completed) {
        for (Task t : tasks) {
            if (t.getId().equals(id)) {
                t.setCompleted(completed);
            }
        }
    }

    @Override
    public void updateTask(Task task, OnTaskUpdatedListener listener) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                listener.onUpdated(true);
                return;
            }
        }
        listener.onUpdated(false);
    }

    @Override
    public String getCurrentUserId() {
        return "TEST_USER";
    }
}
