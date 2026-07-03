package com.tiredcity.admin;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Application class. Khoi tao Firebase (Firestore + Auth) tro toi CUNG project
 * voi web-admin va app khach hang (tiredcity-daf1e) de dung chung du lieu.
 *
 * Cau hinh duoc set thang bang code (FirebaseOptions) thay vi google-services.json
 * de app admin (package com.tiredcity.admin) khong can dang ky rieng tren console —
 * chi doc/ghi Firestore va dang nhap Email/Password nen khong yeu cau SHA-1.
 */
public class TiredCityAdminApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId("tiredcity-daf1e")
                    .setApplicationId("1:683649996737:android:73368172de04155a4dc140")
                    .setApiKey("AIzaSyAFKXern20RnlEdF_qbgp8n18q7KD2c4gc")
                    .setGcmSenderId("683649996737")
                    .setStorageBucket("tiredcity-daf1e.firebasestorage.app")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }
    }
}
