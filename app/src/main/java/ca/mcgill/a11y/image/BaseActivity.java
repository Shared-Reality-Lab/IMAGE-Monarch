package ca.mcgill.a11y.image;

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_MENU;
import static android.view.KeyEvent.KEYCODE_ZOOM_IN;
import static android.view.KeyEvent.KEYCODE_ZOOM_OUT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BrailleDisplay;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

public class BaseActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{
    SpeechRecognizer speechRecognizer;
    Intent speechRecognizerIntent;

    static BrailleDisplay brailleServiceObj = null;
    int confirmButton = 504;
    int backButton = 503;
    String voiceCommand = null;

    private GestureDetectorCompat mDetector;

    boolean zoom = false;
    int zoomVal = 100;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Actions
        setContentView(R.layout.activity_main);



        if (DataAndMethods.brailleServiceObj==null) {
            brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
            try {
                DataAndMethods.getFile();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            brailleServiceObj = DataAndMethods.brailleServiceObj;
            DataAndMethods.initialize(brailleServiceObj, getApplicationContext(), findViewById(android.R.id.content));
        }

        ((Button) findViewById(R.id.zeros)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.zeros)).hasFocus() &&
                        keyEvent.getKeyCode()==confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN &&
                        voiceCommand == null){
                    DataAndMethods.ttsEnabled=false;
                    DataAndMethods.displayOn=false;
                    brailleServiceObj.display(DataAndMethods.data);
                }
                return false;
            }
        });



        ((Button) findViewById(R.id.ones)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.ones)).hasFocus() &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN &&
                        (keyEvent.getKeyCode()== confirmButton ||
                        keyEvent.getKeyCode() == backButton) &&
                        voiceCommand == null){
                    try{
                        DataAndMethods.ttsEnabled=true;
                        DataAndMethods.displayOn= true;
                        if(getLocalClassName().equals("Guidance")){
                            if(keyEvent.getKeyCode()== confirmButton) {
                                DataAndMethods.presentTarget++;
                            }
                            else{
                                DataAndMethods.presentTarget--;
                            }
                            brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer));
                        }
                        else{
                            if(keyEvent.getKeyCode()==confirmButton){
                                DataAndMethods.presentLayer++;
                                if (DataAndMethods.presentLayer>=DataAndMethods.layerCount+1)
                                    DataAndMethods.presentLayer=0;
                            }
                            else{
                                DataAndMethods.presentLayer--;
                                if (DataAndMethods.presentLayer<0)
                                    DataAndMethods.presentLayer= DataAndMethods.layerCount;
                            }
                            if (getLocalClassName().equals("Exploration")) {
                                brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
                            }
                            else{
                                brailleServiceObj.display(DataAndMethods.getAnnotationBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
                            }
                        }
                    }catch(IOException e) {
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
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN &&
                        voiceCommand == null){
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


        ((Button) findViewById(R.id.mode)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.mode)).hasFocus() &&
                        (keyEvent.getKeyCode()== confirmButton ||
                                keyEvent.getKeyCode() == backButton) &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN &&
                        voiceCommand == null){
                    try {
                    if (keyEvent.getKeyCode() == confirmButton) {
                        if (getLocalClassName().equals("Guidance")) {
                            executeCommand("exploration mode");
                        }
                        else if((getLocalClassName().equals("Exploration"))){
                            executeCommand("annotation mode");
                        }
                        else{
                            executeCommand("guidance mode");
                        }
                    }
                    else{
                        if (getLocalClassName().equals("Guidance")) {
                            executeCommand("annotation mode");
                        }
                        else if((getLocalClassName().equals("Exploration"))){
                            executeCommand("guidance mode");
                        }
                        else{
                            executeCommand("exploration mode");
                        }
                    }
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
                return false;
            }
        });




        // Voice Command Recognition Stuff
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
                String cmd =data.get(0);
                voiceCommand = cmd.toLowerCase();;
                //Log.d("SPEECHREC", data.get(0));
                if (checkCommandValidity(cmd)){
                    DataAndMethods.speaker("Acquired command: "+ cmd +". Press confirm to execute.");
                }
                else{
                    DataAndMethods.speaker("Did not acquire a valid command");
                    voiceCommand = null;
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
                put(421, "UP");
                put(420, "DOWN");
                put(KEYCODE_ZOOM_OUT, "ZOOM OUT");
                put(KEYCODE_ZOOM_IN, "ZOOM IN");
                put(KEYCODE_DPAD_UP, "DPAD UP");
                put(KEYCODE_DPAD_DOWN, "DPAD DOWN");
                put(KEYCODE_DPAD_LEFT, "DPAD LEFT");
                put(KEYCODE_DPAD_RIGHT, "DPAD RIGHT");
                put(KEYCODE_MENU, "MENU");
                put(confirmButton, "OK");
                put(backButton, "CANCEL");
            }};
            if (voiceCommand!=null && keyMapping.getOrDefault(keyCode, "default")!= "OK"){
                voiceCommand = null;
                DataAndMethods.speaker("");
            }
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                // Navigating between files
                case "UP":
                case "DOWN":
                    Log.d("KEY EVENT", event.toString());
                    DataAndMethods.getFile();
                    return true;

                case "ZOOM OUT":
                    Log.d("KEY EVENT", event.toString());
                    if(!DataAndMethods.zoomingOut) {
                        DataAndMethods.speaker("Zoom mode enabled");
                        DataAndMethods.zoomingOut = true;
                        DataAndMethods.zoomingIn=false;
                    }
                    else {
                        DataAndMethods.zoomingOut=false;
                        DataAndMethods.speaker("Zoom mode disabled");
                    }
                    return true;
                case "ZOOM IN":
                    Log.d("KEY EVENT", event.toString());
                    if(!DataAndMethods.zoomingIn) {
                        DataAndMethods.speaker("Zoom mode enabled");
                        DataAndMethods.zoomingIn = true;
                        DataAndMethods.zoomingOut=false;
                    }
                    else {
                        DataAndMethods.zoomingIn=false;
                        DataAndMethods.speaker("Zoom mode disabled");
                    }
                    return true;
                case "DPAD UP":
                case "DPAD DOWN":
                case "DPAD LEFT":
                case "DPAD RIGHT":
                    if (DataAndMethods.zoomVal>100 && (DataAndMethods.zoomingIn || DataAndMethods.zoomingOut)){
                        Log.d("DPAD", String.valueOf(keyCode));
                        DataAndMethods.pan(keyCode, getLocalClassName());
                    }
                    return false;
                case "MENU":
                    DataAndMethods.pingsPlayer(R.raw.blip);
                    speechRecognizer.startListening(speechRecognizerIntent);
                    return false;
                case "OK":
                    if (voiceCommand!=null)
                        executeCommand(voiceCommand);
                    return false;

                case "CANCEL":
                    if (voiceCommand!=null) {
                        DataAndMethods.pingsPlayer(R.raw.image_error);
                        voiceCommand = null;
                    }
                    return false;
                default:
                    Log.d("KEY EVENT", event.toString());
                    return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);
        }
    }

    private boolean checkCommandValidity(String cmd){
        cmd = cmd.toLowerCase();
        if (cmd.contains("zoom") && cmd.matches(".*\\d.*")) {
            cmd=cmd.replace("zoom", "").replaceAll("\\s", "");
            if (cmd.matches("\\d+"))
                return true;
        }
        else if (cmd.contains("mode") && (cmd.contains("exploration")|| cmd.contains("guidance")|| cmd.contains("annotation"))){
            return true;
        }
        else if (cmd.equals("next")) {
            return true;
        }
        else if (cmd.equals("previous")){
            return true;
        }



        /*else if (cmd.equals("annotate")){
            return true;
        }*/

        // Layer - title -> Switches to exploration mode
        return false;
    }

    public void executeCommand(String cmd) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        Log.d("COMMAND", cmd);
        if (cmd.contains("zoom") && cmd.matches(".*\\d.*")) {
            cmd=cmd.replace("zoom", "").replaceAll("\\s", "");
            if (cmd.matches("\\d+")){
                zoomVal = Integer.parseInt(cmd);
                zoom = true;
                DataAndMethods.speaker("Select position");
            }
            return;
        }
        else if (cmd.contains("mode") && (cmd.contains("exploration")|| cmd.contains("guidance")|| cmd.contains("annotation"))){
            Intent myIntent = null;
            if (cmd.contains("exploration")){
                myIntent = new Intent(this, Exploration.class);
                //myIntent.putExtra("key", value); //Optional parameters
                DataAndMethods.speaker("Switching to Exploration mode");
            }
            else if(cmd.contains("annotation")){
                myIntent = new Intent(this, Annotation.class);
                //myIntent.putExtra("key", value); //Optional parameters
                DataAndMethods.speaker("Switching to Annotation mode");
            }
            else {
                myIntent = new Intent(this, Guidance.class);
                //myIntent.putExtra("key", value); //Optional parameters
                DataAndMethods.speaker("Switching to Guidance mode");
            }
            this.startActivity(myIntent);

        }
        else if (cmd.equals("next")) {
            DataAndMethods.ttsEnabled=true;
            DataAndMethods.displayOn= true;
            if(getLocalClassName().equals("Guidance")){
                    DataAndMethods.presentTarget++;
                brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer));
            }
            else {
                DataAndMethods.presentLayer++;
                if (DataAndMethods.presentLayer==DataAndMethods.layerCount+1)
                    DataAndMethods.presentLayer=0;
            if (getLocalClassName().equals("Exploration")) {
                brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
            }
            else{
                brailleServiceObj.display(DataAndMethods.getAnnotationBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
            }
        }}
        else if (cmd.equals("previous")){
            DataAndMethods.ttsEnabled=true;
            DataAndMethods.displayOn= true;
            if(getLocalClassName().equals("Guidance")){
                DataAndMethods.presentTarget--;
                brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer));
            }
            else{
                DataAndMethods.presentLayer--;
                if (DataAndMethods.presentLayer<0)
                    DataAndMethods.presentLayer= DataAndMethods.layerCount;
                if (getLocalClassName().equals("Exploration")) {
                    brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
                }
                else{
                    brailleServiceObj.display(DataAndMethods.getAnnotationBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
                }
            }
        }
        voiceCommand = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getActionMasked();
        if (action==MotionEvent.ACTION_UP && zoom){
            Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
            zoom = false;
            voiceCommand = null;
            try {
                //Log.d("CLASS", getLocalClassName());
                DataAndMethods.zoomTo(pins, zoomVal, getLocalClassName());
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }
        /*
            int action = event.getActionMasked();
            if (action==MotionEvent.ACTION_UP)
            {
                Integer [] pins=DataAndMethods.pinCheck(event.getX(), event.getY());
                Log.d("VOICE ZOOM", "Here");
                //try{
                    //DataAndMethods.zoomTo(pins, getLocalClassName());
                //}
                /*catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }}
        return false;
    }*/


    @Override
    protected void onResume() {
        super.onResume();
        /*
        mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        mDetector.setOnDoubleTapListener(this);
        DataAndMethods.handler = e -> {
            if(DataAndMethods.ttsEnabled){
                try{
                    //Log.d("ACTIVITY", "Running registration on Guidance");
                    // This works! Gesture control can now be used along with the handler.
                    onTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d("MOTION EVENT", String.valueOf(ex));
                }}

            return false;
        };
        brailleServiceObj.registerMotionEventHandler(DataAndMethods.handler);
         */
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }
}
