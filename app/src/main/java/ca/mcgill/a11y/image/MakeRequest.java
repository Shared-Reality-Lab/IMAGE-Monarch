package ca.mcgill.a11y.image;

import ca.mcgill.a11y.image.MapRequestFormat;
import ca.mcgill.a11y.image.PhotoRequestFormat;
import ca.mcgill.a11y.image.ResponseFormat;

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
