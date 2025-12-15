package com.abandiak.alerta.repositories;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abandiak.alerta.data.model.Task;
import com.abandiak.alerta.data.repository.TaskRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.text.SimpleDateFormat;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
public class TaskRepositoryTest {

    private FirebaseFirestore mockDb;
    private CollectionReference mockTasks;
    private Query mockQuery;
    private DocumentReference mockTaskDoc;

    private Query mockDateQuery;

    private MockedStatic<android.util.Log> logStatic;

    private TaskRepository repo;

    @Before
    public void setup() {

        mockDb = mock(FirebaseFirestore.class);
        mockTasks = mock(CollectionReference.class);
        mockQuery = mock(Query.class);
        mockTaskDoc = mock(DocumentReference.class);
        mockDateQuery = mock(Query.class);

        when(mockDb.collection("tasks")).thenReturn(mockTasks);
        when(mockTasks.document(anyString())).thenReturn(mockTaskDoc);

        when(mockTasks.whereEqualTo(eq("createdBy"), anyString())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(eq("date"), anyString())).thenReturn(mockQuery);

        when(mockTasks.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);

        when(mockQuery.get()).thenReturn(Tasks.forResult(mock(QuerySnapshot.class)));
        when(mockQuery.addSnapshotListener(any(EventListener.class)))
                .thenReturn(() -> {});

        when(mockDb.collection("tasks").whereEqualTo(eq("date"), anyString()))
                .thenReturn(mockDateQuery);

        when(mockDateQuery.addSnapshotListener(any(EventListener.class)))
                .thenReturn(() -> {});

        when(mockTaskDoc.set(any(), any(SetOptions.class)))
                .thenReturn(Tasks.forResult(null));

        when(mockTaskDoc.update(anyString(), any()))
                .thenReturn(Tasks.forResult(null));

        when(mockTaskDoc.delete())
                .thenReturn(Tasks.forResult(null));

        logStatic = mockStatic(android.util.Log.class);
        logStatic.when(() -> android.util.Log.d(anyString(), anyString())).thenReturn(0);
        logStatic.when(() -> android.util.Log.e(anyString(), anyString())).thenReturn(0);

        repo = new TaskRepository(mockDb, "user123");
    }

    @After
    public void tearDown() {
        logStatic.close();
    }

    private void runLooper() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @Test
    public void addTask_success_callsOnSuccess() {

        TaskRepository.OnTaskAddedListener listener = mock(TaskRepository.OnTaskAddedListener.class);

        Task t = new Task("T1", "Test", "user123", "10:00", false, "2024");

        repo.addTask(t, listener);
        runLooper();

        verify(mockTaskDoc).set(eq(t), any(SetOptions.class));
        verify(listener).onSuccess();
    }

    @Test
    public void addTask_failure_callsOnError() {

        when(mockTaskDoc.set(any(), any(SetOptions.class)))
                .thenReturn(Tasks.forException(new Exception()));

        TaskRepository.OnTaskAddedListener listener = mock(TaskRepository.OnTaskAddedListener.class);
        Task t = new Task("T1", "Test", "user123", "10:00", false, "2024");

        repo.addTask(t, listener);
        runLooper();

        verify(listener).onError(any());
    }

    @Test
    public void getTasksForToday_success() {

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(mockQuery.get()).thenReturn(Tasks.forResult(snap));

        List<Task> list = List.of(new Task());
        when(snap.toObjects(Task.class)).thenReturn(list);

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        repo.getTasksForToday(listener);
        runLooper();

        verify(listener).onSuccess(list);
    }

    @Test
    public void getTasksForToday_failure() {

        when(mockQuery.get()).thenReturn(Tasks.forException(new Exception()));

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        repo.getTasksForToday(listener);
        runLooper();

        verify(listener).onError(any());
    }

    @Test
    public void getTasksForToday_nullSnapshot_callsOnError() {

        when(mockQuery.get()).thenReturn(Tasks.forResult(null));

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        repo.getTasksForToday(listener);
        runLooper();

        verify(listener).onError(any());
    }

    @Test
    public void listenForTodayTasks_success() {

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        doAnswer(inv -> {
            EventListener<QuerySnapshot> cb = inv.getArgument(0);

            QuerySnapshot snap = mock(QuerySnapshot.class);
            when(snap.toObjects(Task.class)).thenReturn(List.of(new Task()));

            cb.onEvent(snap, null);

            return (ListenerRegistration) ()-> {};
        }).when(mockQuery).addSnapshotListener(any());

        repo.listenForTodayTasks(listener);
        runLooper();

        verify(listener).onSuccess(any());
    }

    @Test
    public void listenForTodayTasks_error() {

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        doAnswer(inv -> {
            EventListener<QuerySnapshot> cb = inv.getArgument(0);

            cb.onEvent(null, new FirebaseFirestoreException("ERR",
                    FirebaseFirestoreException.Code.UNKNOWN));

            return (ListenerRegistration) ()-> {};
        }).when(mockQuery).addSnapshotListener(any());

        repo.listenForTodayTasks(listener);
        runLooper();

        verify(listener).onError(any());
    }

    @Test
    public void listenForTodayTasks_nullSnapshot_doesNothing() {

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        doAnswer(inv -> {
            EventListener<QuerySnapshot> cb = inv.getArgument(0);

            cb.onEvent(null, null);

            return (ListenerRegistration) () -> {};
        }).when(mockQuery).addSnapshotListener(any());

        repo.listenForTodayTasks(listener);
        runLooper();

        verify(listener, never()).onSuccess(any());
        verify(listener, never()).onError(any());
    }


    @Test
    public void updateCompletion_ok() {

        repo.updateTaskCompletion("X1", true);
        runLooper();

        verify(mockTaskDoc).update(eq("completed"), eq(true));
    }

    @Test
    public void updateTask_success() {

        Task t = new Task("T1", "AA", "user123", "1", false, "2024");

        TaskRepository.OnTaskUpdatedListener listener = mock(TaskRepository.OnTaskUpdatedListener.class);

        repo.updateTask(t, listener);
        runLooper();

        verify(listener).onUpdated(true);
    }

    @Test
    public void updateTask_failure() {

        when(mockTaskDoc.set(any(), any(SetOptions.class)))
                .thenReturn(Tasks.forException(new Exception()));

        Task t = new Task("T1", "AA", "user123", "1", false, "2024");

        TaskRepository.OnTaskUpdatedListener listener = mock(TaskRepository.OnTaskUpdatedListener.class);

        repo.updateTask(t, listener);
        runLooper();

        verify(listener).onUpdated(false);
    }

    @Test
    public void delete_success() {

        TaskRepository.OnTaskDeletedListener l = mock(TaskRepository.OnTaskDeletedListener.class);

        repo.deleteTask("ID1", l);
        runLooper();

        verify(l).onDeleted(true);
    }

    @Test
    public void delete_failure() {

        when(mockTaskDoc.delete()).thenReturn(Tasks.forException(new Exception()));

        TaskRepository.OnTaskDeletedListener l = mock(TaskRepository.OnTaskDeletedListener.class);

        repo.deleteTask("ID1", l);
        runLooper();

        verify(l).onDeleted(false);
    }

    @Test
    public void listenForTodayTasksIncludingTeams_filtersCorrectly() {

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        List<String> teams = List.of("TEAM1");

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        QueryDocumentSnapshot d1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot d2 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot d3 = mock(QueryDocumentSnapshot.class);

        Task mine = new Task();
        mine.setCreatedBy("user123");
        mine.setDate(today);

        Task team = new Task();
        team.setTeamId("TEAM1");

        Task ignored = new Task();
        ignored.setCreatedBy("NOPE");

        when(d1.toObject(Task.class)).thenReturn(mine);
        when(d2.toObject(Task.class)).thenReturn(team);
        when(d3.toObject(Task.class)).thenReturn(ignored);

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.iterator()).thenReturn(List.of(d1, d2, d3).iterator());

        doAnswer(inv -> {
            EventListener<QuerySnapshot> cb = inv.getArgument(0);
            cb.onEvent(snap, null);
            return (ListenerRegistration) () -> {};
        }).when(mockDateQuery).addSnapshotListener(any(EventListener.class));

        repo.listenForTodayTasksIncludingTeams(teams, listener);
        runLooper();

        verify(listener).onSuccess(argThat(list ->
                list.size() == 2 &&
                        list.stream().anyMatch(t -> "user123".equals(t.getCreatedBy())) &&
                        list.stream().anyMatch(t -> "TEAM1".equals(t.getTeamId()))
        ));
    }

    @Test
    public void listenForTodayTasksIncludingTeams_skipsNullDocument() {

        TaskRepository.OnTasksLoadedListener listener = mock(TaskRepository.OnTasksLoadedListener.class);

        QueryDocumentSnapshot d1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot d2 = mock(QueryDocumentSnapshot.class);

        when(d1.toObject(Task.class)).thenReturn(null);
        when(d2.toObject(Task.class)).thenReturn(new Task());

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.iterator()).thenReturn(List.of(d1, d2).iterator());

        doAnswer(inv -> {
            EventListener<QuerySnapshot> cb = inv.getArgument(0);
            cb.onEvent(snap, null);
            return (ListenerRegistration) () -> {};
        }).when(mockDateQuery).addSnapshotListener(any());

        repo.listenForTodayTasksIncludingTeams(Collections.emptyList(), listener);
        runLooper();

        verify(listener).onSuccess(argThat(List::isEmpty));
    }
}
