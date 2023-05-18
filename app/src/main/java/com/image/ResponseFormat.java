package com.image;

import com.google.gson.annotations.SerializedName;

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