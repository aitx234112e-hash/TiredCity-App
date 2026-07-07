package com.tiredcity.app.data.network;

import com.tiredcity.app.data.model.N8nRequest;
import com.tiredcity.app.data.model.N8nResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface N8nAgentService {

    /** POST toi URL webhook day du (nap tu BuildConfig.N8N_WEBHOOK_URL). */
    @POST
    Call<N8nResponse> ask(@Url String webhookUrl, @Body N8nRequest request);
}
