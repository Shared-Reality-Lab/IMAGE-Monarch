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


import static ca.mcgill.a11y.image.DataAndMethods.backButton;
import static ca.mcgill.a11y.image.DataAndMethods.confirmButton;
import static ca.mcgill.a11y.image.DataAndMethods.displayGraphic;
import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.speaker;
import static ca.mcgill.a11y.image.DataAndMethods.update;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

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
import ca.mcgill.a11y.image.renderers.Exploration;
import ca.mcgill.a11y.image.renderers.Guidance;

public class ClassroomSelector extends BaseActivity implements MediaPlayer.OnCompletionListener {
    public static String channelSubscribed = "263773"; // make this the place the request takes it from

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_selector);

        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));

        ((Button) findViewById(R.id.exploration_mode)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.exploration_mode)).setOnFocusChangeListener(focusListener);
        ((Button) findViewById(R.id.guidance_mode)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.guidance_mode)).setOnFocusChangeListener(focusListener);
    }


    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View view, boolean b) {
            switch (view.getId()){
                case R.id.exploration_mode:
                    speaker("Exploration Mode");
                    break;
                case R.id.guidance_mode:
                    speaker("Guidance Mode");
                    break;
            }
        }
    };
    private View.OnKeyListener btnListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getKeyCode()== DataAndMethods.confirmButton &&
                    keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                Intent myIntent = null;
                if ((findViewById(R.id.exploration_mode)).hasFocus()){
                    myIntent = new Intent(getApplicationContext(), Exploration.class);
                    DataAndMethods.speaker("Switching to Exploration mode");
                }
                else if ((findViewById(R.id.guidance_mode)).hasFocus()){
                    myIntent = new Intent(getApplicationContext(), Guidance.class);
                    DataAndMethods.speaker("Switching to Guidance mode");
                }
                //Code snippet 3
                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(myIntent);
            }
            return false;
        }};

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
                return true;
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
        Log.d("ACTIVITY", "Classroom Selector Resumed");
        DataAndMethods.speaker("Classroom Selector");
        startService(new Intent(getApplicationContext(), PollingService.class));
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "Exploration Paused");
        stopService(new Intent(getApplicationContext(), PollingService.class));
        super.onPause();
    }

}
