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
package ca.mcgill.a11y.image;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

public class PhotoRequestFormat extends BaseRequestFormat {
    @SerializedName("graphic")
    private String graphic;
    @SerializedName("dimensions")
    private Integer[] dims;

    public void setValues(String base64, Integer[] dims) throws JSONException {
        this.graphic= base64;
        this.dims=dims;
    }

}
