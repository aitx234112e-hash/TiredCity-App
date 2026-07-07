package com.tiredcity.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.utils.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

public class AuthRepository {
    private final ApiService apiService;
    private final PreferenceManager prefs;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AuthRepository(ApiService apiService, PreferenceManager prefs) {
        this.apiService = apiService;
        this.prefs = prefs;
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
        body.put("name", fullName);
        return apiService.register(body);
    }

    public void syncUserProfileToFirestore(UserProfile profile) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null || profile == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("uid", fUser.getUid());
        data.put("email", fUser.getEmail());
        data.put("name", profile.getName());
        data.put("phone", profile.getPhone());
        data.put("birthDate", profile.getBirthDate());
        data.put("address", profile.getAddress());
        data.put("province", profile.getProvince());
        data.put("district", profile.getDistrict());
        data.put("ward", profile.getWard());
        data.put("street", profile.getStreet());
        data.put("menh", profile.getMenh());
        data.put("zodiac", profile.getZodiac());
        data.put("animal", profile.getAnimal());
        data.put("role", "user");
        data.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(fUser.getUid()).set(data, SetOptions.merge());
    }

    public void syncAccount(UserProfile localProfile, OnSyncCallback callback) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        db.collection("users").document(fUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile cloudProfile = documentSnapshot.toObject(UserProfile.class);
                        if (cloudProfile != null) {
                            prefs.saveUser(cloudProfile);
                            if (callback != null) callback.onSuccess(cloudProfile);
                        }
                    } else {
                        createNewUserProfile(fUser, localProfile);
                        if (callback != null) callback.onSuccess(localProfile);
                    }
                });
    }

    private void createNewUserProfile(FirebaseUser fUser, UserProfile profile) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", fUser.getUid());
        data.put("email", fUser.getEmail());
        if (profile != null) {
            data.put("name", profile.getName());
            data.put("phone", profile.getPhone());
            data.put("birthDate", profile.getBirthDate());
            data.put("menh", profile.getMenh());
            data.put("zodiac", profile.getZodiac());
            data.put("animal", profile.getAnimal());
        } else {
            data.put("name", fUser.getDisplayName());
        }
        data.put("role", "user");
        data.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(fUser.getUid()).set(data, SetOptions.merge());
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        prefs.clearToken();
        prefs.clearCredentials();
    }

    public interface OnSyncCallback {
        void onSuccess(UserProfile profile);
    }
}
