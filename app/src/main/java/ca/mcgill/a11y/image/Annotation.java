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


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.scand.svg.SVGHelper;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Annotation extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    private BrailleDisplay brailleServiceObj = null;

    // keyCode of confirm button as per current standard
    int confirmButton = 504;
    int backButton = 503;
    SpeechRecognizer speechRecognizer;
    Intent speechRecognizerIntent;

    private GestureDetectorCompat mDetector;


    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ACTIVITY", "Annotation Created");
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);

        brailleServiceObj = DataAndMethods.brailleServiceObj;
        DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }



        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("SPEECHREC", "Listening");
                //Toast toast = Toast.makeText(getApplicationContext() , "Listening...", Toast.LENGTH_SHORT);
                //toast.show();
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                Log.d("SPEECHREC", String.valueOf(i));
                switch(i){
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        DataAndMethods.speaker("Failed to recognize text");
                    default:
                        DataAndMethods.speaker("Error occurred during speech recognition");
                }
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d("SPEECHREC", data.get(0));
                Toast toast = Toast.makeText(getApplicationContext() , data.get(0), Toast.LENGTH_SHORT);
                toast.show();
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        ((Button) findViewById(R.id.zeros)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.zeros)).hasFocus() &&
                        keyEvent.getKeyCode()==confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    DataAndMethods.ttsEnabled=false;
                    DataAndMethods.displayOn = false;
                    brailleServiceObj.display(DataAndMethods.data);
                }
                return false;
            }
        });

        ((Button) findViewById(R.id.mode)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.mode)).hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    Intent myIntent = new Intent(Annotation.this, Guidance.class);
                    //myIntent.putExtra("key", value); //Optional parameters
                    DataAndMethods.speaker("Switching to Guidance mode");
                    Annotation.this.startActivity(myIntent);

                }
                return false;
            }
        });

        ((Button) findViewById(R.id.ones)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.ones)).hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    try {
                        // Display current layer
                        DataAndMethods.ttsEnabled=true;
                        DataAndMethods.presentLayer++;
                        if (DataAndMethods.presentLayer==DataAndMethods.layerCount+1)
                            DataAndMethods.presentLayer=0;
                        DataAndMethods.displayOn = true;
                        brailleServiceObj.display(DataAndMethods.getAnnotationBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
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
                        DataAndMethods.presentLayer--;
                        if (DataAndMethods.presentLayer<0)
                            DataAndMethods.presentLayer= DataAndMethods.layerCount;
                        DataAndMethods.displayOn = true;
                        brailleServiceObj.display(DataAndMethods.getAnnotationBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
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
        });
    }

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


    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            int action = event.getActionMasked();
            if (action==MotionEvent.ACTION_UP)
            {
                ArrayList<String[][]> tags = DataAndMethods.occupancy;
                Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
                try{
                    // Check if zooming mode is enabled
                    if (DataAndMethods.zoomingIn || DataAndMethods.zoomingOut){
                        DataAndMethods.zoom(pins);
                    }
                    else {
                        DataAndMethods.fetchObjects(pins);
                        if (DataAndMethods.selectedIds!= null){
                            Intent myIntent = new Intent(Annotation.this, AnnotationMode.class);
                            DataAndMethods.speaker("Annotating");
                            Annotation.this.startActivity(myIntent);
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
            return true;
        }
        return super.onTouchEvent(event);
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
        DataAndMethods.pingsPlayer(R.raw.blip);
        speechRecognizer.startListening(speechRecognizerIntent);
        //speechRecognizer.stopListening();
        /*
        Integer [] pins= DataAndMethods.pinCheck(event.getX(), event.getY());
        try{
            // Speak out detailed description based on finger location
            //speechRecognizer.stopListening();
            DataAndMethods.speaker(DataAndMethods.tags.get(1)[pins[1]][pins[0]]);
        }
        catch(RuntimeException ex){
            Log.d("TTS ERROR", String.valueOf(ex));
        }*/
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
        /*Integer [] pins=pinCheck(event.getX(), event.getY());
        try{
            // Speak out label tags based on finger location
            speaker(tags.get(0)[pins[1]][pins[0]]);
        }
        catch(RuntimeException ex){
            Log.d(TAG, String.valueOf(ex));
        }*/
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
        /*
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                */
                //speechRecognizer.startListening(speechRecognizerIntent);
                //findViewById(android.R.id.content).post(new Runnable(){ public void run(){  }});
            /*} // This is your code
        };
        mainHandler.post(myRunnable);
        */
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

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        Log.d("ACTIVITY", "Annotation Resumed");

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        DataAndMethods.handler = e -> {
            if(DataAndMethods.ttsEnabled){
                try{
                    //Log.d("ACTIVITY", "Running registration on Annotation");
                    // This works! Gesture control can now be used along with the handler.
                    dispatchTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d("MOTION EVENT", String.valueOf(ex));
                }}

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
        if (DataAndMethods.displayOn){
            try {
                brailleServiceObj.display(DataAndMethods.getAnnotationBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
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
        Log.d("ACTIVITY", "Annotation Paused");
        brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }
}
