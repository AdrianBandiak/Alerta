package com.abandiak.alerta.ui.tasks;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static org.hamcrest.Matchers.not;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.RootMatchers;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.tasks.TaskAdapter;
import com.abandiak.alerta.data.model.Task;
import com.abandiak.alerta.util.FakeTaskRepository;
import com.abandiak.alerta.util.ToastMatcher;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TasksActivityTest {

    @Before
    public void setup() {
        FakeTaskRepository.reset();
        TasksActivity.repoOverride = new FakeTaskRepository();
        TaskAdapter.repoOverride = TasksActivity.repoOverride;
    }


    private ActivityScenario<TasksActivity> launch() {
        return ActivityScenario.launch(TasksActivity.class);
    }

    @Test
    public void testEmptyStateShown() {
        FakeTaskRepository.setTasks(new ArrayList<>());

        launch();

        onView(withId(R.id.emptyState)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerTasks)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.textHeaderSubtitle))
                .check(matches(withText("0 in progress • 0 completed")));
    }

    @Test
    public void testTasksVisible() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("A1", "Test Task 1", "TEST", "12:00", false, "2025-01-01"));
        tasks.add(new Task("A2", "Task 2", "TEST", "12:10", true, "2025-01-01"));

        FakeTaskRepository.setTasks(tasks);

        launch();

        onView(withId(R.id.recyclerTasks)).check(matches(isDisplayed()));
        onView(withId(R.id.emptyState)).check(matches(withEffectiveVisibility(Visibility.GONE)));

        onView(withText("[#A1] Test Task 1")).check(matches(isDisplayed()));
        onView(withText("[#A2] Task 2")).check(matches(isDisplayed()));

        onView(withId(R.id.textHeaderSubtitle))
                .check(matches(withText("1 in progress • 1 completed")));
    }

    @Test
    public void testOpenCreateTaskDialog() {
        launch();

        SystemClock.sleep(300);
        onView(withId(R.id.fabAddTask)).perform(click());

        onView(withId(R.id.inputTaskTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCreate)).check(matches(isDisplayed()));
    }

    @Test
    public void testValidationMissingTitle() {
        launch();

        SystemClock.sleep(300);
        onView(withId(R.id.fabAddTask)).perform(click());

        SystemClock.sleep(200);

        onView(withId(R.id.btnCreate)).perform(click());

        onView(withId(R.id.inputTaskTitle))
                .check(matches(hasErrorText("Title required")));
    }

    @Test
    public void testValidationMissingType() {
        launch();

        onView(withId(R.id.fabAddTask)).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.inputTaskTitle)).perform(typeText("Sample"), closeSoftKeyboard());

        onView(withId(R.id.btnCreate)).perform(click());

        onView(withText("Select task type"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSuccessfulTaskCreation() {
        launch();

        onView(withId(R.id.fabAddTask)).perform(click());
        SystemClock.sleep(300);

        onView(withId(R.id.inputTaskTitle))
                .perform(typeText("New Task"), closeSoftKeyboard());

        onView(withId(R.id.inputTaskType)).perform(click());
        SystemClock.sleep(300);

        onView(withText("NORMAL"))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        onView(withId(R.id.btnCreate)).perform(click());

        onView(withText("Task added."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testOpenTaskDetailsDialog() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("T1", "DetailTest", "UID", "10:00", false, "2025-01-01"));
        FakeTaskRepository.setTasks(tasks);

        launch();

        onView(withText("[#T1] DetailTest")).perform(click());

        onView(withId(R.id.textTaskTitle)).check(matches(withText("DetailTest")));
        onView(withId(R.id.textStatus)).check(matches(isDisplayed()));
    }

    @Test
    public void testDeleteTask() {
        Task t = new Task("DEL1", "DeleteMe", "U", "11:00", false, "2025-01-01");
        List<Task> tasks = new ArrayList<>();
        tasks.add(t);
        FakeTaskRepository.setTasks(tasks);

        launch();

        onView(withText("[#DEL1] DeleteMe")).perform(click());
        onView(withId(R.id.btnDelete)).perform(click());

        SystemClock.sleep(200);

        onView(withId(R.id.btnConfirm)).perform(click());

        onView(withText("Task deleted"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
