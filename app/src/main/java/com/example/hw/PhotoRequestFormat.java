package com.example.hw;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Format in which photo requests are made to the server
public class PhotoRequestFormat {
    @SerializedName("request_uuid")
    private String Uuid= UUID.randomUUID().toString();
    @SerializedName("timestamp")
    private long timestamp = System.currentTimeMillis() / 1000L;
    @SerializedName("graphic")
    private String graphic;
    @SerializedName("dimensions")
    private Integer[] dims;
    @SerializedName("context")
    private String context="";
    @SerializedName("language")
    private String lang="en";
    @SerializedName("capabilities")
    private String[] caps= new String[]{};
    @SerializedName("renderers")
    private String[] rends= new String[]{"ca.mcgill.a11y.image.renderer.TactileSVG"};
    @SerializedName("preprocessors")
    private JsonObject preps= new JsonObject();

    public void setValues(String base64, Integer[] dims) throws JSONException {
        this.graphic= base64;
        this.dims=dims;
    }
}