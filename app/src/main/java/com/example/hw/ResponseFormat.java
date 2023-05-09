package com.example.hw;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

// Format of response from the server
public class ResponseFormat {
    @SerializedName("request_uuid")
    public String Uuid;
    @SerializedName("timestamp")
    public long timestamp;
    @SerializedName("renderings")
    public Rendering[] renderings=null;

    public class Rendering{
        @SerializedName("description")
        public String desc;
        @SerializedName("type_id")
        public String type_id;
        @SerializedName("data")
        public Data data;
    }
    public class Data{
        @SerializedName("graphic")
        public String graphic;
    }
}