package ca.a11y.mcgill.image;

import ca.a11y.mcgill.image.MapRequestFormat;
import ca.a11y.mcgill.image.PhotoRequestFormat;
import ca.a11y.mcgill.image.ResponseFormat;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

// Defines functions to be called to make photo or map requests
public interface MakeRequest {
    @Headers("Content-Type: application/json")
    @POST("render/")
    Call<ResponseFormat> makePhotoRequest(@Body PhotoRequestFormat req);

    @Headers("Content-Type: application/json")
    @POST("render/")
    Call<ResponseFormat> makeMapRequest(@Body MapRequestFormat req);
}
