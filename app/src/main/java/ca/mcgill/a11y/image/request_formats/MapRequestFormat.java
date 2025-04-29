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
package ca.mcgill.a11y.image.request_formats;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

// map request schema to IMAGE-server
public class MapRequestFormat extends BaseRequestFormat {
    @SerializedName("coordinates")
    private Coordinates coords=new Coordinates();
    @SerializedName("url")
    private String url= "https://example-map-url.com";
    @SerializedName("placeID")
    public String placeID;

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
    public void setPlaceID(String placeID){
        this.placeID = placeID;
    }
}
