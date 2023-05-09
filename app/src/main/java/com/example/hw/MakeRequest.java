package com.example.hw;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface MakeRequest {
    @Headers("Content-Type: application/json")
    @POST("render/")
    Call<ResponseFormat> makePhotoRequest(@Body PhotoRequestFormat req);

    @Headers("Content-Type: application/json")
    @POST("render/")
    Call<ResponseFormat> makeMapRequest(@Body MapRequestFormat req);
}