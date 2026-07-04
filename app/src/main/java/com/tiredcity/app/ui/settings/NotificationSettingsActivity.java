package com.tiredcity.app.ui.settings;

import android.os.Bundle;
import androidx.annotation.StringRes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityNotificationSettingsBinding;
import com.tiredcity.app.databinding.ItemSettingsSwitchRowBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Cài đặt thông báo — 2 nhóm (Tin tức &amp; Thông báo, Khuyến mãi),
 * mỗi nhóm 4 công tắc (Push / Email / ZNS / SMS).
 * Trạng thái lưu bằng SharedPreferences qua {@link com.tiredcity.app.utils.PreferenceManager}.
 */
public class NotificationSettingsActivity extends BaseActivity {

    private ActivityNotificationSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Nhóm Tin tức & Thông báo
        bindRow(binding.rowNewsPush,  R.string.settings_notif_push_title,  Constants.KEY_NOTIF_NEWS_PUSH);
        bindRow(binding.rowNewsEmail, R.string.settings_notif_email_title, Constants.KEY_NOTIF_NEWS_EMAIL);
        bindRow(binding.rowNewsZns,   R.string.settings_notif_zns_title,   Constants.KEY_NOTIF_NEWS_ZNS);
        bindRow(binding.rowNewsSms,   R.string.settings_notif_sms_title,   Constants.KEY_NOTIF_NEWS_SMS);

        // Nhóm Khuyến mãi
        bindRow(binding.rowPromoPush,  R.string.settings_notif_push_title,  Constants.KEY_NOTIF_PROMO_PUSH);
        bindRow(binding.rowPromoEmail, R.string.settings_notif_email_title, Constants.KEY_NOTIF_PROMO_EMAIL);
        bindRow(binding.rowPromoZns,   R.string.settings_notif_zns_title,   Constants.KEY_NOTIF_PROMO_ZNS);
        bindRow(binding.rowPromoSms,   R.string.settings_notif_sms_title,   Constants.KEY_NOTIF_PROMO_SMS);
    }

    private void bindRow(ItemSettingsSwitchRowBinding row, @StringRes int labelRes, String prefKey) {
        row.tvLabel.setText(labelRes);
        // Mặc định BẬT; đọc lại trạng thái đã lưu.
        row.switchToggle.setChecked(preferenceManager.getToggle(prefKey, true));
        row.getRoot().setOnClickListener(v -> row.switchToggle.toggle());
        row.switchToggle.setOnCheckedChangeListener((btn, isChecked) -> {
                preferenceManager.setToggle(prefKey, isChecked);
                syncSettingsToCloud();
        });
    }

    private void syncSettingsToCloud() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> settings = new HashMap<>();
        settings.put("news_push", preferenceManager.getToggle(Constants.KEY_NOTIF_NEWS_PUSH, true));
        settings.put("news_email", preferenceManager.getToggle(Constants.KEY_NOTIF_NEWS_EMAIL, true));
        settings.put("news_zns", preferenceManager.getToggle(Constants.KEY_NOTIF_NEWS_ZNS, true));
        settings.put("news_sms", preferenceManager.getToggle(Constants.KEY_NOTIF_NEWS_SMS, true));
        settings.put("promo_push", preferenceManager.getToggle(Constants.KEY_NOTIF_PROMO_PUSH, true));
        settings.put("promo_email", preferenceManager.getToggle(Constants.KEY_NOTIF_PROMO_EMAIL, true));
        settings.put("promo_zns", preferenceManager.getToggle(Constants.KEY_NOTIF_PROMO_ZNS, true));
        settings.put("promo_sms", preferenceManager.getToggle(Constants.KEY_NOTIF_PROMO_SMS, true));

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update("notification_settings", settings);
    }
}
