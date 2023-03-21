package android.os;

import androidx.annotation.NonNull;
import android.view.MotionEvent;

public interface BrailleDisplay
{
    //must match BrailleDisplayService.java
    public static final String BRAILLE_DISPLAY_SERVICE = "braille";

    public interface MotionEventHandler {
        boolean handleMotionEvent(MotionEvent e);
    };

    int getDotPerLineCount();
    int getDotLineCount();

    /**
     * Update the whole braille display
     * @param data Dot array where each byte represent a dot, size must match display size
     */
    void display(byte[][] data);

    /**
     * Update a portion of braille display
     * @param data Dot array where each byte represent a dot
     * @param x coordinate where to place the dots in the display
     * @param y coordinate where to place the dots in the display
     */
    void display(byte[][] data, int x, int y);

    /**
     * Forces a refresh of all dots
     */
    void forceRefresh();

    void setDebugView(boolean enable);

    void registerMotionEventHandler(@NonNull MotionEventHandler handler);
    void unregisterMotionEventHandler(@NonNull MotionEventHandler handler);
}

