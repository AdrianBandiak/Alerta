package com.abandiak.alerta.repositories;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abandiak.alerta.data.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
public class UserRepositoryTest {

    FirebaseFirestore mockDb;
    CollectionReference usersCol;
    DocumentReference userDoc;
    DocumentSnapshot userSnapshot;

    MockedStatic<FirebaseFirestore> firestoreStatic;

    UserRepository repo;

    @Before
    public void setup() {

        mockDb = mock(FirebaseFirestore.class);
        usersCol = mock(CollectionReference.class);
        userDoc = mock(DocumentReference.class);
        userSnapshot = mock(DocumentSnapshot.class);

        firestoreStatic = mockStatic(FirebaseFirestore.class);
        firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        when(mockDb.collection("users")).thenReturn(usersCol);
        when(usersCol.document(anyString())).thenReturn(userDoc);

        repo = new UserRepository();
    }

    @After
    public void cleanup() {
        firestoreStatic.close();
    }

    void runLooper() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @Test
    public void getUserById_cacheHit_returnsImmediately() {

        UserRepository.UserCallback cb = mock(UserRepository.UserCallback.class);

        String userId = "U1";

        var cacheField = UserRepository.class.getDeclaredFields()[1];
        cacheField.setAccessible(true);

        try {
            HashMap<String, DocumentSnapshot> cache =
                    (HashMap<String, DocumentSnapshot>) cacheField.get(repo);

            cache.put(userId, userSnapshot);

        } catch (Exception e) { throw new RuntimeException(e); }

        repo.getUserById(userId, cb);
        runLooper();

        verify(cb).onResult(userSnapshot);
        verify(userDoc, never()).get();
    }

    @Test
    public void getUserById_firestoreSuccess() {

        UserRepository.UserCallback cb = mock(UserRepository.UserCallback.class);

        when(userDoc.get()).thenReturn(Tasks.forResult(userSnapshot));

        repo.getUserById("U2", cb);
        runLooper();

        verify(cb).onResult(userSnapshot);

        try {
            var cacheField = UserRepository.class.getDeclaredFields()[1];
            cacheField.setAccessible(true);
            HashMap<String, DocumentSnapshot> cache =
                    (HashMap<String, DocumentSnapshot>) cacheField.get(repo);

            Assert.assertTrue(cache.containsKey("U2"));

        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    public void getUserById_firestoreFailure() {

        UserRepository.UserCallback cb = mock(UserRepository.UserCallback.class);

        when(userDoc.get()).thenReturn(Tasks.forException(new Exception("FAIL")));

        repo.getUserById("U3", cb);
        runLooper();

        verify(cb).onResult(null);
    }

}
