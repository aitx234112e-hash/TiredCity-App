package com.tiredcity.app.ui.notification;

import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.NotificationAdapter;
import com.tiredcity.app.data.local.NotificationStore;
import com.tiredcity.app.data.model.NotificationItem;
import com.tiredcity.app.databinding.ActivityNotificationBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends BaseActivity {

    private ActivityNotificationBinding binding;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.notification_title));
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        notifications = buildMockNotifications();

        adapter = new NotificationAdapter(notifications, (item, position) -> {
            if (item.isUnread()) {
                item.setUnread(false);
                adapter.notifyItemChanged(position);
            }
        });

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        // Đã xem danh sách → xoá badge chưa đọc ở màn hình chính.
        new NotificationStore(this).markAllRead();

        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = notifications == null || notifications.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** Dữ liệu thông báo mẫu cho bản xem trước. */
    private List<NotificationItem> buildMockNotifications() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem(
                getString(R.string.notif_order_title),
                getString(R.string.notif_order_message),
                getString(R.string.notif_time_recent),
                NotificationItem.Type.ORDER, true));
        list.add(new NotificationItem(
                getString(R.string.notif_promo_title),
                getString(R.string.notif_promo_message),
                getString(R.string.notif_time_today),
                NotificationItem.Type.PROMO, true));
        list.add(new NotificationItem(
                getString(R.string.notif_system_title),
                getString(R.string.notif_system_message),
                getString(R.string.notif_time_yesterday),
                NotificationItem.Type.SYSTEM, false));
        return list;
    }
}
