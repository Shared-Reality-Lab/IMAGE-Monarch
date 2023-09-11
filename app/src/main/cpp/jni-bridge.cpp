/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <android/input.h>
#include "AudioEngine.h"
#include <android/log.h>


static AudioEngine *audioEngine = new AudioEngine();

extern "C"
{
JNIEXPORT void JNICALL
Java_ca_mcgill_a11y_image_MainActivity_touchEvent(JNIEnv *env, jobject obj, jint action) {
    switch (action) {
        case AMOTION_EVENT_ACTION_DOWN:
            audioEngine->setToneOn(true);
            break;
        case AMOTION_EVENT_ACTION_UP:
            audioEngine->setToneOn(false);
            break;
        default:
            break;
    }
}
JNIEXPORT void JNICALL
Java_ca_mcgill_a11y_image_Guidance_startEngine(JNIEnv *env, jobject /* this */) {
    audioEngine->start();
}

JNIEXPORT void JNICALL
Java_ca_mcgill_a11y_image_Guidance_stopEngine(JNIEnv *env, jobject /* this */) {
    audioEngine->stop();
}

}
extern "C"
JNIEXPORT void JNICALL
Java_ca_mcgill_a11y_image_Guidance_guidance(JNIEnv *env, jobject thiz, jint action, jfloat amplitude, jfloat dist) {
    // TODO: implement guidance()
    switch (action) {
        case AMOTION_EVENT_ACTION_DOWN:
            audioEngine->guide(true, amplitude, 440.0- dist, dist);
            break;

        case AMOTION_EVENT_ACTION_UP:
            audioEngine->guide(false, amplitude, 440.0- dist,  dist);
            break;
        default:
            break;
    }
}
