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
import android.speech.tts.TextToSpeech;
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

import com.humanware.keysoftsdk.ui.menu.AccessibleListView;
import com.humanware.keysoftsdk.ui.menu.accessibleitem.AccessibleItem;
import com.humanware.keysoftsdk.ui.menu.accessibleitem.attributes.AccessibleItemAttributes;

import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.PollingService;
import ca.mcgill.a11y.image.R;
import ca.mcgill.a11y.image.renderers.Exploration;
import ca.mcgill.a11y.image.renderers.Guidance;
import timber.log.Timber;

public class ClassroomSelector extends BaseActivity implements MediaPlayer.OnCompletionListener {
    public static String channelSubscribed; // make this the place the request takes it from

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_classroom_selector);


        channelSubscribed = getApplicationContext().getString(R.string.share_code);

        // DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
    }

    @Override
    public AccessibleListView.Sort initialize() {
        listview = inflateSettingItemLayout();
        return AccessibleListView.Sort.BY_POSITION;
    }

    private AccessibleListView inflateSettingItemLayout() {
        AccessibleListView listView = findViewById(com.humanware.keysoftsdk.R.id.settings_listview);
        listView.setNext(makeAccessibleItem(R.string.exploration_mode));
        listView.setNext(makeAccessibleItem(R.string.guidance_mode));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Class<?> targetActivity;

            switch (position) {
                case 0:
                    targetActivity = Exploration.class;
                    //DataAndMethods.speaker(getResources().getString(R.string.swi_exploration_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                case 1:
                    targetActivity = Guidance.class;
                    //DataAndMethods.speaker(getResources().getString(R.string.swi_guidance_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            Intent myIntent = new Intent(this, targetActivity);
            //Code snippet 3
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(myIntent);
        });

        return listView;
    }

    private AccessibleItem makeAccessibleItem(
            int labelNameId
    ) {
        return new AccessibleItem(
                new AccessibleItemAttributes(
                        getResources().getString(labelNameId),
                        com.humanware.keysoftsdk.R.layout.one_textview,
                        com.humanware.keysoftsdk.R.id.textview
                )
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyMapping.getOrDefault(keyCode, "default")) {
            // Navigating between files
            case "UP":
            case "DOWN":
                // make force refresh
                //Log.d("KEY EVENT", event.toString());
                try {
                    DataAndMethods.checkForUpdate();
                } catch (IOException | JSONException e) {
                    Timber.e(e, "EXCEPTION");
                    throw new RuntimeException(e);
                }
                return true;
            case "BACK":
                finish();
                return false;
            default:
                Timber.d("KEY EVENT: "+ event.toString());
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
    public void onResume() {
        Timber.d("ACTIVITY: "+ "Classroom Selector Resumed");
        //DataAndMethods.speaker(getResources().getString(R.string.res_classroom_selector), TextToSpeech.QUEUE_FLUSH);
        //startService(new Intent(getApplicationContext(), PollingService.class));
        super.onResume();
    }
    @Override
    public void onPause() {
        Timber.d("ACTIVITY: "+ "Exploration Paused");
        //stopService(new Intent(getApplicationContext(), PollingService.class));
        super.onPause();
    }

}
