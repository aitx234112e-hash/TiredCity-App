package com.tiredcity.app.data.network;

import com.tiredcity.app.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client tro toi n8n AI Agent webhook.
 * URL webhook doc tu BuildConfig.N8N_WEBHOOK_URL (nap tu local.properties luc build),
 * gui thang qua @Url nen baseUrl chi la placeholder cho Retrofit.
 * Xem huong dan dung workflow: docs/n8n-ai-agent-setup.md
 */
public final class N8nAgentClient {

    private static volatile N8nAgentService service;

    private N8nAgentClient() {}

    /** URL webhook day du, dung khi goi service.ask(url(), request). */
    public static String url() {
        return BuildConfig.N8N_WEBHOOK_URL;
    }

    public static N8nAgentService get() {
        if (service == null) {
            synchronized (N8nAgentClient.class) {
                if (service == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(BuildConfig.DEBUG
                            ? HttpLoggingInterceptor.Level.BASIC
                            : HttpLoggingInterceptor.Level.NONE);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)   // Agent goi tool -> co the cham
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();

                    service = new Retrofit.Builder()
                            // baseUrl bat buoc phai hop le, nhung request thuc dung @Url.
                            .baseUrl("https://n8n.cloud/")
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(N8nAgentService.class);
                }
            }
        }
        return service;
    }

    /** True neu da cau hinh URL webhook hop le (chua cau hinh -> dung fallback rule-based). */
    public static boolean isConfigured() {
        String u = BuildConfig.N8N_WEBHOOK_URL;
        return u != null && u.startsWith("http") && !u.contains("YOUR_");
    }
}
