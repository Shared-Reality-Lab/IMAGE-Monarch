package ca.mcgill.a11y.image;

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_MENU;
import static android.view.KeyEvent.KEYCODE_ZOOM_IN;
import static android.view.KeyEvent.KEYCODE_ZOOM_OUT;

import static ca.mcgill.a11y.image.DataAndMethods.brailleServiceObj;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

public class BaseActivity extends AppCompatActivity {
    SpeechRecognizer speechRecognizer;
    Intent speechRecognizerIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                Log.d("SPEECHREC", data.get(0));
                if (checkCommandValidity(cmd)){
                    DataAndMethods.speaker("Acquired command: "+ cmd);
                }
                else{
                    DataAndMethods.speaker("Did not acquire a valid command");
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
            Intent myIntent = null;
            Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
                put(420, "UP");
                put(421, "DOWN");
                put(504, "NXT_LYR");
                put(503, "PRV_LYR");
                /*put(KEYCODE_ZOOM_OUT, "ZOOM OUT");
                put(KEYCODE_ZOOM_IN, "ZOOM IN");
                put(KEYCODE_DPAD_UP, "DPAD UP");
                put(KEYCODE_DPAD_DOWN, "DPAD DOWN");
                put(KEYCODE_DPAD_LEFT, "DPAD LEFT");
                put(KEYCODE_DPAD_RIGHT, "DPAD RIGHT");
                put(KEYCODE_MENU, "MENU");*/
            }};
            switch (keyMapping.getOrDefault(keyCode, "default")) {
                // Navigating between files
                case "UP":
                    Log.d("KEY EVENT", event.toString());
                    DataAndMethods.changeFile(DataAndMethods.fileSelected);
                    DataAndMethods.presentLayer = -1;
                    DataAndMethods.presentTarget = 0;
                    return true;
                case "DOWN":
                    brailleServiceObj.display(DataAndMethods.data);
                    DataAndMethods.presentTarget =0;
                    DataAndMethods.presentLayer=-1;
                    DataAndMethods.displayOn= false;
                    if (getLocalClassName().equals("Exploration")) {
                        myIntent = new Intent(this, Guidance.class);
                        DataAndMethods.speaker("Switching to Guidance mode");
                    }
                    else{
                        myIntent = new Intent(this, Exploration.class);
                        DataAndMethods.speaker("Switching to Exploration mode");
                    }
                    this.startActivity(myIntent);
                    return true;
                case "NXT_LYR":
                    DataAndMethods.ttsEnabled=true;
                    DataAndMethods.displayOn= true;
                    if (getLocalClassName().equals("Exploration")) {
                        DataAndMethods.presentLayer++;
                        if (DataAndMethods.presentLayer>=DataAndMethods.layerCount)
                            DataAndMethods.presentLayer=0;
                        brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
                    } else{
                        //Log.d("MODE", "HERE!");
                        DataAndMethods.presentTarget++;
                        //Log.d("PRESENT TARGET", String.valueOf(DataAndMethods.presentTarget));
                        brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentTarget));
                    }
                    return true;
                case "PRV_LYR":
                    DataAndMethods.ttsEnabled=true;
                    DataAndMethods.displayOn= true;
                    //Log.d("MODE", getLocalClassName());
                    if (getLocalClassName().equals("Exploration")) {
                        DataAndMethods.presentLayer--;
                        if (DataAndMethods.presentLayer<0)
                            DataAndMethods.presentLayer= DataAndMethods.layerCount-1;
                        brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));
                    } else{
                        DataAndMethods.presentTarget--;
                        //Log.d("PRESENT TARGET", String.valueOf(DataAndMethods.presentTarget));
                        brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentTarget));
                    }
                    return true;
                /*// Navigating between files
                
                    Log.d("KEY EVENT", event.toString());
                    DataAndMethods.changeFile(--DataAndMethods.fileSelected);
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
                */
                default:
                    Log.d("KEY EVENT", event.toString());
                    return false;
            }
        } catch (IOException | JSONException e) {
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

    private void executeCommand(String cmd){   }



}
