package com.tiredcity.app.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.network.ApiService;

import java.util.List;

public class EventRepository {
    private final ApiService apiService;
    private final FirebaseFirestore db;

    public EventRepository(ApiService apiService) {
        this.apiService = apiService;
        this.db = FirebaseFirestore.getInstance();
    }

    public void getEventsFromFirestore(OnEventsLoadedListener listener) {
        db.collection("events")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    listener.onError(error.getMessage());
                    return;
                }
                if (value != null) {
                    List<Event> list = value.toObjects(Event.class);
                    listener.onSuccess(list);
                }
            });
    }

    public void getEventById(String id, OnEventLoadedListener listener) {
        db.collection("events").document(id).get()
            .addOnSuccessListener(d -> {
                if (d.exists()) {
                    listener.onSuccess(d.toObject(Event.class));
                } else {
                    listener.onError("Event not found");
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnEventLoadedListener {
        void onSuccess(Event event);
        void onError(String message);
    }

    public interface OnEventsLoadedListener {
        void onSuccess(List<Event> events);
        void onError(String message);
    }
}
