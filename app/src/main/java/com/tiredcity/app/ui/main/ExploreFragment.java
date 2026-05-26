package com.tiredcity.app.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ArticleAdapter;
import com.tiredcity.app.adapter.EventAdapter;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.databinding.FragmentExploreBinding;
import com.tiredcity.app.utils.PreferenceManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private ApiService apiService;
    private ArticleAdapter articleAdapter;
    private EventAdapter eventAdapter;
    private boolean showingArticles = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferenceManager prefs = new PreferenceManager(requireContext());
        apiService = ApiClient.getApiService(prefs.getToken());

        binding.rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingArticles = (tab.getPosition() == 0);
                loadContent();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { loadContent(); }
        });

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.brand_gold));
        binding.swipeRefresh.setOnRefreshListener(this::loadContent);

        loadContent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadContent() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(true);

        if (showingArticles) {
            apiService.getArticles(1, 20).enqueue(new Callback<ApiListResponse<Article>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Article>> call,
                                       Response<ApiListResponse<Article>> response) {
                    if (binding == null) return;
                    binding.swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Article> articles = response.body().getData();
                        articleAdapter = new ArticleAdapter(articles);
                        binding.rvContent.setAdapter(articleAdapter);
                    }
                }

                @Override
                public void onFailure(Call<ApiListResponse<Article>> call, Throwable t) {
                    if (binding != null) binding.swipeRefresh.setRefreshing(false);
                }
            });
        } else {
            apiService.getEvents(1, 20).enqueue(new Callback<ApiListResponse<Event>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Event>> call,
                                       Response<ApiListResponse<Event>> response) {
                    if (binding == null) return;
                    binding.swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Event> events = response.body().getData();
                        eventAdapter = new EventAdapter(events);
                        binding.rvContent.setAdapter(eventAdapter);
                    }
                }

                @Override
                public void onFailure(Call<ApiListResponse<Event>> call, Throwable t) {
                    if (binding != null) binding.swipeRefresh.setRefreshing(false);
                }
            });
        }
    }
}
