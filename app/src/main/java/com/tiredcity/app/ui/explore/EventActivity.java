package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.EventAdapter;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.repository.FirestoreEventRepository;
import com.tiredcity.app.databinding.ActivityEventBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Trang Sự kiện — danh sách sự kiện. Mở từ mục "Sự kiện" ở Trang chủ. */
public class EventActivity extends BaseActivity {

    private ActivityEventBinding binding;
    private final EventAdapter adapter = new EventAdapter(new ArrayList<>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEvents.setAdapter(adapter);

        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.tc_red));
        binding.swipeRefresh.setOnRefreshListener(this::loadEvents);

        loadEvents();
    }

    private void loadEvents() {
        binding.swipeRefresh.setRefreshing(true);
        // Đọc thẳng Firestore (collection events) — cùng nguồn với admin.
        new FirestoreEventRepository().getEvents(events -> {
            if (binding == null) return;
            if (events != null && !events.isEmpty()) {
                showEvents(events);
            } else {
                // Firestore lỗi/rỗng → hiển thị dữ liệu mẫu để vẫn xem được giao diện.
                showEvents(buildMockEvents());
            }
        });
    }

    private void showEvents(List<Event> events) {
        binding.swipeRefresh.setRefreshing(false);
        adapter.updateEvents(events);
        binding.tvEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Dữ liệu mẫu offline cho bản demo giao diện. */
    private List<Event> buildMockEvents() {
        // title | location | daysFromNow | online
        Object[][] data = {
            {"Triển lãm Việt Phục Xuân 2025", "Nhà Văn hoá Thanh Niên, TP.HCM", 7, false},
            {"Workshop phối đồ theo mệnh", "Tired City Store, Hà Nội", 14, false},
            {"Talkshow: Cổ phục & Gen Z", "Online qua Zoom", 3, true},
            {"Đêm trình diễn Nhật Bình", "Văn Miếu – Quốc Tử Giám", 21, false},
            {"Chợ phiên Việt phục cuối tuần", "Phố đi bộ Hồ Gươm", 10, false},
        };
        List<Event> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        int i = 0;
        for (Object[] row : data) {
            Event e = new Event();
            e.setId(String.valueOf(++i));
            e.setTitle((String) row[0]);
            e.setLocation((String) row[1]);
            e.setStartDate(new Date(now + (int) row[2] * 86_400_000L));
            e.setOnline((boolean) row[3]);
            list.add(e);
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
