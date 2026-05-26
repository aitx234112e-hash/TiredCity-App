package com.tiredcity.app.data.repository;

import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.utils.PreferenceManager;
import retrofit2.Call;
import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private final ApiService apiService;
    private final PreferenceManager preferenceManager;

    public AuthRepository(ApiService apiService, PreferenceManager preferenceManager) {
        this.apiService        = apiService;
        this.preferenceManager = preferenceManager;
    }

    public Call<ApiResponse<User>> login(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        return apiService.login(body);
    }

    public Call<ApiResponse<User>> register(String email, String password, String fullName) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("fullName", fullName);
        return apiService.register(body);
    }

    public void logout() {
        preferenceManager.clearToken();
    }

    public boolean isLoggedIn() {
        String token = preferenceManager.getToken();
        return token != null && !token.isEmpty();
    }
}
