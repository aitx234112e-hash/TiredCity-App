package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tiredcity.app.R;
import com.tiredcity.app.adapter.EventAdapter;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.EventRepository;
import com.tiredcity.app.databinding.ActivityEventBinding;
import com.tiredcity.app.ui.base.BaseActivity;

import java.util.ArrayList;

/** Trang Sự kiện — danh sách sự kiện. Mở từ mục "Sự kiện" ở Trang chủ. */
public class EventActivity extends BaseActivity {

    private ActivityEventBinding binding;
    private EventViewModel viewModel;
    private final EventAdapter adapter = new EventAdapter(new ArrayList<>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Repository và ViewModel
        EventRepository repository = new EventRepository(ApiClient.getApiService(preferenceManager.getToken()));
        EventViewModelFactory factory = new EventViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(EventViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEvents.setAdapter(adapter);

        adapter.setOnEventClickListener(event -> {
            android.content.Intent intent = new android.content.Intent(this, EventDetailActivity.class);
            intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });

        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.tc_red));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadEvents());

        observeViewModel();
        
        viewModel.loadEvents();
    }

    private void observeViewModel() {
        viewModel.getEvents().observe(this, events -> {
            binding.swipeRefresh.setRefreshing(false);
            adapter.updateEvents(events);
            binding.tvEmpty.setVisibility(events.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading);
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
