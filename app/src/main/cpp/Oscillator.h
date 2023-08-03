#include <atomic>
#include <stdint.h>

class Oscillator {
public:
    void setWaveOn(bool isWaveOn);
    void sonar(bool isWaveOn, float dist);
    void setSampleRate(int32_t sampleRate);
    void render(float *audioData, int32_t numFrames);
    void setFrequency(float freq);
    void setBeat(int sqr);

private:
    std::atomic<bool> isWaveOn_{false};
    float amplitude_= 0.0;
    float frequency_= 440.0;
    float sampleRate_= NULL;
    uint32_t squareWave_= 1000;
    double phase_ = 0.0;
    double phaseIncrement_ = 0.0;
    uint32_t count_ =0;
};
