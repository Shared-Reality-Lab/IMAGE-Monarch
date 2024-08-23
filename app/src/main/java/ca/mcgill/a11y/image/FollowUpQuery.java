package ca.mcgill.a11y.image;

import static ca.mcgill.a11y.image.DataAndMethods.keyMapping;
import static ca.mcgill.a11y.image.DataAndMethods.showRegion;

import androidx.core.view.GestureDetectorCompat;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class FollowUpQuery extends BaseActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    Intent intent;
    String query;
    Integer state;

    Integer[] region = null;
    private GestureDetectorCompat mDetector;
    private BrailleDisplay brailleServiceObj = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ACTIVITY", "FollowUpQuery Created");
        super.onCreate(savedInstanceState);
        brailleServiceObj = DataAndMethods.brailleServiceObj;

    }

    public void updateState() {
        state++;
        switch(state){
            case 0:
                Log.d("QUERY", query);
                DataAndMethods.speaker("Query received: "+query+" ...Click on top left corner of region of interest or press confirm to query without selection");
                break;
            case 1:
                //Log.d("STATE", "State 1");
                DataAndMethods.speaker("Click on bottom right corner of region of interest or press confirm to proceed without selection. Press cancel to return");
                break;
            case 2:
                //Log.d("STATE", "State 2");
                DataAndMethods.speaker("Press confirm to query for selected region. Press cancel to return to region selection");
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        switch (keyMapping.getOrDefault(keyCode, "default")) {
            case "OK":
                switch(state){
                    case 0:
                    case 2:
                        // Make request without region
                        DataAndMethods.sendFollowUpQuery(query, region);
                        finish();
                        break;
                    default:
                        DataAndMethods.pingsPlayer(R.raw.image_error);
                        break;
                }
                return false;
            case "CANCEL":
                switch(state){
                    case 1:
                    case 2:
                        state = -1;
                        updateState();
                        break;
                    default:
                        DataAndMethods.pingsPlayer(R.raw.image_error);
                        break;
                }
                return false;
            default:
                Log.d("KEY EVENT", event.toString());
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            int action = event.getActionMasked();
            if (action==MotionEvent.ACTION_UP)
            {
                Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
                switch(state){
                    case 0:
                        region = new Integer[]{0, 0, 0, 0};
                        region[0] = pins[0];
                        region[1]=pins[1];
                        updateState();
                        break;
                    case 1:
                        region[2] = pins[0];
                        region[3] = pins[1];
                        // Need to check for valid region here; Also set region to null if it is not...
                        if (DataAndMethods.validateRegion(region)){
                            try {
                                showRegion(region);
                            } catch (XPathExpressionException | ParserConfigurationException |
                                     IOException | SAXException e) {
                                Log.d("EXCEPTION", String.valueOf(e));
                                throw new RuntimeException(e);
                            }
                            updateState();
                        }else{
                            //DataAndMethods.speaker("Invalid region!");
                            region = null;
                            state = -1;
                            updateState();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
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
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d("GESTURE!", "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    protected void onResume() {
        Log.d("ACTIVITY", "FollowUpQuery Resumed");
        intent = getIntent();
        query = intent.getStringExtra("query");

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        DataAndMethods.handler = e -> {
                try{
                    onTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d("MOTION EVENT", String.valueOf(ex));
                }

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
        try {
            brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, false));
        } catch (IOException | XPathExpressionException | ParserConfigurationException |
                 SAXException e) {
            throw new RuntimeException(e);
        }
        state = -1;
        updateState();
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d("ACTIVITY", "FollowUpQuery Paused");
        DataAndMethods.presentLayer--;
        brailleServiceObj.unregisterMotionEventHandler(DataAndMethods.handler);
        super.onPause();
    }
}