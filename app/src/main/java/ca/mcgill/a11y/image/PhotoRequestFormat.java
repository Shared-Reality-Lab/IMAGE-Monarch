package ca.mcgill.a11y.image;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

public class PhotoRequestFormat extends ca.mcgill.a11y.image.BaseRequestFormat {
    @SerializedName("graphic")
    private String graphic;
    @SerializedName("dimensions")
    private Integer[] dims;

    public void setValues(String base64, Integer[] dims) throws JSONException {
        this.graphic= base64;
        this.dims=dims;
    }

}
