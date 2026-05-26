package com.tiredcity.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.tiredcity.app.data.model.UserProfile;

public class PreferenceManager {

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Token ──────────────────────────────────────────────────────────────────

    public void saveToken(String token) {
        prefs.edit().putString(Constants.KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    // ── User ID ────────────────────────────────────────────────────────────────

    public void saveUserId(String userId) {
        prefs.edit().putString(Constants.KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(Constants.KEY_USER_ID, null);
    }

    // ── Full UserProfile (JSON serialised) ────────────────────────────────────

    public void saveUser(UserProfile user) {
        prefs.edit().putString(Constants.KEY_USER_PROFILE, gson.toJson(user)).apply();
    }

    public UserProfile getUser() {
        String json = prefs.getString(Constants.KEY_USER_PROFILE, null);
        if (json == null) return null;
        return gson.fromJson(json, UserProfile.class);
    }

    // ── Menh (Five Elements) ──────────────────────────────────────────────────

    public void setMenh(String menh) {
        prefs.edit().putString(Constants.KEY_MENH, menh).apply();
    }

    public String getMenh() {
        return prefs.getString(Constants.KEY_MENH, null);
    }

    // ── Zodiac ────────────────────────────────────────────────────────────────

    public void setZodiac(String zodiac) {
        prefs.edit().putString(Constants.KEY_ZODIAC, zodiac).apply();
    }

    public String getZodiac() {
        return prefs.getString(Constants.KEY_ZODIAC, null);
    }

    // ── Language ──────────────────────────────────────────────────────────────

    public void setLanguage(String lang) {
        prefs.edit().putString(Constants.KEY_LANGUAGE, lang).apply();
    }

    public String getLanguage() {
        return prefs.getString(Constants.KEY_LANGUAGE, Constants.DEFAULT_LANGUAGE);
    }

    // ── Onboarding ────────────────────────────────────────────────────────────

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(Constants.KEY_ONBOARDING, done).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(Constants.KEY_ONBOARDING, false);
    }

    // Backward-compat aliases
    public void setOnboardingShown(boolean shown) { setOnboardingDone(shown); }
    public boolean isOnboardingShown()             { return isOnboardingDone(); }

    // ── Clear ─────────────────────────────────────────────────────────────────

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public void clearToken() {
        prefs.edit()
             .remove(Constants.KEY_TOKEN)
             .remove(Constants.KEY_USER_ID)
             .apply();
    }
}
