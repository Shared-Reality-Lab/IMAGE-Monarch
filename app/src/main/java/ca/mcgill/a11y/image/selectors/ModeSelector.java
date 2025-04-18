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
import static ca.mcgill.a11y.image.DataAndMethods.update;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.GestureDetectorCompat;

import java.util.Locale;

import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.renderers.Exploration;
import ca.mcgill.a11y.image.R;

// Launcher activity; switches mode of application
public class ModeSelector extends BaseActivity implements MediaPlayer.OnCompletionListener {
    private BrailleDisplay brailleServiceObj = null;
    private GestureDetectorCompat mDetector;


    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selector);

        brailleServiceObj = DataAndMethods.brailleServiceObj;
        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));

        ((Button) findViewById(R.id.classroom_mode)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.classroom_mode)).setOnFocusChangeListener(focusListener);
        ((Button) findViewById(R.id.photo_mode)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.photo_mode)).setOnFocusChangeListener(focusListener);
        ((Button) findViewById(R.id.map_mode)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.map_mode)).setOnFocusChangeListener(focusListener);
        // Code snippet 1
        
    }

    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View view, boolean b) {
            switch (view.getId()){
                case R.id.classroom_mode:
                    speaker(getResources().getString(R.string.classroom_mode),TextToSpeech.QUEUE_FLUSH);
                    break;
                case R.id.photo_mode:
                    speaker(getResources().getString(R.string.photo_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                case R.id.map_mode:
                    speaker(getResources().getString(R.string.map_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                // Code snippet 2
            }
        }
    };
    private View.OnKeyListener btnListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getKeyCode()== DataAndMethods.confirmButton &&
                    keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                Intent myIntent = null;
                if ((findViewById(R.id.classroom_mode)).hasFocus()){
                    myIntent = new Intent(getApplicationContext(), ClassroomSelector.class);
                    DataAndMethods.speaker(getResources().getString(R.string.swi_classroom_mode), TextToSpeech.QUEUE_FLUSH);
                }
                else if ((findViewById(R.id.photo_mode)).hasFocus()){
                    myIntent = new Intent(getApplicationContext(), PhotoSelector.class);
                    DataAndMethods.speaker(getResources().getString(R.string.swi_photo_mode), TextToSpeech.QUEUE_FLUSH);

                }
                else if ((findViewById(R.id.map_mode)).hasFocus()){
                    myIntent = new Intent(getApplicationContext(), MapSelector.class);
                    DataAndMethods.speaker(getResources().getString(R.string.swi_map_mode), TextToSpeech.QUEUE_FLUSH);
                }
                //Code snippet 3
                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(myIntent);
            }
            return false;
        }};




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
        Log.d("ACTIVITY", "ModeSelector Resumed");
        DataAndMethods.speaker(getResources().getString(R.string.res_mode_selector), TextToSpeech.QUEUE_FLUSH);
        DataAndMethods.image = null;
        update.setValue(false);
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "ModeSelector Paused");
        super.onPause();
    }

}
