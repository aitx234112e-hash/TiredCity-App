package com.tiredcity.app.utils;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Registers a custom Glide module for the app.
 * This enables Glide's annotation processor to generate the GlideApp class.
 */
@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    // No custom configuration needed; Glide defaults are sufficient.
    // To customise caching, disk cache strategy, or memory settings,
    // override applyOptions(Context, GlideBuilder) here.
}
