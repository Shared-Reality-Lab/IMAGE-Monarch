package ca.mcgill.a11y.image;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class BaseRequestFormat {
    @SerializedName("request_uuid")
    private String Uuid= UUID.randomUUID().toString();
    @SerializedName("timestamp")
    private long timestamp = System.currentTimeMillis() / 1000L;
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
}
