package com.tiredcity.app.utils;

public final class Constants {
    private Constants() {}

    public static final String BASE_URL        = "https://tiredcity.vn/";
    public static final String CLAUDE_API_KEY  = "YOUR_CLAUDE_API_KEY_HERE";

    // SharedPreferences name
    public static final String PREF_NAME       = "tiredcity_prefs";

    // SharedPreferences keys
    public static final String KEY_TOKEN            = "key_token";
    public static final String KEY_USER_ID          = "key_user_id";
    public static final String KEY_MENH             = "key_menh";
    public static final String KEY_ZODIAC           = "key_zodiac";
    public static final String KEY_LANGUAGE         = "key_language";
    public static final String KEY_ONBOARDING        = "key_onboarding";
    public static final String KEY_ONBOARDING_SHOWN  = "key_onboarding"; // alias
    public static final String KEY_USER_PROFILE     = "key_user_profile";
    public static final String KEY_REMEMBER_ME      = "key_remember_me";
    public static final String KEY_SAVED_EMAIL      = "key_saved_email";
    public static final String KEY_SAVED_PASSWORD   = "key_saved_password";
    public static final String KEY_AVATAR           = "key_avatar";

    // Settings — notification toggles (default ON). Group "news" & "promo".
    public static final String KEY_NOTIF_NEWS_PUSH  = "key_notif_news_push";
    public static final String KEY_NOTIF_NEWS_EMAIL = "key_notif_news_email";
    public static final String KEY_NOTIF_NEWS_ZNS   = "key_notif_news_zns";
    public static final String KEY_NOTIF_NEWS_SMS   = "key_notif_news_sms";
    public static final String KEY_NOTIF_PROMO_PUSH  = "key_notif_promo_push";
    public static final String KEY_NOTIF_PROMO_EMAIL = "key_notif_promo_email";
    public static final String KEY_NOTIF_PROMO_ZNS   = "key_notif_promo_zns";
    public static final String KEY_NOTIF_PROMO_SMS   = "key_notif_promo_sms";

    // Settings — security
    public static final String KEY_PIN_UNLOCK       = "key_pin_unlock";

    // Intent extras
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_ORDER_ID   = "extra_order_id";
    public static final String EXTRA_CATEGORY   = "extra_category";

    // Languages
    public static final String LANG_VI          = "vi";
    public static final String LANG_EN          = "en";
    public static final String DEFAULT_LANGUAGE = LANG_VI;

    // Order statuses
    public static final String ORDER_PENDING   = "PENDING";
    public static final String ORDER_CONFIRMED = "CONFIRMED";
    public static final String ORDER_SHIPPING  = "SHIPPING";
    public static final String ORDER_DELIVERED = "DELIVERED";
    public static final String ORDER_CANCELLED = "CANCELLED";

    // Menh (Ngũ Hành)
    public static final String MENH_KIM  = "Kim";
    public static final String MENH_MOC  = "Mộc";
    public static final String MENH_THUY = "Thủy";
    public static final String MENH_HOA  = "Hỏa";
    public static final String MENH_THO  = "Thổ";

    // Misc
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final long AUTO_SCROLL_MS   = 4000L;
}
