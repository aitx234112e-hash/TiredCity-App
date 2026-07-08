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

        adapter.setOnEventClickListener(this::openEventDetail);

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

    /** Riêng card "Yêu Nước Từ Trong Nôi" mở trang chi tiết biên tập sẵn;
     *  các sự kiện khác dùng trang chi tiết chung. */
    private void openEventDetail(Event event) {
        if (isYeuNuocWorkshop(event)) {
            startActivity(new android.content.Intent(this, WorkshopYeuNuocActivity.class));
            return;
        }
        PostDetailActivity.start(this, PostContent.forEvent(event, getString(R.string.home_section_events)));
    }

    /** Nhận diện đúng card này dù Firestore đã đổi tên hay chưa: khớp theo id
     *  document seed, hoặc theo tiêu đề (tên mới "YÊU NƯỚC" lẫn tên cũ "phối đồ theo mệnh"). */
    private boolean isYeuNuocWorkshop(Event event) {
        String id = event.getId() != null ? event.getId() : "";
        if (id.equals("workshop-phoi-do-theo-menh")) return true;
        String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
        return title.contains("yêu nước") || title.contains("phối đồ theo mệnh");
    }

    private void showEvents(List<Event> events) {
        binding.swipeRefresh.setRefreshing(false);
        // Sự kiện chưa có ảnh riêng vẫn cần ảnh bìa — lấy từ kho ảnh sự kiện nội bộ,
        // cùng ảnh mà trang chi tiết sẽ dùng.
        for (Event e : events) {
            boolean hasRemoteImage = e.getImageUrl() != null && !e.getImageUrl().isEmpty();
            if (!hasRemoteImage && e.getLocalImageRes() == 0) {
                e.setLocalImageRes(PostContent.heroForTitle(e.getTitle()));
            }
        }
        adapter.updateEvents(events);
        binding.tvEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Dữ liệu mẫu offline cho bản demo giao diện. */
    private List<Event> buildMockEvents() {
        // title | location | daysFromNow | online
        Object[][] data = {
            {"Triển lãm Việt Phục Xuân 2025", "Nhà Văn hoá Thanh Niên, TP.HCM", 7, false},
            {"Workshop YÊU NƯỚC TỪ TRONG NÔI", "Tired City Store, Hà Nội", 14, false},
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
