package com.nlu.convertapp.api;

import com.nlu.convertapp.models.ViettelSpeechToTextResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ViettelAsrApi {
    @Multipart
    @POST("asr/recognize")
    Call<ViettelSpeechToTextResponse> convertSpeechToText(
            @Part MultipartBody.Part file,
            @Part("token") RequestBody token
    );
} 