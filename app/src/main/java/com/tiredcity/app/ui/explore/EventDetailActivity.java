package com.tiredcity.app.ui.explore;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.EventRepository;
import com.tiredcity.app.databinding.ActivityEventDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.DateUtils;

/** Trang chi tiết sự kiện — đồng bộ dữ liệu từ Firestore. */
public class EventDetailActivity extends BaseActivity {

    private static final String VAN_MIEU_GEO = "geo:21.0287,105.8355?q=Văn Miếu Quốc Tử Giám, Hà Nội";
    private ActivityEventDetailBinding binding;
    private EventRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EventRepository(ApiClient.getApiService(preferenceManager.getToken()));
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> Toast.makeText(this, R.string.ed_register_toast, Toast.LENGTH_SHORT).show());
        binding.btnDirection.setOnClickListener(v -> openDirections());

        String eventId = getIntent().getStringExtra(Constants.EXTRA_EVENT_ID);
        if (eventId != null) {
            loadEventDetails(eventId);
        } else {
            setupStaticDemo();
        }
    }

    private void loadEventDetails(String id) {
        repository.getEventById(id, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (binding == null) return;
                bindEventData(event);
            }

            @Override
            public void onError(String message) {
                setupStaticDemo();
            }
        });
    }

    private void bindEventData(Event event) {
        // Bìa đầu luôn dùng banner cố định "DÂN TỘC TỰ HÀO" (event_1) nên giữ nguyên
        // tiêu đề/phụ đề mặc định trong layout để khớp với chữ trên ảnh banner.
        binding.tvFactPlaceValue.setText(event.isOnline() ? "🌐 Online" : event.getLocation());
        binding.tvAboutBody.setText(event.getDescription());
        
        if (event.getEventDate() != null) {
            binding.tvFactTimeValue.setText(DateUtils.formatDisplayDate(event.getEventDate()));
        }

        // Bìa đầu luôn dùng ảnh cố định event_1.jpg cho đồng bộ
        loadImage(binding.ivHero, R.drawable.event_1);

        // Keep other static images for demo feel
        loadImage(binding.ivDisplay, R.drawable.event_4);
        loadImage(binding.ivView, R.drawable.event_8);
        loadImage(binding.ivExp1, R.drawable.event_2);
        loadImage(binding.ivExp2, R.drawable.event_6);
        loadImage(binding.ivExp3, R.drawable.event_5);
        loadImage(binding.ivExp4, R.drawable.event_7);
        loadImage(binding.ivVisitors, R.drawable.event_3);
        loadImage(binding.ivPoster, R.drawable.event_9);
    }

    private void setupStaticDemo() {
        loadImage(binding.ivHero, R.drawable.event_1);
        loadImage(binding.ivDisplay, R.drawable.event_4);
        loadImage(binding.ivView, R.drawable.event_8);
        loadImage(binding.ivExp1, R.drawable.event_2);
        loadImage(binding.ivExp2, R.drawable.event_6);
        loadImage(binding.ivExp3, R.drawable.event_5);
        loadImage(binding.ivExp4, R.drawable.event_7);
        loadImage(binding.ivVisitors, R.drawable.event_3);
        loadImage(binding.ivPoster, R.drawable.event_9);
    }

    private void loadImage(ImageView target, int resId) {
        Glide.with(this)
                .load(resId)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(target);
    }

    private void openDirections() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(VAN_MIEU_GEO));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.ed_fact_place_value, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
