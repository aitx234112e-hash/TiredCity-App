package com.tiredcity.app.ui.explore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

public class EventViewModel extends ViewModel {
    private final EventRepository repository;

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public EventViewModel(EventRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Event>> getEvents() { return events; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadEvents() {
        isLoading.setValue(true);
        repository.getEventsFromFirestore(new EventRepository.OnEventsLoadedListener() {
            @Override
            public void onSuccess(List<Event> list) {
                events.setValue(list);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue(message);
                isLoading.setValue(false);
            }
        });
    }
}
