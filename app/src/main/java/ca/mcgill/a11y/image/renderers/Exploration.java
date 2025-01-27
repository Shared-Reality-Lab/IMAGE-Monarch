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
package ca.mcgill.a11y.image.renderers;


import static ca.mcgill.a11y.image.DataAndMethods.backButton;
import static ca.mcgill.a11y.image.DataAndMethods.confirmButton;
import static ca.mcgill.a11y.image.DataAndMethods.displayGraphic;
import static ca.mcgill.a11y.image.DataAndMethods.followingUp;
import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.silentStart;
import static ca.mcgill.a11y.image.DataAndMethods.update;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import androidx.lifecycle.MutableLiveData;

import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.PollingService;
import ca.mcgill.a11y.image.R;

public class Exploration extends BaseActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    private BrailleDisplay brailleServiceObj = null;
    private GestureDetectorCompat mDetector;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        // Set the gesture detector as the double tap
        //mDetector.setOnDoubleTapListener(this);

        brailleServiceObj = DataAndMethods.brailleServiceObj;
        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
        DataAndMethods.image = null;
        DataAndMethods.update.observe(this,new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean changedVal) {
                if (changedVal && DataAndMethods.image != null){
                    displayGraphic(confirmButton, "Exploration", silentStart);
                    if (silentStart)
                        silentStart = false;
                }
            }

        });

        followingUp.observe(this,new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean changedVal) {
                if (!changedVal){
                    startService(new Intent(getApplicationContext(), PollingService.class));
                }
            }
        });
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
            super.onKeyDown(keyCode, event);
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                // Navigating between files
                case "UP":
                case "DOWN":
                    // make force refresh
                    Log.d("KEY EVENT", event.toString());
                    try {
                        DataAndMethods.checkForUpdate();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    //DataAndMethods.checkForUpdate();
                    return true;

                case "OK":
                    if (DataAndMethods.image!= null)
                        DataAndMethods.displayGraphic(confirmButton, "Exploration", false);
                    return false;

                case "CANCEL":
                    if (DataAndMethods.image!= null)
                        DataAndMethods.displayGraphic(backButton, "Exploration", false);
                    return false;
                case "MENU":
                    DataAndMethods.pingsPlayer(R.raw.blip);
                    DataAndMethods.speechRecognizer.startListening(DataAndMethods.speechRecognizerIntent);
                    return true;
                default:
                    Log.d("KEY EVENT", event.toString());
                    return false;
            }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            int action = event.getActionMasked();
            if (action==MotionEvent.ACTION_UP)
            {
                ArrayList<String[][]> tags = DataAndMethods.tags;
                Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
                try{
                    // Check if zooming mode is enabled
                    if (DataAndMethods.zoomingIn || DataAndMethods.zoomingOut){
                        DataAndMethods.zoom(pins, "Exploration");
                    }
                    else {
                        Log.d("TAGS", "ACCESS TTS");
                        // Speak out label tags based on finger location and ping when detailed description is available
                        if ((tags.get(1)[pins[1]][pins[0]] != null) && (tags.get(1)[pins[1]][pins[0]].trim().length() > 0)) {
                            //Log.d("CHECKING!", tags.get(1)[pins[1]][pins[0]]);
                            DataAndMethods.speaker(tags.get(0)[pins[1]][pins[0]], TextToSpeech.QUEUE_FLUSH, "ping");
                        } else {
                            //Log.d("CHECKING!", tags.get(0)[pins[1]][pins[0]]);
                            DataAndMethods.speaker(tags.get(0)[pins[1]][pins[0]], TextToSpeech.QUEUE_FLUSH);
                        }
                    }
                }
                catch(RuntimeException ex){
                    Log.d("TTS ERROR", String.valueOf(ex));
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d("GESTURE!","onDown: " + event.toString());

        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d("GESTURE!", "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Integer [] pins= DataAndMethods.pinCheck(event.getX(), event.getY());
        try{
            // Speak out detailed description based on finger location
            DataAndMethods.speaker(DataAndMethods.tags.get(1)[pins[1]][pins[0]], TextToSpeech.QUEUE_FLUSH);
        }
        catch(RuntimeException ex){
            Log.d("TTS ERROR", String.valueOf(ex));
        }
        Log.d("GESTURE!", "onLongPress: " + event.toString());

    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        Log.d("GESTURE!", "onScroll: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d("GESTURE!", "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d("GESTURE!", "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d("GESTURE!", "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d("GESTURE!", "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d("GESTURE!", "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
    }

    @Override
    protected void onResume() {
        Log.d("ACTIVITY", "Exploration Resumed");
        if (!silentStart)
            DataAndMethods.speaker("Exploration mode", TextToSpeech.QUEUE_FLUSH);

        if (!followingUp.getValue()) {
            startService(new Intent(getApplicationContext(), PollingService.class));
        }
        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        DataAndMethods.handler = e -> {
            if(DataAndMethods.ttsEnabled){
                try{
                    onTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d("MOTION EVENT", String.valueOf(ex));
                }}

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "Exploration Paused");
        stopService(new Intent(getApplicationContext(), PollingService.class));
        brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }

}
