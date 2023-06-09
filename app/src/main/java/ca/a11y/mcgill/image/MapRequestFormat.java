package ca.a11y.mcgill.image;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

public class MapRequestFormat extends BaseRequestFormat{
    @SerializedName("coordinates")
    private Coordinates coords=new Coordinates();
    @SerializedName("url")
    private String url= "https://example-map-url.com";

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
