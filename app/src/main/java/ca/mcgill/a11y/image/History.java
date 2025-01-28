package ca.mcgill.a11y.image;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.util.Iterator;

// keeping track of requests history
public class History{
    String type;
    JSONObject request;
    String temp_type;
    JSONObject temp_request;

    String response;

    /*public void updateHistory(Object req) throws JSONException {
        Gson gson = new Gson();
        String json = gson.toJson(req);
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("graphic")){
            this.temp_type = "Photo";
        } else if (jsonObject.has("coordinates")) {
            this.temp_type = "Map";
        }
        this.temp_request = jsonObject;
    }
    public void setHistory(Boolean set){
        if (set){
            this.type = this.temp_type;
            this.request = this.temp_request;
        }
        this.temp_type = null;
        this.temp_request = null;
        // Log.d("HISTORY", history() );
    }*/
    public void updateHistory(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("graphic")){
            this.type = "Photo";
        } else if (jsonObject.has("coordinates")) {
            this.type = "Map";
        }
        this.request = jsonObject;

        /*Iterator<String> iterator = jsonObject.keys(); // Your Iterator<String> object
        StringBuilder sb = new StringBuilder();

        while (iterator.hasNext()) {
            sb.append(iterator.next()).append(", "); // Append each element
        }
        Log.d("KEYS", sb.toString());*/
    }

    // public String history(){
    //    return this.type + ", " + this.request;
    // }

    public void setResponse(String response){
        this.response = response;
    }


}
