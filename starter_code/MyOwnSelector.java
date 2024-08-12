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
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.core.view.GestureDetectorCompat;

import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.renderers.BasicPhotoMapRenderer;
import ca.mcgill.a11y.image.renderers.MyOwnRenderer;
import ca.mcgill.a11y.image.R;

// Launcher activity; switches mode of application
public class MyOwnSelector extends BaseActivity implements MediaPlayer.OnCompletionListener {
    private BrailleDisplay brailleServiceObj = null;
    private GestureDetectorCompat mDetector;


    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);

        // Code snippet 1

    }

    // Code snippet 2


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
        Log.d("ACTIVITY", "MyOwnSelector Resumed");
        DataAndMethods.speaker("My Own Selector");
        DataAndMethods.makePseudoServerCall();
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "MyOwnSelector Paused");
        super.onPause();
    }

}
