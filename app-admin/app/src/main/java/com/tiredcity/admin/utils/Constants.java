package com.tiredcity.admin.utils;

import com.tiredcity.admin.BuildConfig;

public final class Constants {
    private Constants() {}

    /** Base URL backend NestJS (cau hinh trong build.gradle.kts). */
    public static final String API_BASE_URL = BuildConfig.API_BASE_URL;

    // SharedPreferences
    public static final String PREF_NAME = "tiredcity_admin";
    public static final String KEY_TOKEN = "access_token";
    public static final String KEY_ROLE = "role";
}
