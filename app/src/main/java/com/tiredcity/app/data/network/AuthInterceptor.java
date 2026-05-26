package com.tiredcity.app.data.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private String token;

    public AuthInterceptor(String token) {
        this.token = token;
    }

    public void updateToken(String newToken) {
        this.token = newToken;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }

        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(original.method(), original.body())
                .build();

        return chain.proceed(request);
    }
}
