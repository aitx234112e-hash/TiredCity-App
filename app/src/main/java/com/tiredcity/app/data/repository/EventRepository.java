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

    public interface OnEventsLoadedListener {
        void onSuccess(List<Event> events);
        void onError(String message);
    }
}
