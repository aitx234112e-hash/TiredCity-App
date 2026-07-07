package com.tiredcity.app.data.network;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            try {
                // Fetch fresh token synchronously (with timeout)
                GetTokenResult result = Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS);
                token = result.getToken();
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                // Fallback to existing token if refresh fails
            }
        }

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
