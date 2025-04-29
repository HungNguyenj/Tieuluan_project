package com.nlu.convertapp.api;

import com.nlu.convertapp.models.SpeechToTextResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ElevenLabsApi {
    @POST("v1/text-to-speech/{voice_id}")
    Call<ResponseBody> convertTextToSpeech(
            @Path("voice_id") String voiceId,
            @Query("output_format") String outputFormat,
            @Header("xi-api-key") String apiKey,
            @Body RequestBody requestBody
    );
    
    @Multipart
    @POST("v1/speech-to-text")
    Call<SpeechToTextResponse> convertSpeechToText(
            @Header("xi-api-key") String apiKey,
            @Part("model_id") RequestBody modelId,
            @Part MultipartBody.Part file
    );
} 