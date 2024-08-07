/*
 * Copyright (c) 2024 IMAGE Project, Shared Reality Lab, McGill University
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
package ca.mcgill.a11y.image.selectors;

import static ca.mcgill.a11y.image.DataAndMethods.fileSelected;
import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.pingsPlayer;
import static ca.mcgill.a11y.image.DataAndMethods.update;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import org.json.JSONException;
import java.io.IOException;
import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.renderers.BasicPhotoMapRenderer;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.R;

// generates photo request by navigating files in specified directory and allows for selecting among photo renderer(s)
public class PhotoSelector extends BaseActivity implements MediaPlayer.OnCompletionListener{


    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_photo_selector);

        //mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        // Set the gesture detector as the double tap
        //mDetector.setOnDoubleTapListener(this);

        //brailleServiceObj = DataAndMethods.brailleServiceObj;
        //DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //super.onKeyDown(keyCode, event);
        switch (keyMapping.getOrDefault(keyCode, "default")) {
            // Navigating between files
            case "UP":
                Log.d("KEY EVENT", event.toString());
                try {
                    DataAndMethods.speaker(DataAndMethods.getFile(++fileSelected));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return true;
            case "DOWN":
                Log.d("KEY EVENT", event.toString());
                try {
                    DataAndMethods.speaker(DataAndMethods.getFile(--fileSelected));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return true;
            case "OK":
                if (update.getValue())
                    {startActivity(new Intent(getApplicationContext(), BasicPhotoMapRenderer.class));}
                else
                    pingsPlayer(R.raw.image_error);
                return false;
            case "BACK":
                finish();
                return false;
            default:
                Log.d("KEY EVENT", event.toString());
                return false;
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
    }


    @Override
    protected void onResume() {
        Log.d("ACTIVITY", "PhotoSelector Resumed");
        DataAndMethods.speaker("Photo selector");
        try {
            // might want to read file name here
            DataAndMethods.getFile(++DataAndMethods.fileSelected);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "PhotoSelector Paused");
        super.onPause();
    }

}
