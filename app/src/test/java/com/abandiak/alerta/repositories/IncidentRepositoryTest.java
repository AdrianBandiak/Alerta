package com.abandiak.alerta.repositories;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abandiak.alerta.data.model.Incident;
import com.abandiak.alerta.data.repository.IncidentRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Method;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
public class IncidentRepositoryTest {

    private FirebaseFirestore mockDb;
    private CollectionReference mockIncidents;
    private DocumentReference mockIncidentDoc;

    private CollectionReference mockTeams;
    private Query mockTeamsQuery;

    private MockedStatic<FirebaseAuth> authStatic;
    private MockedStatic<android.util.Log> logStatic;

    private IncidentRepository repo;

    @Before
    public void setup() {

        mockDb = mock(FirebaseFirestore.class);
        mockIncidents = mock(CollectionReference.class);
        mockIncidentDoc = mock(DocumentReference.class);

        mockTeams = mock(CollectionReference.class);
        mockTeamsQuery = mock(Query.class);

        when(mockDb.collection("incidents")).thenReturn(mockIncidents);
        when(mockIncidents.document(anyString())).thenReturn(mockIncidentDoc);

        when(mockDb.collection("teams")).thenReturn(mockTeams);
        when(mockTeams.whereArrayContains(anyString(), any())).thenReturn(mockTeamsQuery);

        logStatic = mockStatic(android.util.Log.class);
        logStatic.when(() -> android.util.Log.d(anyString(), anyString())).thenReturn(0);
        logStatic.when(() -> android.util.Log.w(anyString(), anyString())).thenReturn(0);
        logStatic.when(() -> android.util.Log.e(anyString(), anyString())).thenReturn(0);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        FirebaseUser mockUser = mock(FirebaseUser.class);

        when(mockUser.getUid()).thenReturn("userA");
        when(mockAuth.getUid()).thenReturn("userA");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        authStatic = mockStatic(FirebaseAuth.class);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        when(mockIncidentDoc.update(anyMap())).thenReturn(Tasks.forResult(null));
        when(mockIncidentDoc.update(eq("logs"), any())).thenReturn(Tasks.forResult(null));

        repo = new IncidentRepository(mockDb);
    }

    @After
    public void cleanup() {
        authStatic.close();
        logStatic.close();
    }

    private void runLooper() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @Test
    public void createIncident_withMap_callsAdd() {

        when(mockIncidents.add(anyMap()))
                .thenReturn(Tasks.forResult(mockIncidentDoc));

        Map<String, Object> data = Map.of("title", "Fire");

        repo.createIncident(data);
        runLooper();

        verify(mockIncidents).add(argThat(
                (Map<String, Object> m) -> "Fire".equals(m.get("title"))
        ));
    }

    @Test
    public void createIncident_withObject_callsAdd() {

        when(mockIncidents.add(anyMap()))
                .thenReturn(Tasks.forResult(mockIncidentDoc));

        Incident inc = new Incident("T", "D", "Type", 1.0, 2.0, "Region", "userA");

        repo.createIncident(inc);
        runLooper();

        verify(mockIncidents).add(anyMap());
    }

    @Test
    public void updateIncidentVerification_verifiedTrue_updatesFieldsAndLogs() {

        repo.updateIncidentVerification("ID123", true);
        runLooper();

        verify(mockIncidentDoc).update(argThat(
                (Map<String, Object> m) ->
                        Boolean.TRUE.equals(m.get("verified")) &&
                                "userA".equals(m.get("verifiedBy"))
        ));

        verify(mockIncidentDoc).update(eq("logs"), any());
    }

    @Test
    public void updateIncidentVerification_verifiedFalse_setsVerifiedByNull() {

        repo.updateIncidentVerification("ID123", false);
        runLooper();

        verify(mockIncidentDoc).update(argThat(
                (Map<String, Object> m) ->
                        Boolean.FALSE.equals(m.get("verified")) &&
                                m.get("verifiedBy") == null
        ));

        verify(mockIncidentDoc).update(eq("logs"), any());
    }

    @Test
    public void updateIncidentVerification_failureStopsSecondUpdate() {

        when(mockIncidentDoc.update(anyMap()))
                .thenReturn(Tasks.forException(new Exception("fail")));

        repo.updateIncidentVerification("ID123", true);
        runLooper();

        verify(mockIncidentDoc, never()).update(eq("logs"), any());
    }

    @Test
    public void addIncidentLog_addsLogToArray() {
        repo.addIncidentLog("ID123", "ACTION");
        runLooper();

        verify(mockIncidentDoc).update(eq("logs"), any());
    }

    @Test
    public void deleteIncident_callsDelete() {

        when(mockIncidentDoc.delete()).thenReturn(Tasks.forResult(null));

        repo.deleteIncident("ID123");
        runLooper();

        verify(mockIncidentDoc).delete();
    }

    private Query mockQueryChain() {
        Query q = mock(Query.class);
        when(q.orderBy(anyString(), any())).thenReturn(q);
        when(q.limit(anyLong())).thenReturn(q);
        when(q.whereEqualTo(anyString(), any())).thenReturn(q);
        when(q.addSnapshotListener(any())).thenReturn(() -> {});
        return q;
    }

    @Test
    public void listenVisibleIncidents_userNull_usesPublicOnly() {

        authStatic.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
        when(FirebaseAuth.getInstance().getUid()).thenReturn(null);

        Query q = mockQueryChain();
        when(mockIncidents.whereArrayContainsAny(eq("aud"), anyList())).thenReturn(q);

        repo.listenVisibleIncidentsForCurrentUser("TYPE", "RB", "REG", mock(EventListener.class));
        runLooper();

        verify(mockIncidents).whereArrayContainsAny(eq("aud"),
                argThat(list -> list.contains("public")));
    }

    @Test
    public void listenVisibleIncidents_teamFetchSuccess_usesTeamTokens() {

        DocumentSnapshot d1 = mock(DocumentSnapshot.class);
        when(d1.getId()).thenReturn("TEAM1");

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.getDocuments()).thenReturn(List.of(d1));

        when(mockTeamsQuery.get()).thenReturn(Tasks.forResult(snap));

        Query q = mockQueryChain();
        when(mockIncidents.whereArrayContainsAny(eq("aud"), anyList())).thenReturn(q);

        repo.listenVisibleIncidentsForCurrentUser("TYPE", "RB", "REG", mock(EventListener.class));
        runLooper();

        verify(mockIncidents).whereArrayContainsAny(eq("aud"),
                argThat(list ->
                        list.contains("public") &&
                                list.contains("userA") &&
                                list.contains("TEAM1")));
    }

    @Test
    public void listenVisibleIncidents_teamFetchFails_fallbackToUserOnly() {

        when(mockTeamsQuery.get()).thenReturn(Tasks.forException(new Exception()));

        Query q = mockQueryChain();
        when(mockIncidents.whereArrayContainsAny(eq("aud"), anyList())).thenReturn(q);

        repo.listenVisibleIncidentsForCurrentUser("TYPE", "RB", "REG", mock(EventListener.class));
        runLooper();

        verify(mockIncidents).whereArrayContainsAny(eq("aud"),
                argThat(list ->
                        list.contains("public") &&
                                list.contains("userA")));
    }

    @Test
    public void listenVisibleIncidents_typeAll_doesNotApplyTypeFilter() {

        Query q = mockQueryChain();
        when(mockTeamsQuery.get()).thenReturn(Tasks.forResult(mock(QuerySnapshot.class)));
        when(mockIncidents.whereArrayContainsAny(eq("aud"), anyList())).thenReturn(q);

        repo.listenVisibleIncidentsForCurrentUser("ALL", "RB", "REG", mock(EventListener.class));
        runLooper();

        verify(q, never()).whereEqualTo(eq("type"), any());
    }

    @Test
    public void listenVisibleIncidents_nullFilters_areIgnored() {

        Query q = mockQueryChain();
        when(mockTeamsQuery.get()).thenReturn(Tasks.forResult(mock(QuerySnapshot.class)));
        when(mockIncidents.whereArrayContainsAny(eq("aud"), anyList())).thenReturn(q);

        repo.listenVisibleIncidentsForCurrentUser("TYPE", null, null, mock(EventListener.class));
        runLooper();

        verify(q, never()).whereEqualTo(eq("regionBucket"), any());
        verify(q, never()).whereEqualTo(eq("region"), any());
    }

    @Test
    public void buildTokens_trimsTo10() throws Exception {

        List<String> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) many.add("T" + i);

        Method m = IncidentRepository.class.getDeclaredMethod("buildTokens", String.class, List.class);
        m.setAccessible(true);

        List<String> result = (List<String>) m.invoke(repo, "userA", many);

        assertEquals(10, result.size());
    }
}
