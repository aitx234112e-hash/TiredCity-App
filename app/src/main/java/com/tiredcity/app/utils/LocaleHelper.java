package com.tiredcity.app.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {
    private LocaleHelper() {}

    /**
     * Wraps a Context with the user's saved locale.
     * Call from Activity.attachBaseContext().
     */
    public static Context onAttach(Context context) {
        String lang = new PreferenceManager(context).getLanguage();
        return setLocale(context, lang);
    }

    /**
     * Applies the given language code and returns a new wrapped Context.
     */
    public static Context setLocale(Context context, String language) {
        if (language == null || language.isEmpty()) language = Constants.DEFAULT_LANGUAGE;
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            //noinspection deprecation
            context.getResources().updateConfiguration(config,
                    context.getResources().getDisplayMetrics());
            return context;
        }
    }

    /** Returns the currently saved language code. */
    public static String getCurrentLanguage(Context context) {
        return new PreferenceManager(context).getLanguage();
    }

    /**
     * Switches between Vietnamese and English, saves the choice,
     * and returns the new language code.
     */
    public static String toggleLanguage(Context context) {
        PreferenceManager prefs = new PreferenceManager(context);
        String current = prefs.getLanguage();
        String next = Constants.LANG_VI.equals(current) ? Constants.LANG_EN : Constants.LANG_VI;
        prefs.setLanguage(next);
        setLocale(context, next);
        return next;
    }

    // Alias for compatibility with BaseActivity
    public static Context wrap(Context context, String language) {
        return setLocale(context, language);
    }
}
