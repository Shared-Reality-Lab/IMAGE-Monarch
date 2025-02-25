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
import static ca.mcgill.a11y.image.DataAndMethods.speaker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import org.json.JSONException;

import ca.mcgill.a11y.image.renderers.BasicPhotoMapRenderer;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.R;

// generates map request using latitude and longitude coordinates and allows for selecting among map renderer(s) 
public class MapSelector extends AppCompatActivity implements MediaPlayer.OnCompletionListener{
    private BrailleDisplay brailleServiceObj = null;


    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_selector);

        //mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        // Set the gesture detector as the double tap
        //mDetector.setOnDoubleTapListener(this);

        brailleServiceObj = DataAndMethods.brailleServiceObj;
        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
        DataAndMethods.update.observe(this,new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean changedVal) {
                if (changedVal){
                    startActivity(new Intent(getApplicationContext(), BasicPhotoMapRenderer.class));
                }
            }

        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        switch (DataAndMethods.keyMapping.getOrDefault(keyCode, "default")) {
            case "OK":
                try {
                    Double latitude= Double.parseDouble(((EditText) findViewById(R.id.latitude)).getText().toString());
                    Double longitude= Double.parseDouble(((EditText) findViewById(R.id.longitude)).getText().toString());
                    DataAndMethods.getMap(latitude, longitude);
                } catch (NumberFormatException e){
                    speaker(getResources().getString(R.string.invalid_coord), TextToSpeech.QUEUE_FLUSH);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                //startActivity(new Intent(getApplicationContext(), BasicPhotoMapRenderer.class));
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
        Log.d("ACTIVITY", "MapSelector Resumed");
        DataAndMethods.speaker(getResources().getString(R.string.res_map_selector), TextToSpeech.QUEUE_FLUSH);
        DataAndMethods.image= null;
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "MapSelector Paused");
        super.onPause();
    }
}
