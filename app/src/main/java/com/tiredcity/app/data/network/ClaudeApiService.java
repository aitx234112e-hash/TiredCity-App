package com.tiredcity.app.data.network;

import com.tiredcity.app.data.model.ClaudeRequest;
import com.tiredcity.app.data.model.ClaudeResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ClaudeApiService {

    @POST("v1/messages")
    Call<ClaudeResponse> createMessage(@Body ClaudeRequest request);
}
