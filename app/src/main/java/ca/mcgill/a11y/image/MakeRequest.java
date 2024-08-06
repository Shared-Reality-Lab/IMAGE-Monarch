/*
 * Copyright (c) 2023 IMAGE Project, Shared Reality Lab, McGill University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * and our Additional Terms along with this program.
 * If not, see <https://github.com/Shared-Reality-Lab/IMAGE-Monarch/LICENSE>.
 */
package ca.mcgill.a11y.image;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

// Defines functions to be called to make photo, map, and update requests
public interface MakeRequest {
    //String url = "display/"+channelSubscribed;
    @Headers("Content-Type: application/json")
    @GET
    Call<ResponseFormat> checkForUpdates(@Url String url);

    @Headers("Content-Type: application/json")
    @POST("render/")
    Call<ResponseFormat> makePhotoRequest(@Body PhotoRequestFormat req);

    @Headers("Content-Type: application/json")
    @POST("render/")
    Call<ResponseFormat> makeMapRequest(@Body MapRequestFormat req);
}
