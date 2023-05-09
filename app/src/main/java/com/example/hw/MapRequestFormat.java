package com.example.hw;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

// Format in which map requests are made to the server
public class MapRequestFormat {
    @SerializedName("request_uuid")
    private String Uuid= UUID.randomUUID().toString();
    @SerializedName("timestamp")
    private long timestamp = System.currentTimeMillis() / 1000L;
    @SerializedName("coordinates")
    private Coordinates coords=new Coordinates();
    @SerializedName("url")
    private String url= "https://fake.site.com/some-url";
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

    public class Coordinates{
        @SerializedName("latitude")
        Double lat;
        @SerializedName("longitude")
        Double lon;
    }
    public void setValues(Double lat, Double lon) throws JSONException {

        this.coords.lat = lat;
        this.coords.lon = lon;
    }
}
