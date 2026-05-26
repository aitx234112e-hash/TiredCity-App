package com.tiredcity.app.data.network;

import com.tiredcity.app.utils.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static volatile Retrofit instance;
    private static String cachedToken = null;

    private ApiClient() {}

    public static synchronized Retrofit getInstance(String token) {
        if (instance == null || tokenChanged(token)) {
            cachedToken = token;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(token))
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }

    public static ApiService getApiService(String token) {
        return getInstance(token).create(ApiService.class);
    }

    public static synchronized void reset() {
        instance = null;
        cachedToken = null;
    }

    private static boolean tokenChanged(String newToken) {
        if (cachedToken == null && newToken == null) return false;
        if (cachedToken == null || newToken == null) return true;
        return !cachedToken.equals(newToken);
    }
}
