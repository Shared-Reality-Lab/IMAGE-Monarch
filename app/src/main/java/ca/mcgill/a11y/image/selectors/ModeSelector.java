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
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.GestureDetectorCompat;

import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager;
import com.humanware.keysoftsdk.ui.menu.AccessibleListView;
import com.humanware.keysoftsdk.ui.menu.accessibleitem.AccessibleItem;
import com.humanware.keysoftsdk.ui.menu.accessibleitem.attributes.AccessibleItemAttributes;

import ca.mcgill.a11y.image.BaseActivity;
import ca.mcgill.a11y.image.DataAndMethods;
import ca.mcgill.a11y.image.R;
import timber.log.Timber;

// Launcher activity; switches mode of application
public class ModeSelector extends BaseActivity implements MediaPlayer.OnCompletionListener {

    @Override
    public AccessibleListView.Sort initialize() {
        listview = inflateSettingItemLayout();
        return AccessibleListView.Sort.BY_POSITION;
    }

    private AccessibleListView inflateSettingItemLayout() {
        AccessibleListView listView = findViewById(com.humanware.keysoftsdk.R.id.settings_listview);
        listView.setNext(makeAccessibleItem(R.string.classroom_mode));
        listView.setNext(makeAccessibleItem(R.string.photo_mode));
        listView.setNext(makeAccessibleItem(R.string.map_mode));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Class<?> targetActivity;
            switch (position) {
                case 0:
                    targetActivity = ClassroomSelector.class;
                    DataAndMethods.speaker(getResources().getString(R.string.swi_classroom_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                case 1:
                    targetActivity = PhotoSelector.class;
                    DataAndMethods.speaker(getResources().getString(R.string.swi_photo_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                case 2:
                    targetActivity = MapSelector.class;
                    DataAndMethods.speaker(getResources().getString(R.string.swi_map_mode), TextToSpeech.QUEUE_FLUSH);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            startActivity(new Intent(this, targetActivity));
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
    public boolean onTouchEvent(MotionEvent event){
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
    }


    @Override
    public void onResume() {
        Timber.d("ACTIVITY: "+ "ModeSelector Resumed");
        DataAndMethods.speaker(getResources().getString(R.string.res_mode_selector), TextToSpeech.QUEUE_FLUSH);
        DataAndMethods.image = null;
        update.setValue(false);
        super.onResume();
    }
    @Override
    public void onPause() {
        Timber.d("ACTIVITY: "+ "ModeSelector Paused");
        super.onPause();
    }

}
