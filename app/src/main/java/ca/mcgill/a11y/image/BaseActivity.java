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
import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.speaker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.BrailleDisplay;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
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

// Base activity which is extended by all other activities. Implements functionality common to all/most activities
public class BaseActivity extends AppCompatActivity {
    static BrailleDisplay brailleServiceObj = null;
    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*Configuration newConfig = new Configuration();
        newConfig.setLocale(Locale.forLanguageTag("fr-CA"));
        onConfigurationChanged(newConfig);*/
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(getApplicationContext().getString(R.string.locale));
        AppCompatDelegate.setApplicationLocales(appLocale);

        // View Actions
        setContentView(R.layout.activity_main);
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        if (DataAndMethods.brailleServiceObj==null) {
            brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content), getResources());
        }
        else{
            brailleServiceObj = DataAndMethods.brailleServiceObj;
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content), getResources());
        }
        /*OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                finish();
            }

        };

        this.getOnBackPressedDispatcher().addCallback(onBackPressedCallback);*/

    }
    // check permissions required for voice recognition
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                case "ZOOM OUT":
                    Log.d("KEY EVENT", event.toString());
                    if (!DataAndMethods.zoomingOut) {
                        DataAndMethods.speaker(getResources().getString(R.string.on_zoom_mode), TextToSpeech.QUEUE_FLUSH);
                        DataAndMethods.zoomingOut = true;
                        DataAndMethods.zoomingIn = false;
                    } else {
                        DataAndMethods.zoomingOut = false;
                        DataAndMethods.speaker(getResources().getString(R.string.off_zoom_mode), TextToSpeech.QUEUE_FLUSH);
                    }
                    return true;
                case "ZOOM IN":
                    Log.d("KEY EVENT", event.toString());
                    if (!DataAndMethods.zoomingIn) {
                        DataAndMethods.speaker(getResources().getString(R.string.on_zoom_mode), TextToSpeech.QUEUE_FLUSH);
                        DataAndMethods.zoomingIn = true;
                        DataAndMethods.zoomingOut = false;
                    } else {
                        DataAndMethods.zoomingIn = false;
                        DataAndMethods.speaker(getResources().getString(R.string.off_zoom_mode), TextToSpeech.QUEUE_FLUSH);
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
                case "BACK":
                    //This prevents it from going out of the app with the back button
                    //Log.d("CLASS", getLocalClassName());
                    if (!"selectors.ModeSelector".equalsIgnoreCase(getLocalClassName()))
                        finish();
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

    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        Log.d("CONFIG", getApplicationContext().getString(R.string.app_name));
    }*/
}
