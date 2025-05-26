package com.nlu.convertapp.api;

import com.nlu.convertapp.models.SepayResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface SepayApi {
    @GET("userapi/transactions/list")
    Call<SepayResponse> getTransactions(@Header("Authorization") String token);
} 