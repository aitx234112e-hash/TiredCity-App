package com.tiredcity.app.ui.explore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.repository.ArticleRepository;

import java.util.ArrayList;
import java.util.List;

public class ArticleViewModel extends ViewModel {
    private final ArticleRepository repository;

    private final MutableLiveData<List<Article>> articles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ArticleViewModel(ArticleRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Article>> getArticles() { return articles; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadArticles() {
        isLoading.setValue(true);
        repository.getArticlesFromFirestore(new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> list) {
                articles.setValue(list);
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
