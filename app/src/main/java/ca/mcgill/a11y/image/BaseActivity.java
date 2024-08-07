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

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_MENU;
import static android.view.KeyEvent.KEYCODE_ZOOM_IN;
import static android.view.KeyEvent.KEYCODE_ZOOM_OUT;

import static ca.mcgill.a11y.image.DataAndMethods.backButton;
import static ca.mcgill.a11y.image.DataAndMethods.confirmButton;
import static ca.mcgill.a11y.image.DataAndMethods.displayGraphic;
import static ca.mcgill.a11y.image.DataAndMethods.speaker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BrailleDisplay;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class BaseActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{
    static BrailleDisplay brailleServiceObj = null;
    int zoomVal = 100;
    static String channelSubscribed = "263773";
    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // View Actions
        setContentView(R.layout.activity_main);
        if (DataAndMethods.brailleServiceObj==null) {
            brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
            startService(new Intent(getApplicationContext(), PollingService.class));
        }
        else{
            brailleServiceObj = DataAndMethods.brailleServiceObj;
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getApplicationContext(), PollingService.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
                put(421, "UP");
                put(420, "DOWN");
                put(KEYCODE_ZOOM_OUT, "ZOOM OUT");
                put(KEYCODE_ZOOM_IN, "ZOOM IN");
                put(KEYCODE_DPAD_UP, "DPAD UP");
                put(KEYCODE_DPAD_DOWN, "DPAD DOWN");
                put(KEYCODE_DPAD_LEFT, "DPAD LEFT");
                put(KEYCODE_DPAD_RIGHT, "DPAD RIGHT");
                put(KEYCODE_MENU, "MENU");
                put(confirmButton, "OK");
                put(backButton, "CANCEL");
            }};
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                // Navigating between files
                case "UP":
                case "DOWN":
                    // make force refresh
                    Log.d("KEY EVENT", event.toString());
                    try {
                        DataAndMethods.getFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    //DataAndMethods.getFile();
                    return true;

                case "ZOOM OUT":
                    Log.d("KEY EVENT", event.toString());
                    if (!DataAndMethods.zoomingOut) {
                        DataAndMethods.speaker("Zoom mode enabled");
                        DataAndMethods.zoomingOut = true;
                        DataAndMethods.zoomingIn = false;
                    } else {
                        DataAndMethods.zoomingOut = false;
                        DataAndMethods.speaker("Zoom mode disabled");
                    }
                    return true;
                case "ZOOM IN":
                    Log.d("KEY EVENT", event.toString());
                    if (!DataAndMethods.zoomingIn) {
                        DataAndMethods.speaker("Zoom mode enabled");
                        DataAndMethods.zoomingIn = true;
                        DataAndMethods.zoomingOut = false;
                    } else {
                        DataAndMethods.zoomingIn = false;
                        DataAndMethods.speaker("Zoom mode disabled");
                    }
                    return true;
                case "DPAD UP":
                case "DPAD DOWN":
                case "DPAD LEFT":
                case "DPAD RIGHT":
                    if (DataAndMethods.zoomVal > 100 && (DataAndMethods.zoomingIn || DataAndMethods.zoomingOut)) {
                        Log.d("DPAD", String.valueOf(keyCode));
                        DataAndMethods.pan(keyCode, getLocalClassName());
                    }
                    return false;

                case "OK":
                    DataAndMethods.displayGraphic(confirmButton, "Exploration");
                    return false;

                case "CANCEL":
                    DataAndMethods.displayGraphic(backButton, "Exploration");
                    return false;
                default:
                    Log.d("KEY EVENT", event.toString());
                    return false;
            }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
  
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }
}
