/*
 * Copyright (c) 2023 IMAGE Project, Shared Reality Lab, McGill University
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

package ca.mcgill.a11y.image;

import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_MENU;
import static android.view.KeyEvent.KEYCODE_ZOOM_IN;
import static android.view.KeyEvent.KEYCODE_ZOOM_OUT;

import static ca.mcgill.a11y.image.selectors.ClassroomSelector.channelSubscribed;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.scand.svg.SVGHelper;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import ca.mcgill.a11y.image.request_formats.MakeRequest;
import ca.mcgill.a11y.image.request_formats.MapRequestFormat;
import ca.mcgill.a11y.image.request_formats.PhotoRequestFormat;
import ca.mcgill.a11y.image.request_formats.ResponseFormat;
import ca.mcgill.a11y.image.selectors.ClassroomSelector;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DataAndMethods {
    // SVG data received from response 
    public static String image = null;
    // used to refresh pins to down state
    static byte[][] data = null;
    // short and long  descriptions of objects in current layer
    public static ArrayList<String[][]> tags;
    // default zoom value when new graphic is rendered in percentage
    static Integer zoomVal = 100;
    // current dimensions of graphic; dims = {start-x, start-y, end-x, end-y}
    static Float[] dims = new Float[]{0f, 0f, 0f, 0f};
    // original dimensions of graphic before viewBox manipulations; The height and width are, however, adjusted to fit the 96x40
    static Float[] origDims = new Float[]{0f, 0f, 0f, 0f};
    // original graphic dimension before aspect ratio manipulations as this is needed for the follow up query
    static Float[] origRatioDims = new Float[]{0f, 0f};
    // index of file selected in current directory
    public static int fileSelected = 0;
    // present layer generally ranges between [0, layer count]; present target indicates current target in guidance and
    // targetCount is the number of targets in guidance mode
    static Integer presentLayer = -1, presentTarget = 0, targetCount;
    // sets whether the TTS label is assigned to the area enclosed by a shape
    static boolean labelFill = true;
    // enables/disables TTS read out
    public static boolean ttsEnabled = true;
    // set zooming in/out as enabled or disabled
    public static boolean zoomingIn = false;
    public static boolean zoomingOut = false;

    // showing/hiding non-target elements in guidance mode
    public static boolean showAll = false;
    public static BrailleDisplay brailleServiceObj = null;
    public static BrailleDisplay.MotionEventHandler handler;
    // keep track of current request to server
    private static Call<ResponseFormat> ongoingCall;
    // TTS engine instance
    static TextToSpeech tts = null;
    // application context
    static Context context;
    // application view
    static View view;
    // string used to set viewBox
    static String zoomBox = "";
    // keyCode of confirm button; braille dot 8 is used as Enter in current standard
    public static int confirmButton = 504;
    // keyCode of back button; braille dot 7 is used as backspace in current standard
    public static int backButton = 503;
    // cache storage size
    static final int DISK_CACHE_SIZE = 10 * 1024 * 1024;
    //tracker to check whether new data has been received after server call
    public static MutableLiveData<Boolean> update = new MutableLiveData<>();
    // mapping of keyCodes
    public static Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
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
        put(KEYCODE_BACK, "BACK");
    }};

    // speech recognizer stuff
    public static SpeechRecognizer speechRecognizer = null;
    public static Intent speechRecognizerIntent;

    // initializes the Braille display, TTS and other common components in newly created activity
    public static void initialize(BrailleDisplay brailleServiceObj, Context context, View view) {
        DataAndMethods.brailleServiceObj = brailleServiceObj;
        DataAndMethods.context = context;
        DataAndMethods.view = view;

        // sets array with dimensions of pin array to 0s; used to refresh the pins when required
        data = new byte[brailleServiceObj.getDotLineCount()][];
        for (int i = 0; i < data.length; ++i) {
            data[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(data[i], (byte) 0x00);
        }

        // empty string array to be populated with descriptions when the layer is loaded
        tags = new ArrayList<>();
        tags.add(new String[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);
        tags.add(new String[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);

        // only initialize tts if it is not already set up; otherwise this takes too long
        if (tts == null) {
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {

                @Override
                public void onInit(int status) {

                    if (status != TextToSpeech.SUCCESS) {
                        Log.e("error", "Initialization Failed!" + status);
                    } else {
                        tts.setLanguage(Locale.getDefault());
                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String s) {

                            }

                            @Override
                            public void onDone(String s) {
                                //Log.d("CHECKING!", s);
                                // plays ping when TTS readout is completed based on utteranceId
                                if (s.equals("ping")) {
                                    pingsPlayer(R.raw.blip);
                                }
                            }

                            @Override
                            public void onError(String s) {

                            }
                        });
                    }
                }
            });
        }
        if (speechRecognizer == null) {
            // Voice Command Recognition Stuff
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(DataAndMethods.context);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());


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
                    switch (i) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            DataAndMethods.speaker("Failed to recognize text");
                        default:
                            DataAndMethods.speaker("Error occurred during speech recognition");
                    }
                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String results = data.get(0);
                    onVoiceRecognitionResults(results);
                    //speaker("Query acquired: "+cmd);
                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            });
        }
    }


    // handle voice recognition results()
    public static void onVoiceRecognitionResults(String results){
        if (DataAndMethods.speechRecognizerIntent.getStringExtra("Activity").equals("renderers.BasicPhotoMapRenderer")){
            // Log.d("VOICE_REC", results);
            Intent myIntent = new Intent(DataAndMethods.context, FollowUpQuery.class);
            myIntent.putExtra("query", results);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            DataAndMethods.context.startActivity(myIntent);

        }else
            Log.d("onVoiceRecognitionResults", "NOT HANDLED YET!");
    }
    //check mode and display appropriate layer
    public static void displayGraphic(int keyCode, String mode){
        try{if (mode=="Exploration"){

                DataAndMethods.ttsEnabled=true;
                if(keyCode ==confirmButton){
                    ++ DataAndMethods.presentLayer;
                }
                else{
                    -- DataAndMethods.presentLayer;
                }
                brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));



        } else if (mode=="Guidance"){
            DataAndMethods.ttsEnabled=true;
            if(keyCode ==confirmButton){
                ++ DataAndMethods.presentTarget;
            }
            else{
                -- DataAndMethods.presentTarget;
            }
            brailleServiceObj.display(DataAndMethods.getGuidanceBitmaps(DataAndMethods.getfreshDoc(), true));
        }}
        catch(IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    // function to pad the bitmap to match the pin array aspect ratio
    public static Bitmap padBitmap(Bitmap bitmap, int padX, int padY)
    {
        Bitmap paddedBitmap = Bitmap.createBitmap(
                bitmap.getWidth() + padX,
                bitmap.getHeight() + padY,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawARGB(0x00, 0xFF, 0xFF, 0xFF);
        canvas.drawBitmap(
                bitmap,
                padX / 2,
                padY / 2,
                new Paint(Paint.FILTER_BITMAP_FLAG));

        return paddedBitmap;
    }

    // convert the modified doc to a string
    public static String getStringFromDocument(Document doc)
    {
        try
        {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch(TransformerException ex)
        {
            Log.d("ERROR", "Write Failed");
            return null;
        }
    }

    // get present layer and the description tags from the doc
    public static byte[][] getBitmaps(Document doc, int presentLayer, boolean readCaption) throws IOException, XPathExpressionException {
        //Log.d("LAYER!", String.valueOf(presentLayer));
        XPath xPath = XPathFactory.newInstance().newXPath();
        // get list of layers; Uses default ordering which is expected to be 'document order' but the return type is node-set which is unordered!
        NodeList nodeslist = (NodeList)xPath.evaluate("//*[self::*[@data-image-layer] and not(ancestor::metadata)]", doc, XPathConstants.NODESET);
        //Log.d("XPATH", String.valueOf(nodeslist.getLength()));
        Integer layerCount=nodeslist.getLength();
        //Log.d("PRESENT LAYER", DataAndMethods.presentLayer+","+layerCount);
        if (DataAndMethods.presentLayer>= layerCount+1)
            DataAndMethods.presentLayer= 0;
        else if (DataAndMethods.presentLayer<0)
            DataAndMethods.presentLayer= layerCount;
        //Log.d("PRESENT LAYER", DataAndMethods.presentLayer+","+layerCount);
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            // hide layers which are not the present layer
            if (i!= DataAndMethods.presentLayer && DataAndMethods.presentLayer!=layerCount) {
                ((Element)node).setAttribute("display","none");
            }
            // TTS output of layer description
            if (i==DataAndMethods.presentLayer){
                //Log.d("GETTING TAGS", String.valueOf(nodeslist.getLength()));
                String tag;
                //Log.d("GETTING TAGS", node.getNodeName());
                if (((Element)node).hasAttribute("aria-labelledby")) {
                    tag= doc.getElementById(((Element) node).getAttribute("aria-labelledby")).getTextContent();
                    //Log.d("GETTING TAGS", (doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent()));
                }
                else{
                    tag=((Element)node).getAttribute("aria-label");
                    //Log.d("GETTING TAGS", "Otherwise here!");
                }
                if (readCaption) {
                    speaker("Layer: " + tag);
                }
            }
        }
        //If there is no tag as a layer, hide elements unless the full image is to be shown
        if (DataAndMethods.presentLayer!=layerCount){

            nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@data-image-layer]) and not(descendant::*[@data-image-layer])] ", doc, XPathConstants.NODESET);
            for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
                Node node = nodeslist.item(i);
                ((Element)node).setAttribute("display","none");
            }
        }
        else if (readCaption){
            speaker("Full image");
        }

        NodeList detail= (NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and (self::*[@data-image-zoom])]", doc, XPathConstants.NODESET);
        for(int i = 0 ; i < detail.getLength() ; i ++) {
            Node node = detail.item(i);
            Float zoomLevel= Float.valueOf(((Element)node).getAttribute("data-image-zoom"));
            if (zoomVal<zoomLevel)
                ((Element)node).setAttribute("display","none");
        }
        // get bitmap of present layer
        byte[] byteArray= docToBitmap(doc);
        //Log.d("BITMAP", Arrays.toString(byteArray));

        // the TTS tags are fetched in a separate thread.
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    getDescriptions(doc);
                    //Log.d("DESCRIPTIONS", "Description loaded");
                    // This ping plays when the descriptions (i.e. TTS labels) are loaded.
                    // Generally occurs with a little delay following the tactile rendering
                    pingsPlayer(R.raw.ping);
                } catch (XPathExpressionException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // reshape byte array into 2D array to match pin array dimensions
        byte[][] dataRead = new byte[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            dataRead[i]= Arrays.copyOfRange(byteArray, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }
        return dataRead;
    }

    // get present target and description tags from the doc
    public static byte[][] getGuidanceBitmaps(Document doc, Boolean readCaption) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        String tag = "";
        XPath xPath = XPathFactory.newInstance().newXPath();
        if (targetCount ==0){
            speaker("No guidance mode available");
            return data;
        }
        else if (presentTarget<=0 ){
            presentTarget = targetCount;
        }
        else if (presentTarget > targetCount){
            presentTarget = 1;
        }

        NodeList nodeslist = (NodeList) xPath.evaluate("//*[not(descendant-or-self::*[@data-image-target = '"+presentTarget+"'])]", doc, XPathConstants.NODESET);

        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display","none");
        }

        nodeslist = (NodeList) xPath.evaluate("//*[not(ancestor-or-self::*[@display = 'none'] and ancestor::metadata)]", doc, XPathConstants.NODESET);
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i).cloneNode(true);
            doc.getElementsByTagName("svg").item(0).appendChild(node);
        }

        //speaker(tag);
        byte[] mask = docToBitmap(doc);
        doc = getTargetLayer(getfreshDoc());
        Node targetLayer = ((NodeList) xPath.evaluate("//*[(descendant-or-self::*[@data-image-target = '"+presentTarget+"']) and self::*[@data-image-layer]]", doc, XPathConstants.NODESET)).item(0);
        if (((Element)targetLayer).hasAttribute("aria-labelledby")) {
            tag= doc.getElementById(((Element) targetLayer).getAttribute("aria-labelledby")).getTextContent();
            //Log.d("GETTING TAGS", (doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent()));
        }
        else{
            tag=((Element)targetLayer).getAttribute("aria-label");
            //Log.d("GETTING TAGS", "Otherwise here!");
        }
        Node node = ((NodeList) xPath.evaluate("//*[ancestor-or-self::*[@data-image-target = '"+presentTarget+"']]", doc, XPathConstants.NODESET)).item(0);
        if (((Element)node).hasAttribute("aria-labelledby")) {
            tag += "\n" + doc.getElementById(((Element) node).getAttribute("aria-labelledby")).getTextContent();
            //Log.d("GETTING TAGS", (doc.getElementById(((Element) node).getAttribute("aria-labelledby")).getTextContent()));
        }
        else{
            tag += "\n" + ((Element)node).getAttribute("aria-label");
            //Log.d("GETTING TAGS",((Element)node).getAttribute("aria-label"));
        }
        if (readCaption){
        speaker("Layer: " + tag);}

        byte[] target = docToBitmap(doc);

        byte[] byteArray = new byte[mask.length];
        for (int i = 0; i < mask.length; i++) {
            byteArray[i] = (byte) (mask[i] * target[i]);
        }
        byte[][] dataRead = new byte[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            dataRead[i]= Arrays.copyOfRange(byteArray, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }

        return dataRead;
    }

    // get basic and detailed descriptions
    public static void getDescriptions(Document doc) throws XPathExpressionException, IOException {
        //Log.d("GETTING TAGS", "Here!");
        XPath xPath = XPathFactory.newInstance().newXPath();
        // query elements that are in the present layer AND have element level descriptions (NOT layer level descriptions)
        // Assuming that only elements with short description can have a long description here. Is this assumption safe?!
        NodeList nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and (not(self::*[@data-image-layer]) or not(child::*))  and ((self::*[@aria-labelledby] or self::*[@aria-label]) or parent::*[@data-image-layer])]", doc, XPathConstants.NODESET);        // temporary var for objects tags
        String[] layerTags=new String[brailleServiceObj.getDotPerLineCount()*brailleServiceObj.getDotLineCount()];
        // temporary var for objects long descriptions
        String[] layerDesc=new String[brailleServiceObj.getDotPerLineCount()*brailleServiceObj.getDotLineCount()];
        //Log.d("GETTING TAGS", String.valueOf(nodeslist.getLength()));
        // initially hiding all elements filtered in the previous stage
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display", "none");
        }
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            String tag, detailTag = null;
            Node node = nodeslist.item(i);
            // fetching the tag for each element
            if (!((Element)node).hasAttribute("aria-label") && !((Element)node).hasAttribute("aria-labelledby")){
                continue;
            }
            if (((Element)node).hasAttribute("aria-labelledby")) {
                tag= doc.getElementById(((Element) node).getAttribute("aria-labelledby")).getTextContent();
                //Log.d("TAG", tag);
            }
            else{
                tag=((Element)node).getAttribute("aria-label");
                //Log.d("TAG", tag);
            }
            if (((Element)node).hasAttribute("aria-describedby")) {
                detailTag= doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent();
            }
            else{
                // this returns an empty string even if the attribute doesn't exist i.e. if there is no long description
                detailTag=((Element)node).getAttribute("aria-description");
            }

            if (labelFill) {
                ((Element) node).setAttribute("fill", "black");
            }

            // showing the element whose tag is stored to obtain its bitmap mapping
            ((Element)node).removeAttribute("display");
            byte[] byteArray= docToBitmap(doc);
            // using a 'for' loop to map since there are now 2 kinds of tags: label and detailed. Could possibly find a prettier way to do this Java objects
            for (int j=0; j<layerTags.length; j++){
                if (byteArray[j]!=0){
                    if (layerTags[j]==null){
                        layerTags[j]=tag;
                    }
                    else {
                        layerTags[j]= layerTags[j] + ", " + tag;
                    }
                    //check if detailTag is not blank string.
                    if (!detailTag.equalsIgnoreCase("")){
                        if (layerDesc[j]==null){
                            layerDesc[j]=detailTag;
                        }
                        else {
                            layerDesc[j]= layerDesc[j] + ", " + detailTag;
                        }
                    }
                }
            }
            // hiding element again so we can move on to the next element
            ((Element)node).setAttribute("display", "none");
        }

        // converting string array into 2D array that maps to the pins
        for (int i = 0; i < data.length; ++i) {
            tags.get(0)[i]=Arrays.copyOfRange(layerTags, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
            tags.get(1)[i]=Arrays.copyOfRange(layerDesc, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }
        return;
    }
    // converts the xml doc to bitmap
    public static byte[] docToBitmap(Document doc) throws IOException {

        String img= getStringFromDocument(doc).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ", "");
        //Log.d("SVG",img);
        //Log.d("DIMS", dims[0]+","+ dims[1]+","+dims[2]+","+dims[3]);
        Bitmap svg = SVGHelper.noContext().open(img).setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
        int x = svg.getWidth();
        int y = svg.getHeight();
        //Log.d("SVG",x+", "+ y);
        // padding bitmap to fit to pin array size
        Bitmap svgScaled=padBitmap(svg, (brailleServiceObj.getDotPerLineCount()-x>0)?(brailleServiceObj.getDotPerLineCount()-x):0,
                (brailleServiceObj.getDotLineCount()-y)>0?(brailleServiceObj.getDotLineCount()-y):0);
        svg.recycle();
        // extracting only the alpha value of bitmap to convert it into a byte array
        Bitmap alphas=svgScaled.extractAlpha();
        int size = alphas.getRowBytes() * alphas.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        alphas.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        return byteArray;
    }

    // fetching the file to read from; makes server request and also returns the file name
    public static String getFile(int fileNumber) throws IOException, JSONException {
        String folderName= "/sdcard/IMAGE/client/";
        File directory = new File(folderName);
        File[] files = directory.listFiles();

        int filecount=files.length;
        if (fileNumber>= filecount)
            fileSelected=0;
        else if (fileNumber<0)
            fileSelected=filecount-1;

        Bitmap bitmap = BitmapFactory.decodeFile(folderName+files[fileSelected].getName());
        byte[] imageBytes = Files.readAllBytes(Paths.get(folderName + files[fileSelected].getName()));

        String base64 = "data:"+ getMimeType(files[fileSelected].getName())+";base64,"+ Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        Integer[] dims= new Integer[] {bitmap.getWidth(), bitmap.getHeight()};
        PhotoRequestFormat req= new PhotoRequestFormat();
        req.setValues(base64, dims);
        Retrofit retrofit = requestBuilder(60, 60, "https://image.a11y.mcgill.ca/");

        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.makePhotoRequest(req);
        makeServerCall(call);
        // The regex expression in replaceFirst removes everything following the '.' i.e. .jpg, .png etc.
        return files[fileSelected].getName().replaceFirst("\\.[^.]*$", "");
    }

    // initializes and returns the retrofit request;
    public static Retrofit requestBuilder(long readTimeout, long connectTimeout, String baseUrl){
        // Uncomment the following lines for logging http requests
        //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        //logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                //Need next 2 lines when server response is slow
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .cache(getCache());

        //httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        return retrofit;
    }

    // get file mime type
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    // makes server request for a tactile map rendering based on provided latitude longitude coordinates
    public static void getMap(Double lat, Double lon) throws JSONException {
        MapRequestFormat req= new MapRequestFormat();
        req.setValues(lat, lon);
        Retrofit retrofit = requestBuilder(60, 60, // "https://unicorn.cim.mcgill.ca/image/" );
                "https://image.a11y.mcgill.ca/");
        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.makeMapRequest(req);
        makeServerCall(call);
    }

    // fetches updates from server if they exist
    public static void checkForUpdate() throws IOException, JSONException {
        Retrofit retrofit = requestBuilder(60, 60, context.getString(R.string.server_url));

        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.checkForUpdates(context.getString(R.string.server_url)+"display/"+channelSubscribed);
        makeServerCall(call);
    }

    // initializes dims, zoomBox and origDims for new graphic
    public static void setImageDims() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        if (!node.hasAttribute("viewBox")) {
            Float width = Float.valueOf(node.getAttribute("width"));
            Float height = Float.valueOf(node.getAttribute("height"));
            dims[2] = width;
            dims[3] = height;
            zoomBox = dims[0] + " " + dims[1] + " " + width + " " + height;
            origDims = new Float[]{0f, 0f, width, height};
        }
        else {
            dims = Arrays.copyOf(origDims, origDims.length);
            dims[2] += dims[0];
            dims[3] += dims[1];
        }
    }

    // get number of targets if guidance exists
    public static void targetCounts() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeslist = (NodeList)xPath.evaluate("//*[@data-image-target]", doc, XPathConstants.NODESET);
        targetCount=0;
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            int count = Integer.parseInt(((Element)node).getAttribute("data-image-target"));
            if (count > targetCount)
                targetCount = count;
        }
    }

    // get layer in which the current target is
    public static Document getTargetLayer(Document doc) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node targetLayer = ((NodeList) xPath.evaluate("//*[(descendant-or-self::*[@data-image-target = '"+presentTarget+"']) and self::*[@data-image-layer]]", doc, XPathConstants.NODESET)).item(0);
        //Log.d("TARGET Layer", String.valueOf(((Element) targetLayer).getAttribute("data-image-layer")));

        String layerName = ((Element) targetLayer).getAttribute("data-image-layer");
        NodeList nodeslist = (NodeList) xPath.evaluate("//*[not(descendant-or-self::*[@data-image-layer = '"+layerName+"']) and not(ancestor::*[@data-image-layer = '"+layerName+"'])]", doc, XPathConstants.NODESET);
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display","none");
        }
        nodeslist= (NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and (self::*[@data-image-zoom])]", doc, XPathConstants.NODESET);
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            Float zoomLevel= Float.valueOf(((Element)node).getAttribute("data-image-zoom"));
            if (zoomVal<zoomLevel)
                ((Element)node).setAttribute("display","none");
        }
        return doc;
    }

    // returns cache from local
    public static Cache getCache() {
        File cacheDir = new File(context.getCacheDir(), "cache");
        Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
        return cache;
    }

    // makes async call to serevr
    public static void makeServerCall(Call<ResponseFormat> call){
        // Cancelling any ongoing requests that haven't been completed
        if (ongoingCall!=null){
            ongoingCall.cancel();
        }
        update.setValue(false);
        call.enqueue(new Callback<ResponseFormat>() {
            @Override
            public void onResponse(Call<ResponseFormat> call, Response<ResponseFormat> response) {
                try {
                    if (response.raw().networkResponse().code() != HttpURLConnection.HTTP_NOT_MODIFIED || image == null) {
                        ResponseFormat resource = response.body();
                        ResponseFormat.Rendering[] renderings = resource.renderings;
                        image =(renderings[0].data.graphic).replaceFirst("data:.+,", "");
                        //Log.d("RESPONSE", image);
                        byte[] data = image.getBytes("UTF-8");
                        data = Base64.decode(data, Base64.DEFAULT);
                        image = new String(data, "UTF-8");
                        // Log.d("IMAGE", image);
                        // gets viewBox dims for current image
                        resetGraphicParams();
                        setImageDims();
                        targetCounts();
                        if (renderings[0].data.layer != null) {
                            setDefaultLayer(renderings[0].data.layer);
                        }
                        else{
                            presentLayer = -1;
                        }
                        pingsPlayer(R.raw.image_results_arrived);
                        update.setValue(true);
                    }
                    else{
                        Log.d("CACHE", "Fetching from cache!");
                    }
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                // This occurs when there is no rendering returned
                catch (ArrayIndexOutOfBoundsException| NullPointerException e){
                    pingsPlayer(R.raw.image_error);
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                }
            }

            //onFailure is called both when a request is cancelled (i.e. interrupted with another request)
            // AND when it fails to give a valid response
            @Override
            public void onFailure(Call<ResponseFormat> call, Throwable t) {
                // Ensure that a request was cancelled before playing error ping
                // This text is not read out when a request is cancelled as there is expected to be
                // an ongoing request and can be confused as a result of that request.
                // Causes interrupted requests to die silently!
                Log.d("RESPONSE", "FAILED!");
                if (!call.isCanceled()){
                    pingsPlayer(R.raw.image_error);
                }
            }
        });
        // Saving the in-progress call to allow interruption if needed
        ongoingCall=call;
    }

    // resets zoom and dimension related variables for new graphics
    public static void resetGraphicParams(){
        zoomVal=100;
        dims=new Float[]{0f,0f, 0f, 0f};
        origDims=new Float[]{0f,0f, 0f, 0f};
        origRatioDims = new Float[] {0f, 0f};
        zoomingIn=false;
        zoomingOut=false;
        zoomBox = "";
        targetCount = 0;
        presentTarget = 0;
    }

    // sets requested layer for new graphic; this is only called when 'layer' field exists in server resposne
    public static void setDefaultLayer(String layerInput) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        //Log.d("LAYER INPUT", layerInput);
        if (!layerInput.equals("None")){
            XPath xPath = XPathFactory.newInstance().newXPath();
            Document doc = DataAndMethods.getfreshDoc();
            Node defaultLayer = ((NodeList) xPath.evaluate("//*[self::*[@data-image-layer = '"+layerInput+"']]", doc, XPathConstants.NODESET)).item(0);
            NodeList layers = ((NodeList) xPath.evaluate("//*[self::*[@data-image-layer]]", doc, XPathConstants.NODESET));
            for(int i = 0 ; i < layers.getLength() ; i ++) {
                Node node = layers.item(i);
                if (node.isSameNode(defaultLayer)) {
                    DataAndMethods.presentLayer = (i - 1);
                    break;
                }
            }
        }
        // Making sure the layer doesn't go to -2 on first run
        else if(DataAndMethods.presentLayer>=0){
            DataAndMethods.presentLayer--;
        }
    }

    // get fresh copy of the file void of previously made changes
    public static Document getfreshDoc() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(image));
        //Log.d("STRING", image);
        Document doc = builder.parse(is);

        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);

        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));
        //Log.d("NEWDOC", width+", "+height);
        float x=0 ,y=0;
        float[] translations = new float[]{0 , 0};
        if (width/height > (float) brailleServiceObj.getDotPerLineCount()/ (float) brailleServiceObj.getDotLineCount()) {
            //padding along height
            x = width;
            y = width * brailleServiceObj.getDotLineCount()/brailleServiceObj.getDotPerLineCount();
            translations[1] = (y - height)/2;
        }
        else {
            //padding along width
            y = height;
            x= height * brailleServiceObj.getDotPerLineCount()/brailleServiceObj.getDotLineCount();
            translations[0] = (x- width)/2;
        }
        //Log.d("DIMS", width+ ", "+ height+ ";"+ x+ ", "+ y);
        if((width/height- (float)brailleServiceObj.getDotPerLineCount()/ (float)brailleServiceObj.getDotLineCount())<0.01){
            node.setAttribute("width", String.valueOf(x));
            node.setAttribute("height", String.valueOf(y));
            NodeList nodeslist = (NodeList)xPath.evaluate("//*[self::*[@data-image-layer] and not(ancestor::metadata)]", doc, XPathConstants.NODESET);
            for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
                Node n = nodeslist.item(i);
                ((Element)n).setAttribute("transform", "translate("+translations[0]+" "+translations[1]+")");
            }
            nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@data-image-layer]) and not(descendant::*[@data-image-layer])and not(ancestor::metadata)] ", doc, XPathConstants.NODESET);
            for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
                Node n = nodeslist.item(i);
                ((Element)n).setAttribute("transform", "translate("+translations[0]+" "+translations[1]+")");
            }
            nodeslist = (NodeList) xPath.evaluate("//*[ancestor::metadata]", doc, XPathConstants.NODESET);
            for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
                Node n = nodeslist.item(i);
                ((Element)n).setAttribute("transform", "translate("+translations[0]+" "+translations[1]+")");
            }
        }

            //node.setAttribute("transform", "translate("+translations[0]+" "+translations[1]+")");
        //Log.d("TRANS", "translate("+translations[0]+" "+translations[1]+")");
        // check if it is the initial run
        if (origDims[2]==0f){
            // check if the viewBox attribute exists
            if (node.hasAttribute("viewBox")){
                zoomBox = node.getAttribute("viewBox");
                //Log.d("VIEW_old", zoomBox);
                origDims =  Arrays.stream(zoomBox.split(" ", -1)).map(Float::valueOf).toArray(Float[]::new);
                origRatioDims = Arrays.copyOfRange(origDims, 2, 4);
                // perform manipulations to zoomBox
                origDims[2] = x;
                origDims[3] = y;
                String zoomDims= Arrays.toString(origDims).replaceAll(",", "");
                //Log.d("ZOOM",Arrays.toString(press));
                zoomBox = zoomDims.substring(1,zoomDims.length() - 1);
                //Log.d("VIEW_new", zoomBox);
                node.setAttribute("viewBox", zoomBox );
            }}
            else{
                node.setAttribute("viewBox", zoomBox );
            }
        //Log.d("VIEW", node.getAttribute("viewBox"));
        return doc;
    }

    // find which pin the finger position corresponds to
    public static Integer[] pinCheck(float x, float y){
        float xMin= 0;
        float xMax= 1920;
        float yMin=23;
        float yMax=1080;
        double epsilon= 0.000001;
        // Calculating pin based on position
        int pinX= (int) (Math.ceil((x-xMin+epsilon)/((xMax-xMin)/brailleServiceObj.getDotPerLineCount()))-1);
        int pinY= (int) Math.ceil((y-yMin+epsilon)/((yMax-yMin)/brailleServiceObj.getDotLineCount()))-1;
        return new Integer[] {pinX, pinY};
    }

    // handles zooming functions i.e. zooming in/out and displays new graphic after manipulations
    public static void zoom(Integer[] pins, String mode) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        //Float width= Float.valueOf(node.getAttribute("width"));
        //Float height= Float.valueOf(node.getAttribute("height"));
        Float width = origDims[2];
        Float height = origDims[3];

        if (zoomingIn){
            zoomVal+= 25;
            node.setAttribute("viewBox", zoomer(width, height, zoomVal, pins));
            if (mode.equals("Exploration"))
                brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
            else if (mode.equals("Guidance"))
                brailleServiceObj.display(getGuidanceBitmaps(doc, false));
        }
        else{
            if (zoomVal <= 100){
                speaker("Oops! Cannot zoom out further");
            }
            else {
                zoomVal-= 25;
                if (zoomVal < 100) zoomVal = 100;
                node.setAttribute("viewBox", zoomer(width, height, zoomVal, pins));
                if (mode.equals("Exploration"))
                    brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
                else if (mode.equals("Guidance")) {
                    if (showAll){
                        brailleServiceObj.display(DataAndMethods.displayTargetLayer(doc));
                    }
                    else {
                        brailleServiceObj.display(getGuidanceBitmaps(doc, false));
                    }
                }
            }
        }
        return;
    }

    // computes new graphic dimensions during zoom
    public static String zoomer(float width, float height, int zoomVal, Integer[] pins){
        // image dimensions where the press occured
        float[] press=new float[]{0, 0};
        float sWidth=dims[0], sHeight=dims[1], fWidth=dims[2], fHeight=dims[3];

        //Log.d("RECEIVED", width+", "+height);
        //Log.d("DIMS", dims[0] +", "+dims[1]+", "+dims[2]+", "+dims[3] );
        float scalingFactor, widthNew= fWidth - sWidth, heightNew= fHeight- sHeight;
        //int bufferPins;


        scalingFactor= brailleServiceObj.getDotPerLineCount()/widthNew;
        //Log.d("SCALE", String.valueOf(scalingFactor));
        press[0] = (pins[0]/scalingFactor) + dims[0];
        scalingFactor = brailleServiceObj.getDotLineCount()/heightNew;
        press[1] = (pins[1]/scalingFactor) + dims[1];

        //Log.d("PINS", pins[0]+", "+pins[1]);
        //Log.d("PRESS", press[0]+","+press[1]);
        float zoomWidth= width/((float)zoomVal/100);
        float zoomHeight= height/((float)zoomVal/100);

        //Log.d("SCALE", zoomWidth+","+zoomHeight);
        // zoom while keeping the portion of the image at the point of press at the same pins post zoom
        scalingFactor = zoomWidth/brailleServiceObj.getDotPerLineCount();
        dims[0] = press[0] - (scalingFactor * (pins[0]));
        dims[2] = dims[0] + zoomWidth;
        scalingFactor = zoomHeight / brailleServiceObj.getDotLineCount();
        dims [1] = press[1] - (scalingFactor * (pins[1]));
        dims[3] = dims[1] + zoomHeight;
        //Log.d("NEW DIMS", dims[0]+","+dims[1]+","+dims[2]+","+dims[3]);
        // ensure that the newly calculted dimensions are within the limits of the origincal graphic
        if (dims[0]<origDims[0]){ 
            dims[0] = origDims[0];
            dims[2] = dims[0] + zoomWidth;
        }
        else if (dims[2]>width){
            dims[2] = width;
            dims[0] = dims[2] -zoomWidth;
        }
        if (dims[1]<origDims[1]){
            dims[1] =origDims[1];
            dims[3] = dims[1] + zoomHeight;
        }
        else if (dims[3]>height){
            dims[3] = height;
            dims[1] = dims[3] - zoomHeight;
        }
        Float[] zooming= new Float[]{dims[0], dims[1], zoomWidth, zoomHeight};
        //Float[] zooming= new Float[]{0.0f, 0.0f, zoomWidth, zoomHeight};
        String zoomDims= Arrays.toString(zooming).replaceAll(",", "");
        //Log.d("ZOOM",Arrays.toString(press));
        zoomBox = zoomDims.substring(1,zoomDims.length() - 1);
        //Log.d("DIMS", dims[0] +", "+dims[1]+", "+dims[2]+", "+dims[3] );
        //Log.d("ZOOM",zoomBox);
        return zoomBox;
    }

    // computes graphic dimensions and displays new graphic after pan operation
    public static void pan(int keyCode, String className) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        //Float width= Float.valueOf(node.getAttribute("width"));
        //Float height= Float.valueOf(node.getAttribute("height"));
        Float width = origDims[2];
        Float height = origDims[3];
        // pan by 10%
        Float widthShift= (dims[2]-dims[0])/10;
        Float heightShift=(dims[3]-dims[1])/10;
        // check pan direction
        switch(keyCode) {
            case KEYCODE_DPAD_UP:
                // UP
                dims[1] -= heightShift;
                dims[3] -= heightShift;
                break;
            case KEYCODE_DPAD_DOWN:
                // DOWN
                dims[1] += heightShift;
                dims[3] += heightShift;
                break;
            case KEYCODE_DPAD_LEFT:
                // LEFT
                dims[0] -= widthShift;
                dims[2] -= widthShift;
                break;
            case KEYCODE_DPAD_RIGHT:
                // RIGHT
                dims[0] += widthShift;
                dims[2] += widthShift;
                break;
        }
        //Log.d("DIMS", dims[0]+","+ dims[1]+","+dims[2]+","+dims[3]);
        // ensure that panning stays within graphic bounds
        if (dims[0]<origDims[0]){
            dims[2]-=(dims[0]-origDims[0]);
            dims[0]=origDims[0];
        }
        else if (dims[2] >(width+origDims[0])){
            dims[0]-= (dims[2]-(width+origDims[0]));
            dims[2]=(width+origDims[0]);
        }
        if (dims[1]<origDims[1]){
            dims[3]-=(dims[1]-origDims[1]);
            dims[1]=origDims[1];
        }
        else if (dims[3]>(height+origDims[1])){
            dims[1]-=(dims[3]-(height+origDims[1]));
            dims[3]=(height+origDims[1]);
        }
        //Log.d("DIMS", dims[0]+","+ dims[1]+","+dims[2]+","+dims[3]);
        Float[] panning= new Float[]{dims[0], dims[1], dims[2]-dims[0], dims[3]-dims[1]};
        String panned= Arrays.toString(panning).replaceAll(",", "");
        //Log.d("VIEWBOX", node.getAttribute("viewBox"));
        zoomBox = panned.substring(1,panned.length() - 1);
        node.setAttribute("viewBox", zoomBox);
        //Log.d("PAN", zoomBox);
        if (className.equals("renderers.Exploration") || className.equals("renderers.BasicPhotoMapRenderer"))
            brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
        if (className.equals("renderers.Guidance")){
            if (showAll){
                brailleServiceObj.display(DataAndMethods.displayTargetLayer(doc));
            }
            else {
                brailleServiceObj.display(getGuidanceBitmaps(doc, false));
            }
        }
    }

    // TTS speaker. Probably needs a little more work on flushing and/or selecting whether to continue playing
    public static void speaker(String text, String... utterId){
        tts.speak (text, TextToSpeech.QUEUE_FLUSH, null, utterId.length > 0 ? utterId[0]  : "000000");
        return;
    }

    // plays sound files; used for notification sounds 
    public static void pingsPlayer(int file){
        //set up MediaPlayer
        MediaPlayer mp = new MediaPlayer();

        try {
            mp=MediaPlayer.create(context, file);
            mp.start();
        } catch (Exception e) {
            Log.d("ERROR", e.toString());
        }
    }

    // display layer that the current target is in
    public static byte[][] displayTargetLayer(Document doc) throws XPathExpressionException, IOException {
        doc = getTargetLayer(doc);
        byte[] byteArray = docToBitmap(doc);
        final Handler handler = new Handler(Looper.getMainLooper());
        Document finalDoc = doc;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    getDescriptions(finalDoc);
                    //Log.d("DESCRIPTIONS", "Description loaded");
                    // This ping plays when the descriptions (i.e. TTS labels) are loaded.
                    // Generally occurs with a little delay following the tactile rendering
                    showAll = true;
                    pingsPlayer(R.raw.ping);
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        byte[][] dataRead = new byte[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            dataRead[i]= Arrays.copyOfRange(byteArray, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }
        return dataRead;
    }

    // check if selected region is valid
    public static boolean validateRegion(Integer[] region){
        //Log.d("REGION", Arrays.toString(region));
        if (region[0]<region[2] && region[1]<region[3])
            return true;
        else
            return false;
    }

    // show ROI selected
    public static void showRegion(Integer[] region) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        byte[][] data = DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, false);
        byte[][] selection = new byte[brailleServiceObj.getDotLineCount()][];
        //Log.d("DATA", String.valueOf(data[0].length));
        for (int i = 0; i < selection.length; ++i) {
            selection[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            if (i>=region[1] && i<=region[3]) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                outputStream.write( Arrays.copyOfRange(DataAndMethods.data[i], 0 ,region[0] ));
                outputStream.write( Arrays.copyOfRange(data[i], region[0] ,region[2]+1 ) );
                outputStream.write( Arrays.copyOfRange(DataAndMethods.data[i], region[2]+1 ,brailleServiceObj.getDotPerLineCount() ));
                selection[i]= outputStream.toByteArray( );
            }
            else{
                //Log.d("COORDS", String.valueOf(i));
                Arrays.fill(selection[i], (byte) 0x00);
            }
        }
        brailleServiceObj.display(selection);
    }
    // make follow up query to server
    public static void sendFollowUpQuery(String query, Integer[] region){
        // calculate focus values if region exists
        if (region!=null){
            Float[] focus= new Float[]{0f, 0f, 0f, 0f};
            Float[] currentDims = Arrays.stream(zoomBox.split(" ", -1)).map(Float::valueOf).toArray(Float[]::new);
            Float xPerDot = currentDims[2]/ DataAndMethods.brailleServiceObj.getDotPerLineCount();
            Float yPerDot = currentDims[3]/ DataAndMethods.brailleServiceObj.getDotLineCount();
            Float xShift = origRatioDims[0]==0f?0f:(origDims[2]-origRatioDims[0])/2;
            Float yShift = origRatioDims[1]==0f?0f:(origDims[3]-origRatioDims[1])/2;
            //Float a = 0f;
            //Log.d("CHECK!", String.valueOf(0f==a)); // checking java == on float
            // Log.d("ZOOMED_ON", Arrays.toString(currentDims));
            // Log.d("ORIG_DIMS", Arrays.toString(origDims));
            // Log.d("ORIG_RATIO_DIMS", Arrays.toString(origRatioDims));
            // Log.d("REGION", Arrays.toString(region));
            // get outer bounds of region of selected pins within graphic
            focus[0]= (xPerDot*region[0]+currentDims[0]-xShift<origDims[0])?
                    origDims[0] : xPerDot*region[0]+currentDims[0]-xShift;
            focus[1]=(yPerDot*region[1]+currentDims[1]-yShift<origDims[1])?
                    origDims[1] : yPerDot*region[1]+currentDims[1]-yShift;
            focus[2]= Math.min(xPerDot * (region[2] + 1) + currentDims[0]-xShift, (origDims[0] + origRatioDims[0]));
            focus[3]= Math.min(yPerDot * (region[3] + 1) + currentDims[1]-yShift, (origDims[1] + origRatioDims[1]));
            // Log.d("FOCUS", Arrays.toString(focus));
            focus[0] = (focus[0] -origDims[0]) / origRatioDims[0];
            focus[1] = (focus[1] -origDims[1]) / origRatioDims[1];
            focus[2] = (focus[2] -origDims[0]) / origRatioDims[0];
            focus[3] = (focus[3] -origDims[1]) / origRatioDims[1];
            // Log.d("FOCUS", Arrays.toString(focus));
        }



    }
    // this is used to replicate what the outcome of a server call is for the demo example
    public static void makePseudoServerCall(){
        resetGraphicParams();
        image = //"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> " +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:svg=\"http://www.w3.org/2000/svg\" version=\"1.1\"" +
                        "width=\"96\" height=\"40\" id=\"svg135\"> <g class=\"layer\" data-image-layer=\"Layer 1\">" +
                        "\n<ellipse fill=\"none\" stroke=\"#000000\" stroke-width=\"0.885\" id=\"path358\" " +
                        "cx=\"48\" cy=\"20\" rx=\"15\" ry=\"15\" /> </g> \n</svg>";
    }
}
