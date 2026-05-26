package com.tiredcity.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.tiredcity.app.utils.Constants;

public class PreferenceManager {
    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.KEY_TOKEN, null);
    }

    public void clearToken() {
        prefs.edit().remove(Constants.KEY_TOKEN).remove(Constants.KEY_USER_ID).apply();
    }

    public void saveUserId(String userId) {
        prefs.edit().putString(Constants.KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(Constants.KEY_USER_ID, null);
    }

    public void saveLanguage(String languageCode) {
        prefs.edit().putString(Constants.KEY_LANGUAGE, languageCode).apply();
    }

    public String getLanguage() {
        return prefs.getString(Constants.KEY_LANGUAGE, Constants.DEFAULT_LANGUAGE);
    }

    public boolean isOnboardingShown() {
        return prefs.getBoolean(Constants.KEY_ONBOARDING_SHOWN, false);
    }

    public void setOnboardingShown(boolean shown) {
        prefs.edit().putBoolean(Constants.KEY_ONBOARDING_SHOWN, shown).apply();
    }
}
