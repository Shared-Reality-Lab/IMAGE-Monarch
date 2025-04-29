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

// response schema from server(s)
public class ResponseFormat {
    @SerializedName("request_uuid")
    public String Uuid;
    @SerializedName("timestamp")
    public long timestamp;
    @SerializedName("renderings")
    public Rendering[] renderings=null;
    @SerializedName("graphicBlob")
    public String graphicBlob;
    @SerializedName("coordinates")
    public String coords;
    @SerializedName("placeID")
    public String placeID;

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
        @SerializedName("layer")
        public String layer;

        @SerializedName("text")
        public String text;
    }

}