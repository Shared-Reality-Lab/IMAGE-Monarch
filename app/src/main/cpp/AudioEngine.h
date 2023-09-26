#include <aaudio/AAudio.h>
#include "Oscillator.h"

class AudioEngine {
public:
    bool start();
    void stop();
    void restart();
    void setToneOn(bool isToneOn);
    void guide(bool isToneOn, float amplitude, float dist, int beat, float angle);

private:
    Oscillator oscillator_;
    AAudioStream *stream_;
};
