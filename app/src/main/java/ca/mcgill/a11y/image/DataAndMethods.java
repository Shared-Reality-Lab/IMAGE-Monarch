package ca.mcgill.a11y.image;

import static android.text.TextUtils.isEmpty;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.scand.svg.SVGHelper;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
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

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DataAndMethods {
    static boolean displayOn=false;
    static String image=null;
    static byte[][] data = null;
    static ArrayList<String[][]> tags;
    static ArrayList<String[][]> occupancy;
    static int selectObj=-1;
    static String[] selectedIds = null;
    static int tagType = 0;
    static String[] tagTypes = new String[]{"aria-label", "aria-description"};
    static String speechTag = null;
    static float[] target=null;

    static Integer zoomVal=100;
    static float[] dims=new float[]{0,0, 0, 0};

    static Integer presentLayer = -1, presentTarget = 0;
    static Integer fileSelected = 0;
    Integer presentGuidance=0;
    static boolean labelFill=true;
    static boolean ttsEnabled=true;

    static boolean showAll = false;

    static boolean zoomingIn=false, zoomingOut=false;
    static Integer layerCount=0, targetCount;
    static BrailleDisplay brailleServiceObj = null;
    static BrailleDisplay.MotionEventHandler handler;
    private static Call<ResponseFormat> ongoingCall;
    static TextToSpeech tts;
    static Context context;
    static View view;

    static String zoomBox = "";
    static String layerTitle = "";

    public static void initialize(BrailleDisplay brailleServiceObj, Context context, View view){
        DataAndMethods.brailleServiceObj = brailleServiceObj;
        DataAndMethods.context = context;
        DataAndMethods.view = view;

        data = new byte[brailleServiceObj.getDotLineCount()][];
        for (int i = 0; i < data.length; ++i) {
            data[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(data[i], (byte) 0x00);
        }

        tags = new ArrayList<>();
        tags.add(new String [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);
        tags.add(new String [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);

        occupancy = new ArrayList<>();
        occupancy.add(new String [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {

                if(status != TextToSpeech.SUCCESS){
                    Log.e("error", "Initialization Failed!"+status);
                }
                else {
                    tts.setLanguage(Locale.getDefault());
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String s) {

                        }

                        @Override
                        public void onDone(String s) {
                            //Log.d("CHECKING!", s);
                            // plays ping when TTS readout is completed based on utteranceId
                            if (s.equals("ping")){
                                pingsPlayer(R.raw.blip);
                            }
                        }

                        @Override
                        public void onError(String s) {

                        }
                    });
                }
            }}, "com.google.android.tts");
    }

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
        layerCount=nodeslist.getLength();
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            // hide layers which are not the present layer
            if (i!= presentLayer && presentLayer!=layerCount) {
                ((Element)node).setAttribute("display","none");
            }
            // TTS output of layer description
            if (i==presentLayer){
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
                layerTitle = "Layer: " +tag;
                if (readCaption) {
                    speaker("Layer: " + tag);
                }
            }
        }
        //If there is no tag as a layer, hide elements unless the full image is to be shown
        if (presentLayer!=layerCount){

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

        // fetch TTS tags for elements within present layer
        //getDescriptions(doc);
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
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
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

    public static byte[][] getAnnotationBitmaps(Document doc, int presentLayer, boolean readCaption) throws IOException, XPathExpressionException {
        //Log.d("LAYER!", String.valueOf(presentLayer));
        XPath xPath = XPathFactory.newInstance().newXPath();
        // get list of layers; Uses default ordering which is expected to be 'document order' but the return type is node-set which is unordered!
        NodeList nodeslist = (NodeList)xPath.evaluate("//*[@data-image-layer]", doc, XPathConstants.NODESET);
        //Log.d("XPATH", String.valueOf(nodeslist.getLength()));
        layerCount=nodeslist.getLength();
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            // hide layers which are not the present layer
            if (i!= presentLayer && presentLayer!=layerCount) {
                ((Element)node).setAttribute("display","none");
            }
            // TTS output of layer description
            if (i==presentLayer){
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
        if (presentLayer!=layerCount){

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

        // fetch TTS tags for elements within present layer
        //getDescriptions(doc);
        // get bitmap of present layer
        byte[] byteArray= docToBitmap(doc);
        //Log.d("BITMAP", Arrays.toString(byteArray));

        // the TTS tags are fetched in a separate thread.
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    getOccupancy(doc);
                    //Log.d("DESCRIPTIONS", "Description loaded");
                    // This ping plays when the descriptions (i.e. TTS labels) are loaded.
                    // Generally occurs with a little delay following the tactile rendering
                    pingsPlayer(R.raw.ping);
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
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

    public static byte[][] getGuidanceBitmaps(Document doc, int presentGuidance) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        showAll = false;
        String tag = "";
        XPath xPath = XPathFactory.newInstance().newXPath();

        /*NodeList nodeslist = (NodeList)xPath.evaluate("//*[@data-image-target]", doc, XPathConstants.NODESET);
        targetCount=nodeslist.getLength();
        */
        if (targetCount ==0){
            speaker("No guidance mode available. Defaulting to exploration mode");
            return data;
        }
        else if (presentTarget<=0 ){
            presentTarget = targetCount;
        }
        else if (presentTarget > targetCount){
            /*nodeslist = (NodeList)xPath.evaluate("//*[@data-image-target='"+presentTarget+"']", doc, XPathConstants.NODESET);
            Log.d("TARGET", String.valueOf(nodeslist.getLength()));
            if (nodeslist.getLength() == 0){*/
            presentTarget = 1;
        }

        //Log.d("TARGET", String.valueOf(presentTarget)+" "+targetCount);

        NodeList nodeslist = (NodeList) xPath.evaluate("//*[not(descendant-or-self::*[@data-image-target = '"+presentTarget+"'])]", doc, XPathConstants.NODESET);

        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display","none");
        }

        nodeslist = (NodeList) xPath.evaluate("//*[not(ancestor-or-self::*[@display = 'none'] and ancestor::metadata)]", doc, XPathConstants.NODESET);
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i).cloneNode(true);
            doc.getElementsByTagName("svg").item(0).appendChild((Element)node);
        }

        byte[] mask = docToBitmap(doc);
        doc = getTargetLayer(getfreshDoc());
        /*Node targetLayer = ((NodeList) xPath.evaluate("//*[(descendant-or-self::*[@data-image-target = '"+presentTarget+"']) and self::*[@data-image-layer]]", doc, XPathConstants.NODESET)).item(0);
        if (((Element)targetLayer).hasAttribute("aria-labelledby")) {
            tag= doc.getElementById(((Element) targetLayer).getAttribute("aria-labelledby")).getTextContent();
            //Log.d("GETTING TAGS", (doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent()));
        }
        else{
            tag=((Element)targetLayer).getAttribute("aria-label");
            //Log.d("GETTING TAGS", "Otherwise here!");
        }*/
        Node node = ((NodeList) xPath.evaluate("//*[ancestor-or-self::*[@data-image-target = '"+presentTarget+"']]", doc, XPathConstants.NODESET)).item(0);
        if (((Element)node).hasAttribute("aria-labelledby")) {
            tag = doc.getElementById(((Element) node).getAttribute("aria-labelledby")).getTextContent();
            //Log.d("GETTING TAGS", (doc.getElementById(((Element) node).getAttribute("aria-labelledby")).getTextContent()));
        }
        else{
            tag = ((Element)node).getAttribute("aria-label");
            //Log.d("GETTING TAGS",((Element)node).getAttribute("aria-label"));
        }
        layerTitle = tag;
        speaker(tag);
        // guidanceTag = "Layer: " + tag;
        /*
        Node targetLayer = ((NodeList) xPath.evaluate("//*[(descendant-or-self::*[@data-image-target = '"+presentTarget+"']) and self::*[@data-image-layer]]", doc, XPathConstants.NODESET)).item(0);
        //Log.d("TARGET Layer", String.valueOf(((Element) targetLayer).getAttribute("data-image-layer")));
        String layerName = ((Element) targetLayer).getAttribute("data-image-layer");
        nodeslist = (NodeList) xPath.evaluate("//*[not(descendant-or-self::*[@data-image-layer = '"+layerName+"']) and not(ancestor::*[@data-image-layer = '"+layerName+"'])]", doc, XPathConstants.NODESET);
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display","none");
        }*/

        byte[] target = docToBitmap(doc);

        byte[] byteArray = new byte[mask.length];
        for (int i = 0; i < mask.length; i++) {
            byteArray[i] = (byte) (mask[i] * target[i]);
        }
        //Log.d("BITMAP", Arrays.toString(byteArray));

        // the TTS tags are fetched in a separate thread.
        final Handler handler = new Handler(Looper.getMainLooper());
        Document finalDoc = doc;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    getGuidanceDescriptions(finalDoc, mask);
                    //Log.d("DESCRIPTIONS", "Description loaded");
                    // This ping plays when the descriptions (i.e. TTS labels) are loaded.
                    // Generally occurs with a little delay following the tactile rendering
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
        //return data;
    }

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

    public static void getTarget(Document doc) throws XPathExpressionException, IOException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Document doccopy = doc;

        NodeList nodeslist = (NodeList) xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and ((self::*[@data-image-target]))]", doccopy, XPathConstants.NODESET);
        /*for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display", "none");
        }*/

        Element nd = (Element)((NodeList)xPath.evaluate("/svg", doccopy, XPathConstants.NODESET)).item(0);
        Float origWidth= Float.valueOf(nd.getAttribute("width"));
        Float origHeight= Float.valueOf(nd.getAttribute("height"));

        String img = getStringFromDocument(doccopy).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ", "");
        //Log.d("SVG",img);
        //Bitmap svg = SVGHelper.noContext().open(img).getBitmap();
        //int origWidth = svg.getWidth();
        //int origHeight = svg.getHeight();
        //Log.d("TARGET", origWidth+ ", "+ origHeight);
        Bitmap svg = SVGHelper.noContext().open(img).setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
        int svgWidth = svg.getWidth();
        int svgHeight = svg.getHeight();
        //Log.d("TARGET", svgWidth+ ", "+ svgHeight);
        Node node = nodeslist.item(0);
        if (nodeslist.getLength() > 0) {
            // Log.d("VALS", ((Element) node).getAttribute("cx"));
            float x = Float.parseFloat(((Element) node).getAttribute("cx")) * svgWidth / origWidth + (brailleServiceObj.getDotPerLineCount() - svgWidth) / 2;
            float y = Float.parseFloat(((Element) node).getAttribute("cy")) * svgHeight / origHeight + (brailleServiceObj.getDotLineCount() - svgHeight) / 2;
            float r = Float.parseFloat(((Element) node).getAttribute("r")) * svgHeight / origHeight;
            target = new float[]{x, y, r};
            //Log.d("TARGET", x+ ", "+ y);
        }
        else {
            target = null;
        }
    }
    public static float calcDistance(Integer[] pos, float[] target){
        float dist= ((pos[1]-target[1])*(pos[1]-target[1])+ (pos[0]-target[0])*(pos[0]-target[0]));
        // Log.d("x", String.valueOf(target[0]));
        // Log.d("y", String.valueOf(target[1]));
        return dist;
    }

    public static float calcAngle(Integer[] pos, float[] target){
        Float angle = null, adj=null;
        if (pos[1]> target[1]) {
            adj=pos[1]-target[1];
            Log.d("ANGLE", "Target Y smaller");
        }
        else{
            Log.d("ANGLE", "Target Y greater");
            adj= target[1]- pos[1];
            if (adj==0){
                if (target[0]>pos[0]){
                    return 0;
                }
                else{
                    return (float)Math.PI/2;
                }
            }
        }

        if (target[0]>pos[0]){
            Log.d("ANGLE", "Target X greater");
            angle = (float) Math.atan((target[0]-pos[0])/(adj));
            if (angle>Math.PI/4){
                return 0;
            }
            else{
                return (float) Math.PI/4- angle;
            }
        }
        else{
            Log.d("ANGLE", "Target X smaller");
            angle = (float) Math.atan((pos[0]- target[0])/(adj));
            if (angle>Math.PI/4){
                return (float) (Math.PI/2);
            }
            else{
                return (float) Math.PI/4+ angle;
            }
        }

    }

    //get positions of various elements within the svg
    public static void getOccupancy(Document doc) throws XPathExpressionException, IOException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display=none]) and not(descendant::*[@display=none]) and (parent::*[@data-image-layer] or (self::*[@data-image-layer] and not(child::*)))]", doc, XPathConstants.NODESET);
        String[] occTags=new String[brailleServiceObj.getDotPerLineCount()*brailleServiceObj.getDotLineCount()];

        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display", "none");
        }
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            String tag;
            Node node = nodeslist.item(i);
            // fetching the tag for each element
            tag = ((Element) node).getAttribute("id");

            if (labelFill) {
                ((Element) node).setAttribute("fill", "black");
            }
            // showing the element whose tag is stored to obtain its bitmap mapping
            ((Element)node).removeAttribute("display");
            byte[] byteArray= docToBitmap(doc);
            for (int j=0; j<occTags.length; j++){
                if (byteArray[j]!=0){
                    if (occTags[j]==null){
                        occTags[j]=tag;
                    }
                    else {
                        occTags[j]= occTags[j] + ", " + tag;
                    }
                }
            }
            // hiding element again so we can move on to the next element
            ((Element)node).setAttribute("display", "none");
        }

        // converting string array into 2D array that maps to the pins
        for (int i = 0; i < data.length; ++i) {
            occupancy.get(0)[i]=Arrays.copyOfRange(occTags, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }
        return;
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
            }
            else{
                tag=((Element)node).getAttribute("aria-label");
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

    public static void getGuidanceDescriptions(Document doc, byte[] mask) throws XPathExpressionException, IOException {
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
            }
            else{
                tag=((Element)node).getAttribute("aria-label");
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
            NodeList nl = (NodeList)xPath.evaluate("//*[@data-image-target='"+presentTarget+"' and @data-image-solo='true']", doc, XPathConstants.NODESET);
            String defaultTag= null;
            if (nl.getLength()>0){
                defaultTag = ((Element) (nl.item(0))).getAttribute("aria-label");
            }
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
                if (mask[j]==0){
                    layerTags[j] = null;
                    layerDesc[j] = null;
                }
                if (defaultTag!=null && layerTags[j]!=null){
                    layerTags[j] = defaultTag;
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
        Bitmap svg = SVGHelper.noContext().open(img).setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
        int x = svg.getWidth();
        int y = svg.getHeight();
        // padding bitmap to fit to pin array size
        //Log.d("PARAMS", x+","+y+","+brailleServiceObj.getDotPerLineCount());
        Bitmap svgScaled=padBitmap(svg, brailleServiceObj.getDotPerLineCount()-x, brailleServiceObj.getDotLineCount()-y);
        // extracting only the alpha value of bitmap to convert it into a byte array
        Bitmap alphas=svgScaled.extractAlpha();
        int size = alphas.getRowBytes() * alphas.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        alphas.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        return byteArray;
    }

    // fetching the file to read from; returns file contents as String and also the file name
    public static String[] getFile(int fileNumber) throws IOException, JSONException {
        /*String folderName= "/sdcard/IMAGE/client/";
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
        */
        // Uncomment the following lines for logging http requests
        //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        //logging.setLevel(HttpLoggingInterceptor.Level.BODY);


        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                //Need next 2 lines when server response is slow
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS);

        //httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_url))
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();

        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.checkForUpdates();
        image= makeServerCall(call);
        // The regex expression in replaceFirst removes everything following the '.' i.e. .jpg, .png etc.
        return new String[]{image, ""};
    }

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

        //XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));
        zoomBox = dims[0]+" "+ dims[1] +" "+ width+ " "+ height;

        //Log.d("TARGETS", String.valueOf(targetCount));
    }

    public static String makeServerCall(Call<ResponseFormat> call){
        // Cancelling any ongoing requests that haven't been completed
        if (ongoingCall!=null){
            ongoingCall.cancel();
        }
        pingsPlayer(R.raw.image_request_sent);
        // Disabling the up button when request is in progress to prevent catastrophic fails
        view.findViewById(R.id.ones).setEnabled(false);
        call.enqueue(new Callback<ResponseFormat>() {
            @Override
            public void onResponse(Call<ResponseFormat> call, Response<ResponseFormat> response) {
                try {

                        ResponseFormat resource = response.body();
                        ResponseFormat.Rendering[] renderings = resource.renderings;
                        image =(renderings[0].data.graphic).replaceFirst("data:.+,", "");
                        //Log.d("RESPONSE", image);
                        byte[] data = image.getBytes("UTF-8");
                        data = Base64.decode(data, Base64.DEFAULT);
                        image = new String(data, "UTF-8");
                        pingsPlayer(R.raw.image_results_arrived);
                        //Reset layer count to 0 before new file is loaded
                        layerCount = 0;
                        //count guidance targets
                        targetCounts();
                        // Enabling the up button again when the response has been received.
                        view.findViewById(R.id.ones).setEnabled(true);

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
                if (!call.isCanceled()){
                    pingsPlayer(R.raw.image_error);
                }
            }
        });
        // Saving the in-progress call to allow interruption if needed
        ongoingCall=call;
        return image;
    }

    //similar to getFile. To be used when TTS read out of file name is required. Could possibly replace getFile entirely
    public static void changeFile(int fileNumber) throws JSONException, IOException {
        presentLayer=0;
        brailleServiceObj.display(data);
        String[] output=getFile(fileNumber);
        image=output[0];
        //speaker("Opening file "+ output[1]);
        //Log.d("FILENAME", output[1]);
        return;
    }

    // get file mime type for photos
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
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
        node.setAttribute("viewBox", zoomBox );
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

    public static void fetchObjects(Integer[] pins) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        if (occupancy.get(0)[pins[1]][pins[0]]!=null){
            selectedIds= (occupancy.get(0)[pins[1]][pins[0]]).split(", ");
        }
        else{
            selectedIds=null;
            speaker("Nothing to annotate");
        }
    }

    public static void dispSelectedObjs() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        if (selectObj>=selectedIds.length){
            selectObj =0;
        } else if (selectObj<0) {
            selectObj = selectedIds.length-1;
        }
        String id = selectedIds[selectObj];
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeslist = ((NodeList)xPath.evaluate("//*[not(self::*[@id='"+id+"']) and not(descendant::*[@id='"+id+"']) and not(ancestor::*[@id='"+id+"']) and not(descendant::*[@data-image-layer])]", doc, XPathConstants.NODESET));
        for(int i=0;i<nodeslist.getLength(); i++){
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display","none");
        }
        /*
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        if (node.hasAttribute("viewBox")) {
            Float[] dim = new Float[]{dims[0], dims[1], dims[2] - dims[0], dims[3] - dims[1]};
            String dimensions = Arrays.toString(dim).replaceAll(",", "");
            node.setAttribute("viewBox", dimensions.substring(1, dimensions.length() - 1));
        }*/
        brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
    }

    public static void setTagType(){
        if (DataAndMethods.tagType< 0){
            DataAndMethods.tagType = DataAndMethods.tagTypes.length - 1;
        }
        else if (DataAndMethods.tagType>=DataAndMethods.tagTypes.length) {
            DataAndMethods.tagType = 0;
        }
        switch (DataAndMethods.tagTypes[DataAndMethods.tagType]){
            case "aria-label":
                DataAndMethods.speaker("Editing label");
                break;
            case "aria-description":
                DataAndMethods.speaker("Editing description");
                break;
            default:
                DataAndMethods.speaker("Error");
                break;
        }
    }

    public static void saveTag() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("//*[@id='"+selectedIds[selectObj]+"']", doc, XPathConstants.NODESET)).item(0);
        if (speechTag != null) {
            node.setAttribute(tagTypes[tagType], speechTag);
            image = getStringFromDocument(doc);
            Log.d("IMAGE", image);
            speaker("Tag saved!");
        }
        else{
            speaker("No tag available");
        }
    }

    public static void zoom(Integer[] pins, String mode) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));

        if (zoomingIn){
            zoomVal+= 25;
            node.setAttribute("viewBox", zoomer(width, height, zoomVal, pins));
            if (mode.equals("EXPLORATION"))
                brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
            else if (mode.equals("GUIDANCE"))
                brailleServiceObj.display(getGuidanceBitmaps(doc, presentLayer));
            else
                brailleServiceObj.display(getAnnotationBitmaps(doc, presentLayer, false));
        }
        else{
            if (zoomVal>=125) {
                zoomVal-= 25;
                node.setAttribute("viewBox", zoomer(width, height, zoomVal, pins));
                if (mode.equals("Exploration"))
                    brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
                else if (mode.equals("Guidance"))
                    brailleServiceObj.display(getGuidanceBitmaps(doc, presentLayer));
                else
                    brailleServiceObj.display(getAnnotationBitmaps(doc, presentLayer, false));
            }
            else
                speaker("Oops! Cannot zoom out further");
        }
        return;
    }

    public static String zoomer(float width, float height, int zoomVal, Integer[] pins){
        float[] press=new float[]{0, 0};
        float sWidth=0, sHeight=0, fWidth=width, fHeight=height;
        if (dims[2]!=0 & dims[3]!=0){
            sWidth=dims[0];
            sHeight=dims[1];
            fWidth=dims[2];
            fHeight=dims[3];
        }
        float scalingFactor, widthNew= fWidth - sWidth, heightNew= fHeight- sHeight;
        int bufferPins;
        if (Math.abs(widthNew- brailleServiceObj.getDotPerLineCount())<Math.abs(heightNew- brailleServiceObj.getDotLineCount())){
            scalingFactor= brailleServiceObj.getDotPerLineCount()/widthNew;
            // Log.d("ZOOM", String.valueOf(scalingFactor));
            press[0]= pins[0]/scalingFactor;
            bufferPins = brailleServiceObj.getDotLineCount()- (int) Math.ceil(heightNew*scalingFactor);
            if (pins[1]<=((bufferPins/2)-1)){
                press[1]= sHeight;
            }
            else if(pins[1]>= (brailleServiceObj.getDotLineCount()- (Math.floor(bufferPins/2) -1))){
                press[1]= fHeight;
            }
            else{
                press[1]= pins[1]/scalingFactor;
            }
        }
        else{
            scalingFactor= brailleServiceObj.getDotLineCount()/heightNew;
            // Log.d("ZOOM", String.valueOf(scalingFactor));
            press[1]=pins[1]/scalingFactor;
            bufferPins = brailleServiceObj.getDotPerLineCount()- (int) Math.ceil(width*scalingFactor);
            if (pins[0]<=((bufferPins/2)-1)){
                press[0]= sWidth;
            }
            else if(pins[0]>= (brailleServiceObj.getDotPerLineCount()- (Math.floor(bufferPins/2) -1))){
                press[0]= fWidth;
            }
            else{
                press[0]= pins[0]/scalingFactor;
            }
        }

        float zoomWidth= width/((float)zoomVal/100);
        float zoomHeight= height/((float)zoomVal/100);

        dims[0]= press[0]-zoomWidth/2;
        dims[1]= press[1]-zoomHeight/2;
        dims[2]= press[0]+zoomWidth/2;
        dims[3]= press[1]+zoomHeight/2;

        if (dims[0]<0.001){
            dims[2]=press[0]+zoomWidth/2-dims[0];
            dims[0]=0;
        }
        else if (dims[2]>width){
            dims[0]=press[0]-zoomWidth/2- (dims[2]-width);
            dims[2]=width;
        }
        if (dims[1]<0.001){
            dims[3]=press[1]+zoomHeight/2-dims[1];
            dims[1]=0;
        }
        else if (dims[3]>height){
            dims[1]=press[1]-zoomHeight/2- (dims[3]-height);
            dims[3]=height;
        }
        Float[] zooming= new Float[]{dims[0], dims[1], zoomWidth, zoomHeight};
        String zoomDims= Arrays.toString(zooming).replaceAll(",", "");
        //Log.d("ZOOM",Arrays.toString(press));
        // Log.d("ZOOM",zoomDims);
        zoomBox = zoomDims.substring(1,zoomDims.length() - 1);
        return zoomBox;
    }

    public static void pan(int keyCode, String className) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));
        Float widthShift= (dims[2]-dims[0])/10;
        Float heightShift=(dims[3]-dims[1])/10;
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

        if (dims[0]<0.001){
            dims[2]-=dims[0];
            dims[0]=0;
        }
        else if (dims[2]>width){
            dims[0]-= (dims[2]-width);
            dims[2]=width;
        }
        if (dims[1]<0.001){
            dims[3]-=dims[1];
            dims[1]=0;
        }
        else if (dims[3]>height){
            dims[1]-=(dims[3]-height);
            dims[3]=height;
        }
        Float[] panning= new Float[]{dims[0], dims[1], dims[2]-dims[0], dims[3]-dims[1]};
        String panned= Arrays.toString(panning).replaceAll(",", "");
        zoomBox = panned.substring(1,panned.length() - 1);
        node.setAttribute("viewBox", zoomBox);
        if (className.equals("Exploration"))
            brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
        else if (className.equals("Guidance"))
            brailleServiceObj.display(getGuidanceBitmaps(doc, presentLayer));
        else
            brailleServiceObj.display(getAnnotationBitmaps(doc, presentLayer, false));
    }

    // TTS speaker. Probably needs a little more work on flushing and/or selecting whether to continue playing
    public static void speaker(String text, String... utterId){
        tts.speak (text, TextToSpeech.QUEUE_FLUSH, null, utterId.length > 0 ? utterId[0]  : "000000");
        return;
    }

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

}
