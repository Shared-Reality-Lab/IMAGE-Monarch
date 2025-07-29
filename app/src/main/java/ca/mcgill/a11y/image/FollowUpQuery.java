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
package ca.mcgill.a11y.image;

import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.showRegion;
import static ca.mcgill.a11y.image.DataAndMethods.silentStart;
import static ca.mcgill.a11y.image.DataAndMethods.speaker;
import static ca.mcgill.a11y.image.DataAndMethods.titleRead;

import androidx.core.view.GestureDetectorCompat;

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

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import timber.log.Timber;

public class FollowUpQuery extends BaseActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    Intent intent;
    String query;

    Integer state;

    Integer[] region = null;
    private GestureDetectorCompat mDetector;
    private BrailleDisplay brailleServiceObj = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("ACTIVITY: "+ "FollowUpQuery Created");
        super.onCreate(savedInstanceState);
        brailleServiceObj = DataAndMethods.brailleServiceObj;

    }

    public void updateState() {
        state++;
        switch(state){
            case 0:
                // Log.d("QUERY", query);
                try {
                    brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, false));
                } catch (IOException | XPathExpressionException | ParserConfigurationException |
                         SAXException e) {
                    Timber.e(e, "EXCEPTION");
                    throw new RuntimeException(e);
                }
                DataAndMethods.speaker(getResources().getString(R.string.received_query_0)+query+" "+getResources().getString(R.string.received_query_1_temp), TextToSpeech.QUEUE_FLUSH);
                        //getResources().getString(R.string.received_query_1), TextToSpeech.QUEUE_FLUSH);
                break;
            case 1:
                //Log.d("STATE", "State 1");
                DataAndMethods.speaker(getResources().getString(R.string.state_1), TextToSpeech.QUEUE_FLUSH);
                break;
            case 2:
                //Log.d("STATE", "State 2");
                DataAndMethods.speaker(getResources().getString(R.string.state_2), TextToSpeech.QUEUE_FLUSH);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        switch (keyMapping.getOrDefault(keyCode, "default")) {
            case "OK":
                switch(state){
                    case 0:
                    case 2:
                        // Make request without region
                        try {
                            DataAndMethods.sendFollowUpQuery(query, region);
                        } catch (JSONException | IOException e) {
                            Timber.e(e, "EXCEPTION");
                            throw new RuntimeException(e);
                        }
                        titleRead = false;
                        silentStart = true;
                        // cut the speech
                        speaker("", TextToSpeech.QUEUE_FLUSH);
                        finish();
                        break;
                    default:
                        DataAndMethods.pingsPlayer(R.raw.image_error);
                        break;
                }
                return true;
            case "CANCEL":
                switch(state){
                    case 1:
                    case 2:
                        state = -1;
                        updateState();
                        break;
                    default:
                        DataAndMethods.pingsPlayer(R.raw.image_error);
                        DataAndMethods.update.setValue(true);
                        finish();
                        break;
                }
                return true;
            case "BACK":
                DataAndMethods.update.setValue(true);
                finish();
                return true;
            default:
                Timber.d("KEY EVENT: "+ event.toString());
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
                        //Log.d("TAGS", "ACCESS TTS");
                        // Speak out label tags based on finger location and ping when detailed description is available
                        if ((tags.get(1)[pins[1]][pins[0]] != null) && (tags.get(1)[pins[1]][pins[0]].trim().length() > 0)) {
                            //Log.d("CHECKING!", tags.get(1)[pins[1]][pins[0]]);
                            DataAndMethods.speaker(tags.get(0)[pins[1]][pins[0]], TextToSpeech.QUEUE_FLUSH,"ping");
                        } else {
                            //Log.d("CHECKING!", tags.get(0)[pins[1]][pins[0]]);
                            DataAndMethods.speaker(tags.get(0)[pins[1]][pins[0]], TextToSpeech.QUEUE_FLUSH);
                        }
                    }
                }
                catch(RuntimeException ex){
                    Timber.d(ex, "TTS ERROR");
                } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
                    Timber.e(e, "EXCEPTION");
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
    }

    @Override
    public boolean onDown(MotionEvent event) {
        //Log.d("GESTURE!","onDown: " + event.toString());

        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        //Log.d("GESTURE!", "onFling: " + event1.toString() + event2.toString());
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
            Timber.e(ex, "TTS ERROR");
        }
        // Log.d("GESTURE!", "onLongPress: " + event.toString());

    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        //Log.d("GESTURE!", "onScroll: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        //Log.d("GESTURE!", "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        //Log.d("GESTURE!", "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Timber.d("GESTURE!: "+ "onDoubleTap: " + event.toString());
        Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
        /*switch(state){
            case 0:
                region = new Integer[]{0, 0, 0, 0};
                region[0] = pins[0];
                region[1]=pins[1];
                updateState();
                break;
            case 1:
                region[2] = pins[0];
                region[3] = pins[1];
                // Need to check for valid region here; Also set region to null if it is not...
                if (DataAndMethods.validateRegion(region)){
                    try {
                        showRegion(region);
                    } catch (XPathExpressionException | ParserConfigurationException |
                             IOException | SAXException e) {
                        Log.d("EXCEPTION", String.valueOf(e));
                        throw new RuntimeException(e);
                    }
                    updateState();
                }else{
                    //DataAndMethods.speaker("Invalid region!");
                    region = null;
                    state = -1;
                    updateState();
                }
                break;
            default:
                break;
        }*/
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        //Log.d("GESTURE!", "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        //Log.d("GESTURE!", "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    protected void onResume() {
        Timber.d("ACTIVITY: "+ "FollowUpQuery Resumed");
        intent = getIntent();
        query = intent.getStringExtra("query");

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        DataAndMethods.handler = e -> {
                try{
                    onTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Timber.e(ex, "EXCEPTION");
                }

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);

        state = -1;
        updateState();
        super.onResume();
    }
    @Override
    protected void onPause() {
        Timber.d("ACTIVITY: "+ "FollowUpQuery Paused");
        DataAndMethods.presentLayer--;
        brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }
}