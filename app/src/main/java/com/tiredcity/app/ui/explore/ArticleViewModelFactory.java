package com.tiredcity.app.ui.explore;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.tiredcity.app.data.repository.ArticleRepository;

public class ArticleViewModelFactory implements ViewModelProvider.Factory {
    private final ArticleRepository repository;

    public ArticleViewModelFactory(ArticleRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ArticleViewModel.class)) {
            return (T) new ArticleViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
