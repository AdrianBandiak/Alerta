package com.abandiak.alerta.util;

import com.abandiak.alerta.data.model.Incident;
import com.abandiak.alerta.data.repository.IncidentRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class FakeIncidentRepository extends IncidentRepository {

    private List<Incident> incidents;

    public void setIncidents(List<Incident> list) {
        incidents = list;
    }

    @Override
    public ListenerRegistration listenVisibleIncidentsForCurrentUser(
            String type, String regionBucket, String region,
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {

        if (listener != null) {
            listener.onEvent(null, null);
        }
        return () -> {};
    }

    public List<Incident> getIncidentsForTest() {
        return incidents;
    }
}
