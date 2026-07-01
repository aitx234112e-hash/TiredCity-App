package com.tiredcity.admin.data.remote;

import com.tiredcity.admin.data.model.DashboardOverview;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/**
 * Khai bao cac endpoint admin dung chung backend voi web.
 * (Bo sung dan theo cac module: products, orders...)
 */
public interface AdminApi {

    @GET("dashboard/overview")
    Call<DashboardOverview> getOverview(@Header("Authorization") String bearer);
}
