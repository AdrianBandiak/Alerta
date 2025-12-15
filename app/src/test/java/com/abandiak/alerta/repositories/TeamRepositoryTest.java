package com.abandiak.alerta.repositories;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abandiak.alerta.app.messages.teams.TeamMemberEntry;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.data.repository.TeamRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.*;

@RunWith(RobolectricTestRunner.class)
public class TeamRepositoryTest {

    FirebaseFirestore mockDb;
    CollectionReference teamsCol;
    CollectionReference codesCol;
    CollectionReference usersCol;
    DocumentReference teamDoc;
    DocumentReference codeDoc;
    DocumentReference userDoc;
    Query mockQuery;
    MockedStatic<android.util.Log> logs;
    MockedStatic<FirebaseAuth> authStatic;

    TeamRepository repo;

    FirebaseAuth mockAuth;
    FirebaseUser mockUser;

    @Before
    public void setup() {

        mockDb = mock(FirebaseFirestore.class);

        teamsCol = mock(CollectionReference.class);
        codesCol = mock(CollectionReference.class);
        usersCol = mock(CollectionReference.class);

        when(mockDb.collection("teams")).thenReturn(teamsCol);
        when(mockDb.collection("team_codes")).thenReturn(codesCol);
        when(mockDb.collection("users")).thenReturn(usersCol);

        teamDoc = mock(DocumentReference.class);
        when(teamDoc.getId()).thenReturn("TEAM123");
        codeDoc = mock(DocumentReference.class);
        userDoc = mock(DocumentReference.class);

        when(teamsCol.document(anyString())).thenReturn(teamDoc);
        when(teamsCol.document()).thenReturn(teamDoc);
        when(codesCol.document(anyString())).thenReturn(codeDoc);
        when(usersCol.document(anyString())).thenReturn(userDoc);

        mockQuery = mock(Query.class);
        when(teamsCol.whereArrayContains(anyString(), anyString())).thenReturn(mockQuery);

        logs = mockStatic(android.util.Log.class);
        logs.when(() -> android.util.Log.e(anyString(), anyString())).thenReturn(0);
        logs.when(() -> android.util.Log.d(anyString(), anyString())).thenReturn(0);

        mockAuth = mock(FirebaseAuth.class);
        mockUser = mock(FirebaseUser.class);

        when(mockAuth.getUid()).thenReturn("UID");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getDisplayName()).thenReturn("NAME");

        authStatic = mockStatic(FirebaseAuth.class);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        repo = new TeamRepository(mockDb, "UID");

        when(teamDoc.set(any())).thenReturn(Tasks.forResult(null));
        when(teamDoc.set(any(), any())).thenReturn(Tasks.forResult(null));

        when(teamDoc.update(anyString(), any())).thenReturn(Tasks.forResult(null));
        when(teamDoc.update(anyMap())).thenReturn(Tasks.forResult(null));

        when(teamDoc.delete()).thenReturn(Tasks.forResult(null));

        when(teamDoc.collection("members")).thenReturn(mock(CollectionReference.class));
        when(teamDoc.collection("messages")).thenReturn(mock(CollectionReference.class));
    }

    @After
    public void cleanup() {
        logs.close();
        authStatic.close();
    }

    void runLooper() { ShadowLooper.runUiThreadTasksIncludingDelayedTasks(); }

    @Test
    public void listenMyTeams_uidNull_returnsNull() {
        TeamRepository repo2 = new TeamRepository(mockDb, null);
        ListenerRegistration reg = repo2.listenMyTeams(mock(TeamRepository.TeamsListener.class));
        Assert.assertNull(reg);
    }

    @Test
    public void createTeam_uidNull_callsCallbackWithError() {
        TeamRepository repo2 = new TeamRepository(mockDb, null);
        TeamRepository.CreateTeamCallback cb = mock(TeamRepository.CreateTeamCallback.class);

        repo2.createTeam("A", "B", 1, "R", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void joinByCode_firestoreGetFails_callsError() {

        when(codeDoc.get()).thenReturn(Tasks.forException(new Exception("fail")));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("XYZ", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void getFullTeamMembers_membersQueryFails_returnsEmptyList() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        when(members.get()).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(List::isEmpty));
    }

    @Test
    public void listenMyTeams_success() {
        ListenerRegistration reg = () -> {};
        when(mockQuery.addSnapshotListener(any()))
                .thenAnswer(inv -> {
                    EventListener<QuerySnapshot> cb = inv.getArgument(0);

                    QueryDocumentSnapshot d = mock(QueryDocumentSnapshot.class);
                    Team t = new Team();
                    when(d.toObject(Team.class)).thenReturn(t);
                    when(d.getId()).thenReturn("A");

                    QuerySnapshot snap = mock(QuerySnapshot.class);
                    when(snap.getDocuments()).thenReturn(List.of(d));

                    cb.onEvent(snap, null);
                    return reg;
                });

        TeamRepository.TeamsListener listener = mock(TeamRepository.TeamsListener.class);

        repo.listenMyTeams(listener);
        runLooper();

        verify(listener).onSuccess(argThat(list -> list.size() == 1 && list.get(0).getId().equals("A")));
    }

    @Test
    public void listenMyTeams_error() {
        ListenerRegistration reg = () -> {};
        when(mockQuery.addSnapshotListener(any()))
                .thenAnswer(inv -> {
                    EventListener<QuerySnapshot> cb = inv.getArgument(0);
                    cb.onEvent(null, new FirebaseFirestoreException("ERR", FirebaseFirestoreException.Code.UNKNOWN));
                    return reg;
                });

        TeamRepository.TeamsListener listener = mock(TeamRepository.TeamsListener.class);

        repo.listenMyTeams(listener);
        runLooper();

        verify(listener).onError(any());
    }

    @Test
    public void listenMyTeams_nullSnapshot() {
        ListenerRegistration reg = () -> {};
        when(mockQuery.addSnapshotListener(any())).thenAnswer(inv -> {
            EventListener<QuerySnapshot> cb = inv.getArgument(0);
            cb.onEvent(null, null);
            return reg;
        });

        TeamRepository.TeamsListener listener = mock(TeamRepository.TeamsListener.class);

        repo.listenMyTeams(listener);
        runLooper();

        verify(listener).onSuccess(argThat(List::isEmpty));
    }


    @Test
    public void createTeam_success() {

        CollectionReference members = mock(CollectionReference.class);
        DocumentReference memberDoc = mock(DocumentReference.class);

        when(teamDoc.collection("members")).thenReturn(members);
        when(members.document("UID")).thenReturn(memberDoc);

        when(memberDoc.set(any())).thenReturn(Tasks.forResult(null));
        when(codeDoc.set(any())).thenReturn(Tasks.forResult(null));

        TeamRepository.CreateTeamCallback cb = mock(TeamRepository.CreateTeamCallback.class);

        repo.createTeam("A", "B", 1, "R", cb);
        runLooper();

        verify(cb).onComplete(eq(true), anyString());
    }

    @Test
    public void createTeam_firstStepFails() {

        when(teamDoc.set(any())).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.CreateTeamCallback cb = mock(TeamRepository.CreateTeamCallback.class);

        repo.createTeam("A", "B", 1, "R", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void createTeam_secondStepFails() {

        CollectionReference members = mock(CollectionReference.class);
        DocumentReference memberDoc = mock(DocumentReference.class);
        when(teamDoc.collection("members")).thenReturn(members);
        when(members.document("UID")).thenReturn(memberDoc);

        when(memberDoc.set(any())).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.CreateTeamCallback cb = mock(TeamRepository.CreateTeamCallback.class);

        repo.createTeam("A", "B", 1, "R", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void createTeam_thirdStepFails() {

        CollectionReference members = mock(CollectionReference.class);
        DocumentReference memberDoc = mock(DocumentReference.class);
        when(teamDoc.collection("members")).thenReturn(members);
        when(members.document("UID")).thenReturn(memberDoc);

        when(memberDoc.set(any())).thenReturn(Tasks.forResult(null));
        when(codeDoc.set(any())).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.CreateTeamCallback cb = mock(TeamRepository.CreateTeamCallback.class);

        repo.createTeam("A", "B", 1, "R", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }


    @Test
    public void initializeTeamChat_callsAdd() {

        CollectionReference msgCol = mock(CollectionReference.class);
        when(teamDoc.collection("messages")).thenReturn(msgCol);

        when(msgCol.add(any())).thenReturn(Tasks.forResult(null));

        repo.initializeTeamChat("X");
        runLooper();

        verify(msgCol).add(any());
    }


    @Test
    public void updateTeam_success() {

        Team t = new Team();
        t.setId("ID");

        TeamRepository.UpdateTeamCallback cb = mock(TeamRepository.UpdateTeamCallback.class);

        repo.updateTeam(t, cb);
        runLooper();

        verify(cb).onComplete(eq(true), anyString());
    }

    @Test
    public void updateTeam_failure() {

        Team t = new Team();
        t.setId("ID");

        when(teamDoc.update(anyMap())).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.UpdateTeamCallback cb = mock(TeamRepository.UpdateTeamCallback.class);

        repo.updateTeam(t, cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void updateTeam_missingId() {

        Team t = new Team();

        TeamRepository.UpdateTeamCallback cb = mock(TeamRepository.UpdateTeamCallback.class);

        repo.updateTeam(t, cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }


    @Test
    public void deleteTeam_success() {

        TeamRepository.DeleteTeamCallback cb = mock(TeamRepository.DeleteTeamCallback.class);

        repo.deleteTeam("X", cb);
        runLooper();

        verify(cb).onComplete(true);
    }

    @Test
    public void deleteTeam_failure() {

        when(teamDoc.delete()).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.DeleteTeamCallback cb = mock(TeamRepository.DeleteTeamCallback.class);

        repo.deleteTeam("X", cb);
        runLooper();

        verify(cb).onComplete(false);
    }


    @Test
    public void joinByCode_invalidCode() {

        when(codeDoc.get()).thenReturn(Tasks.forResult(null));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("AAA", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void joinByCode_invalidMapping() {

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("teamId")).thenReturn(null);

        when(codeDoc.get()).thenReturn(Tasks.forResult(snap));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("AAA", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void joinByCode_updateFails() {

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("teamId")).thenReturn("T");

        when(codeDoc.get()).thenReturn(Tasks.forResult(snap));

        when(teamDoc.update(anyString(), any())).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("AAA", cb);
        runLooper();

        verify(cb).onComplete(eq(false), anyString());
    }

    @Test
    public void joinByCode_success() {

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("teamId")).thenReturn("T");

        when(codeDoc.get()).thenReturn(Tasks.forResult(snap));

        CollectionReference members = mock(CollectionReference.class);
        DocumentReference memberDoc = mock(DocumentReference.class);

        when(teamDoc.collection("members")).thenReturn(members);
        when(members.document("UID")).thenReturn(memberDoc);
        when(memberDoc.set(any())).thenReturn(Tasks.forResult(null));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("AAA", cb);
        runLooper();

        verify(cb).onComplete(true, "OK");
    }


    @Test
    public void getFullTeamMembers_noMembers() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.isEmpty()).thenReturn(true);

        when(members.get()).thenReturn(Tasks.forResult(snap));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(List::isEmpty));
    }


    @Test
    public void getFullTeamMembers_userDocExists() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        QueryDocumentSnapshot m1 = mock(QueryDocumentSnapshot.class);
        when(m1.getId()).thenReturn("U1");
        Timestamp ts = Timestamp.now();
        when(m1.getTimestamp("joinedAt")).thenReturn(ts);

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.isEmpty()).thenReturn(false);
        when(snap.iterator()).thenReturn(List.of(m1).iterator());

        when(members.get()).thenReturn(Tasks.forResult(snap));

        DocumentSnapshot userData = mock(DocumentSnapshot.class);
        when(userData.exists()).thenReturn(true);
        when(userData.getString("firstName")).thenReturn("A");
        when(userData.getString("lastName")).thenReturn("B");
        when(userData.getString("photoUrl")).thenReturn("url");

        when(userDoc.get()).thenReturn(Tasks.forResult(userData));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(list -> list.size() == 1 &&
                list.get(0).getFullName().equals("A B")));
    }


    @Test
    public void getFullTeamMembers_userDocMissing() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        QueryDocumentSnapshot m1 = mock(QueryDocumentSnapshot.class);
        when(m1.getId()).thenReturn("U1");

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.isEmpty()).thenReturn(false);
        when(snap.iterator()).thenReturn(List.of(m1).iterator());

        when(members.get()).thenReturn(Tasks.forResult(snap));

        DocumentSnapshot userData = mock(DocumentSnapshot.class);
        when(userData.exists()).thenReturn(false);

        when(userDoc.get()).thenReturn(Tasks.forResult(userData));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(list -> list.get(0).getFullName().contains("...")));
    }


    @Test
    public void getFullTeamMembers_userDocFails() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        QueryDocumentSnapshot m1 = mock(QueryDocumentSnapshot.class);
        when(m1.getId()).thenReturn("U1");

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.isEmpty()).thenReturn(false);
        when(snap.iterator()).thenReturn(List.of(m1).iterator());

        when(members.get()).thenReturn(Tasks.forResult(snap));

        when(userDoc.get()).thenReturn(Tasks.forException(new Exception()));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(list -> list.size() == 1));
    }


    @Test
    public void getFullTeamMembers_sorting() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        QueryDocumentSnapshot m1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot m2 = mock(QueryDocumentSnapshot.class);

        when(m1.getId()).thenReturn("A");
        when(m2.getId()).thenReturn("B");

        Timestamp t1 = Timestamp.now();
        Timestamp t2 = Timestamp.now();

        when(m1.getTimestamp("joinedAt")).thenReturn(t1);
        when(m2.getTimestamp("joinedAt")).thenReturn(t2);

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.isEmpty()).thenReturn(false);
        when(snap.iterator()).thenReturn(List.of(m2, m1).iterator());

        when(members.get()).thenReturn(Tasks.forResult(snap));

        DocumentSnapshot u = mock(DocumentSnapshot.class);
        when(u.exists()).thenReturn(true);
        when(u.getString("firstName")).thenReturn("X");

        when(userDoc.get()).thenReturn(Tasks.forResult(u));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(list -> list.size() == 2));
    }

    @Test
    public void createTeam_noCurrentUser_setsEmptyCreatedByName() {

        FirebaseAuth mockAuth2 = mock(FirebaseAuth.class);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth2);
        when(mockAuth2.getUid()).thenReturn("UID");
        when(mockAuth2.getCurrentUser()).thenReturn(null);

        CollectionReference members = mock(CollectionReference.class);
        DocumentReference memberDoc = mock(DocumentReference.class);

        when(teamDoc.collection("members")).thenReturn(members);
        when(members.document("UID")).thenReturn(memberDoc);

        when(memberDoc.set(any())).thenReturn(Tasks.forResult(null));
        when(codeDoc.set(any())).thenReturn(Tasks.forResult(null));

        TeamRepository.CreateTeamCallback cb = mock(TeamRepository.CreateTeamCallback.class);

        repo.createTeam("A", "B", 1, "R", cb);
        runLooper();

        verify(cb).onComplete(eq(true), anyString());
    }

    @Test
    public void joinByCode_docExistsButTeamIdNull_triggersInvalidMapping() {

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("teamId")).thenReturn(null);

        when(codeDoc.get()).thenReturn(Tasks.forResult(snap));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("XYZ", cb);
        runLooper();

        verify(cb).onComplete(eq(false), eq("Invalid mapping."));
    }

    @Test
    public void joinByCode_updateFails_nullMessage_returnsError() {

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("teamId")).thenReturn("T");

        when(codeDoc.get()).thenReturn(Tasks.forResult(snap));

        Exception ex = new Exception();
        when(teamDoc.update(anyString(), any())).thenReturn(Tasks.forException(ex));

        TeamRepository.JoinCallback cb = mock(TeamRepository.JoinCallback.class);

        repo.joinByCode("AAA", cb);
        runLooper();

        verify(cb).onComplete(eq(false), eq("error"));
    }

    @Test
    public void getFullTeamMembers_userDocExistsButNoName_fallbackId() {

        CollectionReference members = mock(CollectionReference.class);
        when(teamDoc.collection("members")).thenReturn(members);

        QueryDocumentSnapshot m1 = mock(QueryDocumentSnapshot.class);
        when(m1.getId()).thenReturn("U123456");
        Timestamp ts = Timestamp.now();
        when(m1.getTimestamp("joinedAt")).thenReturn(ts);

        QuerySnapshot snap = mock(QuerySnapshot.class);
        when(snap.isEmpty()).thenReturn(false);
        when(snap.iterator()).thenReturn(List.of(m1).iterator());

        when(members.get()).thenReturn(Tasks.forResult(snap));

        DocumentSnapshot userData = mock(DocumentSnapshot.class);
        when(userData.exists()).thenReturn(true);
        when(userData.getString("firstName")).thenReturn(null);
        when(userData.getString("lastName")).thenReturn(null);

        when(userDoc.get()).thenReturn(Tasks.forResult(userData));

        TeamRepository.OnMembersLoaded cb = mock(TeamRepository.OnMembersLoaded.class);

        repo.getFullTeamMembers("T", cb);
        runLooper();

        verify(cb).onLoaded(argThat(list ->
                list.get(0).getFullName().equals("U12345...")
        ));
    }

}
