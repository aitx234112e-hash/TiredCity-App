package com.tiredcity.app.data.network;

import com.tiredcity.app.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client rieng cho Anthropic Claude API (api.anthropic.com).
 * Key doc tu BuildConfig.CLAUDE_API_KEY (nap tu local.properties luc build).
 */
public final class ClaudeApiClient {

    private static final String BASE_URL          = "https://api.anthropic.com/";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static volatile ClaudeApiService service;

    private ClaudeApiClient() {}

    public static ClaudeApiService get() {
        if (service == null) {
            synchronized (ClaudeApiClient.class) {
                if (service == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(BuildConfig.DEBUG
                            ? HttpLoggingInterceptor.Level.BASIC
                            : HttpLoggingInterceptor.Level.NONE);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(chain -> {
                                Request req = chain.request().newBuilder()
                                        .header("x-api-key", BuildConfig.CLAUDE_API_KEY)
                                        .header("anthropic-version", ANTHROPIC_VERSION)
                                        .header("content-type", "application/json")
                                        .build();
                                return chain.proceed(req);
                            })
                            .addInterceptor(logging)
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();

                    service = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(ClaudeApiService.class);
                }
            }
        }
        return service;
    }

    /** True neu da cau hinh key hop le (chua cau hinh -> dung tra loi rule-based). */
    public static boolean isConfigured() {
        String key = BuildConfig.CLAUDE_API_KEY;
        return key != null && !key.isEmpty() && !key.startsWith("YOUR_");
    }
}
