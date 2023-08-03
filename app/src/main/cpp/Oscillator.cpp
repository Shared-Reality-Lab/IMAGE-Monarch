#include "Oscillator.h"
#include <math.h>
#include <android/log.h>

#define TWO_PI (3.14159 * 2)
#define AMPLITUDE 0.3
#define FREQUENCY 440.0

void Oscillator::setSampleRate(int32_t sampleRate) {
    sampleRate_= sampleRate;
    phaseIncrement_ = (TWO_PI * frequency_) / (double) sampleRate;
}

void Oscillator::setWaveOn(bool isWaveOn) {
    isWaveOn_.store(isWaveOn);
}

void Oscillator::setFrequency(float freq) {
    frequency_ = freq;
    if (sampleRate_!=NULL) phaseIncrement_ = (TWO_PI * frequency_) / (double) sampleRate_;
}


void Oscillator::setBeat(int sqr){
    squareWave_= sqr;
    squareWave_= 1500+squareWave_*50;
}

void Oscillator::sonar(bool isWaveOn, float dist) {
    isWaveOn_.store(isWaveOn);
    amplitude_ = dist;
}

void Oscillator::render(float *audioData, int32_t numFrames) {

    if (!isWaveOn_.load()) phase_ = 0;
    // __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%d", squareWave_);
    for (int i = 0; i < numFrames; i++) {

        if (isWaveOn_.load()) {

            count_++;
            if (count_>= squareWave_*2)
                count_=0;
            float square;
            if ( (int)(count_ / squareWave_) != 0) {
                square = 0;
                // __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "HERE");
            }
            else {
                square = 1;
                // __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "NOW HERE");
            }
            float amp= amplitude_ * square;
            // Calculates the next sample value for the sine wave.
            // audioData[i] = (float) (sin(phase_) * AMPLITUDE);
            audioData[i] = (float) (sin(phase_) * amp);
            //audioData[i] = (float) (sin(phase_) * amplitude_);
            // Increments the phase, handling wrap around.
            phase_ += phaseIncrement_;
            if (phase_ > TWO_PI) phase_ -= TWO_PI;

        } else {
            // Outputs silence by setting sample value to zero.
            audioData[i] = 0;
        }
    }
}
