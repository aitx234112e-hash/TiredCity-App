package com.tiredcity.app.ui.explore;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.tiredcity.app.data.repository.EventRepository;

public class EventViewModelFactory implements ViewModelProvider.Factory {
    private final EventRepository repository;

    public EventViewModelFactory(EventRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EventViewModel.class)) {
            return (T) new EventViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
