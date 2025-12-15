package com.abandiak.alerta.repositories;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abandiak.alerta.data.repository.ChatRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ChatRepositoryTest {

    private FirebaseFirestore mockDb;
    private CollectionReference mockDmChats;
    private CollectionReference mockTeams;
    private DocumentReference mockChatDoc;
    private CollectionReference mockMessagesCollection;

    private MockedStatic<FirebaseAuth> authStatic;
    private MockedStatic<android.util.Log> logStatic;

    private ChatRepository repo;

    @Before
    public void setUp() {

        mockDb = mock(FirebaseFirestore.class);
        mockDmChats = mock(CollectionReference.class);
        mockTeams = mock(CollectionReference.class);
        mockChatDoc = mock(DocumentReference.class);
        mockMessagesCollection = mock(CollectionReference.class);

        repo = new ChatRepository(mockDb);

        when(mockDb.collection("dm_chats")).thenReturn(mockDmChats);
        when(mockDb.collection("teams")).thenReturn(mockTeams);
        when(mockDmChats.document(anyString())).thenReturn(mockChatDoc);
        when(mockChatDoc.collection("messages")).thenReturn(mockMessagesCollection);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.getUid()).thenReturn("userA");

        authStatic = mockStatic(FirebaseAuth.class);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        logStatic = mockStatic(android.util.Log.class);
        logStatic.when(() -> android.util.Log.e(anyString(), anyString())).thenReturn(0);
        logStatic.when(() -> android.util.Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);

        when(mockChatDoc.set(anyMap())).thenReturn(Tasks.forResult(null));
        when(mockChatDoc.set(any())).thenReturn(Tasks.forResult(null));
        when(mockChatDoc.set(any(), any())).thenReturn(Tasks.forResult(null));
    }

    @After
    public void tearDown() {
        authStatic.close();
        logStatic.close();
    }

    private void runLooper() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @Test
    public void dmChatIdFor_sortsLexicographically() {
        ChatRepository repo2 = new ChatRepository(mockDb);
        String id = repo2.dmChatIdFor("zUser", "aUser");
        org.junit.Assert.assertEquals("aUser_zUser", id);
    }

    @Test
    public void sendDirectMessage_createsChatAndSendsMessage() {

        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        when(mockChatDoc.get()).thenReturn(Tasks.forResult(mockSnapshot));
        when(mockMessagesCollection.add(anyMap()))
                .thenReturn(Tasks.forResult(mock(DocumentReference.class)));

        ChatRepository.OnDmCreated callback = mock(ChatRepository.OnDmCreated.class);

        repo.sendDirectMessage("userB", "Hello!", callback);
        runLooper();

        ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockChatDoc).set(metaCaptor.capture());

        Map<String, Object> meta = metaCaptor.getValue();

        org.junit.Assert.assertEquals("Hello!", meta.get("lastMessage"));
        org.junit.Assert.assertTrue(((java.util.List<String>) meta.get("participants")).contains("userA"));
        org.junit.Assert.assertTrue(((java.util.List<String>) meta.get("participants")).contains("userB"));

        verify(mockMessagesCollection).add(anyMap());
        verify(callback).onCreated(false);
    }

    @Test
    public void sendDirectMessage_existingChat_callbackReceivesTrue() {

        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);

        when(mockChatDoc.get()).thenReturn(Tasks.forResult(mockSnapshot));
        when(mockMessagesCollection.add(anyMap()))
                .thenReturn(Tasks.forResult(mock(DocumentReference.class)));

        ChatRepository.OnDmCreated callback = mock(ChatRepository.OnDmCreated.class);

        repo.sendDirectMessage("userB", "Hello again!", callback);
        runLooper();

        verify(callback).onCreated(true);
    }

    @Test
    public void sendDirectMessage_handlesGetFailure() {

        when(mockChatDoc.get()).thenReturn(Tasks.forException(new Exception("fail")));

        repo.sendDirectMessage("userB", "Hello!", mock(ChatRepository.OnDmCreated.class));
        runLooper();

        verify(mockChatDoc, never()).set(anyMap());
    }

    @Test
    public void sendDirectMessage_handlesSetFailure() {

        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        when(mockChatDoc.get()).thenReturn(Tasks.forResult(mockSnapshot));
        when(mockChatDoc.set(anyMap())).thenReturn(Tasks.forException(new Exception("set failed")));

        repo.sendDirectMessage("userB", "Hello!", mock(ChatRepository.OnDmCreated.class));
        runLooper();

        verify(mockMessagesCollection, never()).add(anyMap());
    }

    @Test
    public void sendDirectMessage_handlesAddMessageFailure() {

        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        when(mockChatDoc.get()).thenReturn(Tasks.forResult(mockSnapshot));
        when(mockChatDoc.set(anyMap())).thenReturn(Tasks.forResult(null));
        when(mockMessagesCollection.add(anyMap()))
                .thenReturn(Tasks.forException(new Exception("add failed")));

        repo.sendDirectMessage("userB", "Hello!", mock(ChatRepository.OnDmCreated.class));
        runLooper();

        verify(mockMessagesCollection).add(anyMap());
    }

    @Test
    public void sendDirectMessage_noCallback_doesNotCrash() {

        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        when(mockChatDoc.get()).thenReturn(Tasks.forResult(mockSnapshot));
        when(mockMessagesCollection.add(anyMap()))
                .thenReturn(Tasks.forResult(mock(DocumentReference.class)));

        repo.sendDirectMessage("userB", "Hello!", null);
        runLooper();

        verify(mockMessagesCollection).add(anyMap());
    }

    @Test
    public void sendDirectMessage_doesNothing_whenOtherUserIdIsNull() {

        repo.sendDirectMessage(null, "Hello!", mock(ChatRepository.OnDmCreated.class));

        verifyNoInteractions(mockDmChats);
    }

    @Test
    public void sendDirectMessage_doesNothing_whenCurrentUidIsNull() {

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.getUid()).thenReturn(null);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        repo.sendDirectMessage("userB", "Hello!", mock(ChatRepository.OnDmCreated.class));

        verifyNoInteractions(mockDmChats);
    }

    @Test
    public void listenForDmMessages_registersSnapshotListener() {

        CollectionReference mockMessages = mock(CollectionReference.class);
        when(mockChatDoc.collection("messages")).thenReturn(mockMessages);

        Query mockQuery = mock(Query.class);
        when(mockMessages.orderBy("createdAt")).thenReturn(mockQuery);

        EventListener<QuerySnapshot> listener = mock(EventListener.class);

        repo.listenForDmMessages("userB", listener);

        verify(mockQuery).addSnapshotListener(listener);
    }

    @Test
    public void listenForDmMessages_returnsNull_whenUidIsNull() {

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.getUid()).thenReturn(null);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        ListenerRegistration result =
                repo.listenForDmMessages("userB", mock(EventListener.class));

        org.junit.Assert.assertNull(result);
    }

    @Test
    public void listenForDmChatList_registersListener() {

        Query mockQuery = mock(Query.class);
        when(mockDmChats.whereArrayContains("participants", "userA"))
                .thenReturn(mockQuery);

        Query mockOrdered = mock(Query.class);
        when(mockQuery.orderBy("lastTimestamp", Query.Direction.DESCENDING))
                .thenReturn(mockOrdered);

        EventListener<QuerySnapshot> listener = mock(EventListener.class);

        repo.listenForDmChatList(listener);

        verify(mockOrdered).addSnapshotListener(listener);
    }

    @Test
    public void listenForDmChatList_returnsNull_whenUidIsNull() {

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.getUid()).thenReturn(null);
        authStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        ListenerRegistration result =
                repo.listenForDmChatList(mock(EventListener.class));

        org.junit.Assert.assertNull(result);
    }

    @Test
    public void listenForTeamMessages_registersListener() {

        DocumentReference mockTeamDoc = mock(DocumentReference.class);
        when(mockTeams.document("team123")).thenReturn(mockTeamDoc);

        CollectionReference msgCol = mock(CollectionReference.class);
        when(mockTeamDoc.collection("messages")).thenReturn(msgCol);

        Query mockQuery = mock(Query.class);
        when(msgCol.orderBy("createdAt")).thenReturn(mockQuery);

        EventListener<QuerySnapshot> listener = mock(EventListener.class);

        repo.listenForTeamMessages("team123", listener);

        verify(mockQuery).addSnapshotListener(listener);
    }

    @Test
    public void listenForTeamChatList_registersListener() {

        Query mockQuery = mock(Query.class);
        when(mockTeams.whereIn(eq(FieldPath.documentId()), anyList()))
                .thenReturn(mockQuery);

        EventListener<QuerySnapshot> listener = mock(EventListener.class);

        repo.listenForTeamChatList(Arrays.asList("A", "B"), listener);

        verify(mockQuery).addSnapshotListener(listener);
    }
}
