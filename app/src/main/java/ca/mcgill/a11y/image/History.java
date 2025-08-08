package ca.mcgill.a11y.image;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.util.Iterator;

// keeping track of requests history
public class History{
    String type;
    JSONObject request;
    String response;

    public void updateHistory(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("graphic")){
            this.type = "Photo";
        } else if (jsonObject.has("coordinates") || jsonObject.has("placeID")) {
            this.type = "Map";
        }
        this.request = jsonObject;
    }

    public void setResponse(String response){
        this.response = response;
    }

    public void clearHistory() {
        this.type = null;
        this.request = null;
    }
}
