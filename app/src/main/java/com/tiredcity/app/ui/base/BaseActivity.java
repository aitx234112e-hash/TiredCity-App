package com.tiredcity.app.ui.base;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.PreferenceManager;

public abstract class BaseActivity extends AppCompatActivity {

    protected PreferenceManager preferenceManager;

    @Override
    protected void attachBaseContext(Context base) {
        // Apply saved locale before the activity inflates any layout.
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(this);
    }
}
