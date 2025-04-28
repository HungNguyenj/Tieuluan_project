package com.nlu.convertapp.api;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ViettelAiApi {
    @POST("tts/speech_synthesis")
    Call<ResponseBody> convertTextToSpeech(
            @Body RequestBody requestBody
    );
} 