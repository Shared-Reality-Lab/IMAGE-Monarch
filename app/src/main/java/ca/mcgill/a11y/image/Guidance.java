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


import static android.view.KeyEvent.KEYCODE_ZOOM_IN;
import static android.view.KeyEvent.KEYCODE_ZOOM_OUT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class Guidance extends BaseActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    private BrailleDisplay brailleServiceObj = null;
    // keyCode of confirm button as per current standard
    int confirmButton = 504;
    int backButton = 503;

    private GestureDetectorCompat mDetector;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ACTIVITY", "Guidance Created");
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //startEngine();

        mDetector = new GestureDetectorCompat(this,this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);

        brailleServiceObj = DataAndMethods.brailleServiceObj;
        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));

        //InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        /*
        if (DataAndMethods.brailleServiceObj==null) {
            brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
            try {
                DataAndMethods.changeFile(++DataAndMethods.fileSelected);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            brailleServiceObj = DataAndMethods.brailleServiceObj;
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
        }

        DataAndMethods.handler = e -> {
            if(DataAndMethods.ttsEnabled){
                try{
                    Log.d("ACTIVITY", "Running registration on Guidance");
                    // This works! Gesture control can now be used along with the handler.
                    dispatchTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d("MOTION EVENT", String.valueOf(ex));
                }}

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
        */
/*
        ((Button) findViewById(R.id.zeros)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.zeros)).hasFocus() &&
                        keyEvent.getKeyCode()==confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    DataAndMethods.ttsEnabled=false;
                    DataAndMethods.displayOn=false;
                    brailleServiceObj.display(DataAndMethods.data);
                }
                return false;
            }
        });
*/

/*
        ((Button) findViewById(R.id.ones)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.ones)).hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    try {
                        // Display current layer
                        DataAndMethods.ttsEnabled=true;
                        DataAndMethods.presentTarget++;
                        DataAndMethods.displayOn= true;
                        //Log.d("PRESENT TARGET", String.valueOf(DataAndMethods.presentTarget));
                        brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ParserConfigurationException e) {
                        throw new RuntimeException(e);
                    } catch (SAXException e) {
                        throw new RuntimeException(e);
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (((Button) findViewById(R.id.ones)).hasFocus() &&
                        keyEvent.getKeyCode()== backButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    try {
                        // Display current layer
                        DataAndMethods.ttsEnabled=true;
                        DataAndMethods.presentTarget--;
                        DataAndMethods.displayOn= true;
                        brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ParserConfigurationException e) {
                        throw new RuntimeException(e);
                    } catch (SAXException e) {
                        throw new RuntimeException(e);
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                }

                return false;
            }
        });

        Switch debugSwitch = (Switch) findViewById(R.id.debugViewSwitch);
        debugSwitch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (debugSwitch.hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    if (debugSwitch.isChecked()){
                        debugSwitch.setChecked(false);
                    }
                    else{
                        debugSwitch.setChecked(true);
                    }
                    brailleServiceObj.setDebugView(debugSwitch.isChecked());
                    //audioPlayer("/sdcard/IMAGE/", "audio.mp3");
                }
                return false;
            }
        });
        debugSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                debugSwitch.setChecked(!debugSwitch.isChecked());
            }
        });*/
    }

    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
                put(421, "UP");
                put(420, "DOWN");
            }};
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                // Navigating between files
                case "UP":
                    Log.d("KEY EVENT", event.toString());
                    DataAndMethods.changeFile(++DataAndMethods.fileSelected);
                    return true;
                // Navigating between files
                case "DOWN":
                    Log.d("KEY EVENT", event.toString());
                    DataAndMethods.changeFile(--DataAndMethods.fileSelected);
                    return true;
                default:
                    Log.d("KEY EVENT", event.toString());
                    return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
*/


    @Override
    public boolean onTouchEvent(MotionEvent event){
        /*if (this.mDetector.onTouchEvent(event)) {
            int action = event.getActionMasked();
            //guidance(event.getAction(), 0.5F, 10);


            if (DataAndMethods.target!=null)
            {
                Log.d("TOUCH EVENT", "HERE!");
                Integer [] pins= DataAndMethods.pinCheck(event.getX(), event.getY());
                //Log.d("GUIDANCE PINS", pins[0]+ ","+ pins[1]);
                //Log.d("GUIDANCE TARGET", DataAndMethods.target[0]+ ","+ DataAndMethods.target[1]);
                float dist = DataAndMethods.calcDistance(pins, DataAndMethods.target);
                float angle = DataAndMethods.calcAngle(pins, DataAndMethods.target);
                float amplitude = (float)Math.pow(1.1, 5*((11000-dist)/10000));
                // Log.d("DISTANCE", String.valueOf(Math.pow(dist, 0.5)));
                // Log.d("DISTANCE", String.valueOf(amplitude));
                // Log.d("ACTION", String.valueOf(event.getAction()));
                //Log.d("GUIDANCE", amplitude + ","+ (float) Math.pow(dist, 0.5));
                if ((float)Math.pow(dist, 0.5)>DataAndMethods.target[2]) {
                    guidance(event.getAction(), amplitude, (float) Math.pow(dist, 0.5), angle);
                    //guidance(event.getAction(), 0.5F, 10);
                }
                else{
                    guidance(KeyEvent.ACTION_UP, amplitude, (float)Math.pow(dist, 0.5), angle);
                    DataAndMethods.pingsPlayer(R.raw.success);
                }

            }
            return true;
        return super.dispatchTouchEvent(event);        }*/
        super.onTouchEvent(event);
        if (this.mDetector.onTouchEvent(event)) {
            int action = event.getActionMasked();
            if (action==MotionEvent.ACTION_UP)
            {
                //ArrayList<String[][]> tags = DataAndMethods.tags;
                Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
                try{
                    // Check if zooming mode is enabled
                    if (DataAndMethods.zoomingIn || DataAndMethods.zoomingOut){
                        DataAndMethods.zoom(pins, "Guidance");
                    }
                    /*else {
                        // Speak out label tags based on finger location and ping when detailed description is available
                        if ((tags.get(1)[pins[1]][pins[0]] != null) && (tags.get(1)[pins[1]][pins[0]].trim().length() > 0)) {
                            //Log.d("CHECKING!", tags.get(1)[pins[1]][pins[0]]);
                            DataAndMethods.speaker(tags.get(0)[pins[1]][pins[0]], "ping");
                        } else {
                            DataAndMethods.speaker(tags.get(0)[pins[1]][pins[0]]);
                        }
                    }*/
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
        try {
            brailleServiceObj.display(DataAndMethods.displayTargetLayer(DataAndMethods.getfreshDoc()));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
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


    @SuppressLint("WrongConstant")
    @Override
    protected void onResume() {
        Log.d("ACTIVITY", "Guidance Resumed");

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        DataAndMethods.handler = e -> {
            if(DataAndMethods.ttsEnabled){
                try{
                    //Log.d("ACTIVITY", "Running registration on Guidance");
                    // This works! Gesture control can now be used along with the handler.
                    onTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d("MOTION EVENT", String.valueOf(ex));
                }}

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
        if (DataAndMethods.displayOn){
            try {
                brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer));
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
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "Guidance Paused");
        //stopEngine();
        brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }

}
