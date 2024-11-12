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


import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;

import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.R;

// renders graphic currently stored in string 'image'
public class BasicPhotoMapRenderer extends BaseActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener  {

    private BrailleDisplay brailleServiceObj = null;

    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_photo_renderer);
        brailleServiceObj = DataAndMethods.brailleServiceObj;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        switch (keyMapping.getOrDefault(keyCode, "default")) {
            case "OK":
                if (DataAndMethods.followup){
                    try {
                        DataAndMethods.onShowFollowUp();
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    } catch (ParserConfigurationException e) {
                        throw new RuntimeException(e);
                    } catch (SAXException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    DataAndMethods.displayGraphic(DataAndMethods.confirmButton, "Exploration");
                }
                return true;
            case "CANCEL":
                if (DataAndMethods.followup){
                    DataAndMethods.followup = false;
                }
                else{
                DataAndMethods.displayGraphic(DataAndMethods.backButton, "Exploration");
                }
                return true;
            case "MENU":
                DataAndMethods.pingsPlayer(R.raw.blip);
                DataAndMethods.speechRecognizer.startListening(DataAndMethods.speechRecognizerIntent);
                return true;
            default:
                Log.d("KEY EVENT", event.toString());
                return true;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
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
    protected void onResume() {
        Log.d("ACTIVITY", "Exploration Resumed");

        DataAndMethods.displayGraphic(DataAndMethods.confirmButton, "Exploration");

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
        Log.d("ACTIVITY", "BasicPhotoMapRenderer Paused");
        brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }
}