package com.example.aichatapp;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {

    @POST("models/gemini-1.5-flash:generateContent")
    Call<ResponseBody> getReply(
            @Query("key") String apiKey,
            @Body RequestBody body
    );
}
