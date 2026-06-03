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
//import static ca.mcgill.a11y.image.DataAndMethods.followingUp;
import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.silentStart;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager;
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix;
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget;

import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.PollingService;
import ca.mcgill.a11y.image.R;
import timber.log.Timber;

public class Exploration extends AppCompatActivity {
    private GestureDetectorCompat mDetector;
    private SelfBraillingWidget braillingWidget = null;
    private MutableLiveData<byte[][]> mutableViewedImage = new MutableLiveData<>();

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        braillingWidget = new SelfBraillingWidget(this);
        //braillingWidget.updateDimensionsFromBrailleDisplay(DataAndMethods.brailleServiceObj.getBrailleDisplayDotsSizeX(), DataAndMethods.brailleServiceObj.getBrailleDisplayDotsSizeY());
        setContentView(braillingWidget);
        if (getSupportActionBar() != null) { // makes widget full screen by hiding action bar
            getSupportActionBar().hide();
        }
        braillingWidget.setOnSelfBraillingWidgetListener(new SelfBraillingWidget.OnSelfBraillingWidgetListener() {
            @Override
            public void onFocused() {

            }

            @Override
            public void onDoubleTapAtBraillePosition(int pointX, int pointY) {
                Log.d("POINT", pointX + ", " + pointY);
                ArrayList<String[][]> tags = DataAndMethods.tags;
                Integer [] pins=DataAndMethods.pinCheck(pointX, pointY);
                try{
                    // Check if zooming mode is enabled
                    if (DataAndMethods.zoomingIn || DataAndMethods.zoomingOut) {
                        DataAndMethods.zoom(pins, "Exploration");
                    }
                    else {
                        // Log.d("TAGS", "ACCESS TTS");
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
                    Timber.e(ex, "TTS ERROR");
                } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
                    Timber.e(e, "EXCEPTION");
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSingleTapAtBraillePosition(int i, int i1) {
                return;
            }
        });

        mutableViewedImage.observe(this, it -> braillingWidget.refresh(it));
        //mutableLiveDots.observe(this, it -> DataAndMethods.brailleServiceObj.displayDots(new DotsMatrix(it)));
        //setContentView(R.layout.activity_main);
        //mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        // Set the gesture detector as the double tap
        //mDetector.setOnDoubleTapListener(this);

        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
        DataAndMethods.image = null;
        DataAndMethods.update.observe(this,new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean changedVal) {
                if (changedVal && DataAndMethods.image != null){
                    Log.d("UPDATE!", "Updating image in Exploration");
                    displayGraphic(confirmButton, "Exploration", silentStart);
                    mutableViewedImage.setValue(DataAndMethods.brailleScreen);
                    if (silentStart)
                        silentStart = false;
                }
            }

        });

        /*followingUp.observe(this,new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean changedVal) {
                if (!changedVal){
                    startService(new Intent(getApplicationContext(), PollingService.class));
                }
            }
        });*/
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
            super.onKeyDown(keyCode, event);
            Log.d("KEY EVENT", keyMapping.getOrDefault(keyCode, "default"));
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                // Navigating between files
                case "UP":
                case "DOWN":
                    // make force refresh
                    // Log.d("KEY EVENT", event.toString());
                    try {
                        DataAndMethods.checkForUpdate();
                    } catch (IOException | JSONException e) {
                        Timber.e(e, "EXCEPTION");
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
                case "BACK":
                    super.onBackPressed();
                    return true;
                default:
                    Timber.d("KEY EVENT: "+ event.toString());
                    return false;
            }
    }

    public void onLongPress(MotionEvent event) {
        Integer [] pins= DataAndMethods.pinCheck(event.getX(), event.getY());
        try{
            // Speak out detailed description based on finger location
            DataAndMethods.speaker(DataAndMethods.tags.get(1)[pins[1]][pins[0]], TextToSpeech.QUEUE_FLUSH);
        }
        catch(RuntimeException ex){
            Timber.e(ex, "TTS ERROR");
        }
        //Log.d("GESTURE!", "onLongPress: " + event.toString());

    }

    @Override
    public void onResume() {
        Timber.d("ACTIVITY: "+ "Exploration Resumed");
        if (!silentStart)
            DataAndMethods.speaker(getString(R.string.exploration_mode), TextToSpeech.QUEUE_FLUSH);

        //if (!followingUp.getValue()) {
            startService(new Intent(getApplicationContext(), PollingService.class));
        //}
        //brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
        super.onResume();
    }
    @Override
    public void onPause() {
        Timber.d("ACTIVITY: "+ "Exploration Paused");
        stopService(new Intent(getApplicationContext(), PollingService.class));
        //brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }

}
