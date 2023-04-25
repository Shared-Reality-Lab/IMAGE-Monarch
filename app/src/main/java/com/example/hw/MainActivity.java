package com.example.hw;


import static java.util.Map.entry;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.view.GestureDetectorCompat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.scand.svg.SVGHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    static final String TAG = MainActivity.class.getSimpleName();
    private BrailleDisplay brailleServiceObj = null;
    //final private Handler mHandler = new Handler();
    private byte[][] data = null; // byte array used to reset pins

    int layercount; // number of layers found in svg
    String image;// used to store svg in string format

    //presentLayer: The layer to be displayed when pins are raised;
    //fileSelected: file index from the list of files in specified target directory.
    int presentLayer=0, fileSelected=0;


    // short and long descriptions of objects in the present layer
    private ArrayList<String[][]> tags;
    //private ArrayList<byte[][]> dataLayers;

    static TextToSpeech tts;
    private GestureDetectorCompat mDetector;




    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDetector = new GestureDetectorCompat(this,this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);

        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);

        // Arrays for pin down and TTS tags
        data = new byte[brailleServiceObj.getDotLineCount()][];
        for (int i = 0; i < data.length; ++i) {
            data[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(data[i], (byte) 0x00);
        }

        // Initializing separate 2D arrays for short and long descriptions associated with the pins
        tags = new ArrayList<>();
        tags.add(new String [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);
        tags.add(new String [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()]);

        int[] ids = im.getInputDeviceIds();
        for(int i = 0; i < ids.length;++i) {
            Log.d(TAG, "id: " + ids[i] + ":" + im.getInputDevice(ids[i]).getName());
        }


        // Opening first file in the directory to be read by default
        try {
            image=getFile(0)[0];
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
            
        // Setting up TTS
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {

                if(status != TextToSpeech.SUCCESS){
                    Log.e("error", "Initialization Failed!"+status);
                }
                else {
                    tts.setLanguage(Locale.getDefault());
                }


            }});


        brailleServiceObj.registerMotionEventHandler(new BrailleDisplay.MotionEventHandler() {
            @Override
            public boolean handleMotionEvent(MotionEvent e) {
                // Observed limits of IR outputs for the pin array. Might need tweaking for more accurate mapping of finger location to pin...
                float xMin= 0;
                float xMax= 1920;
                float yMin=23;
                float yMax=1080;
                // Calculating pin based on position
                int pinX= (int) (Math.ceil((e.getX()-xMin+0.000001)/((xMax-xMin)/brailleServiceObj.getDotPerLineCount()))-1);
                int pinY= (int) Math.ceil((e.getY()-yMin+0.000001)/((yMax-yMin)/brailleServiceObj.getDotLineCount()))-1;
                //Log.d(TAG, String.valueOf(e.getX())+","+String.valueOf(e.getY())+ " ; "+ pinX+","+pinY);
                try{
                    // This works! Gesture control can now be used along with the handler.
                    onTouchEvent(e);
                }
                catch(RuntimeException ex){
                    Log.d(TAG, String.valueOf(ex));
                }



                return false;
                //return true;
            }
        });

        ((Button) findViewById(R.id.zeros)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                brailleServiceObj.display(data);
            }
        });


        ((Button) findViewById(R.id.ones)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    // Display current layer
                    brailleServiceObj.display(getBitmaps(getfreshDoc(), presentLayer++));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                }
                //Log.d("LAYER!", String.valueOf(presentLayer));
                if (presentLayer==layercount+1)
                    presentLayer=0;
            }
        });

        Switch debugSwitch = (Switch) findViewById(R.id.debugViewSwitch);
        debugSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                brailleServiceObj.setDebugView(debugSwitch.isChecked());
                //audioPlayer("/sdcard/IMAGE/", "audio.mp3");
            }
        });


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
                put(421, "UP");
                put(420, "DOWN");
            }};
            switch (keyMapping.getOrDefault(keyCode, "default")) {
            // Navigating between files
            case "UP":
                    Log.d(TAG, event.toString());
                    changeFile(++fileSelected);
                    return true;
            // Navigating between files
            case "DOWN":
                    Log.d(TAG, event.toString());
                    changeFile(--fileSelected);
                    return true;
            default:
                Log.d(TAG, event.toString());
                return false;
        }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // function to pad the bitmap to match the pin array aspect ratio
    public Bitmap padBitmap(Bitmap bitmap, int padX, int padY)
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
    public String getStringFromDocument(Document doc)
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
    public byte[][] getBitmaps(Document doc, int presentLayer) throws IOException, XPathExpressionException {
        int layer=0;
        //Log.d("LAYER!", String.valueOf(presentLayer));
        XPath xPath = XPathFactory.newInstance().newXPath();
        // get list of layers; Uses default ordering which is expected to be 'document order' but the return type is node-set which is unordered!
        NodeList nodeslist = (NodeList)xPath.evaluate("//*[@data-image-layer]", doc, XPathConstants.NODESET);
        //Log.d("XPATH", String.valueOf(nodeslist.getLength()));
        layercount=nodeslist.getLength();
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            // hide layers which are not the present layer
            if (i!= presentLayer && presentLayer!=layercount) {
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
                    speaker("Layer: "+tag);
                    }
        }
        //If there is no tag as a layer, hide elements unless the full image is to be shown
        if (presentLayer!=layercount){
            
            nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@data-image-layer]) and not(descendant::*[@data-image-layer])] ", doc, XPathConstants.NODESET);
            for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
                Node node = nodeslist.item(i);
                ((Element)node).setAttribute("display","none");
            }
        }
        else{
            speaker("Full image");
        }
        // fetch TTS tags for elements within present layer
        //getDescriptions(doc);
        // get bitmap of present layer
        byte[] byteArray= docToBitmap(doc);
        //Log.d("BITMAP", Arrays.toString(byteArray));
        getDescriptions(doc);
        // reshape byte array into 2D array to match pin array dimensions
        byte[][] dataRead = new byte[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            dataRead[i]=Arrays.copyOfRange(byteArray, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }
        return dataRead;
    }
    // get basic and detailed descriptions
    public void getDescriptions(Document doc) throws XPathExpressionException, IOException {
        //Log.d("GETTING TAGS", "Here!");
        XPath xPath = XPathFactory.newInstance().newXPath();
        // query elements that are in the present layer AND have element level descriptions (NOT layer level descriptions)
        // Assuming that only elements with short description can have a long description here. Is this assumption safe?!
        NodeList nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and (not(self::*[@data-image-layer]) or not(child::*)) and (self::*[@aria-labelledby] or self::*[@aria-label])]", doc, XPathConstants.NODESET);        // temporary var for objects tags
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
                    if (layerDesc[j]==null){
                        layerDesc[j]=detailTag;
                    }
                    else {
                        layerDesc[j]= layerDesc[j] + ", " + tag;
                    }
                }
            }
            /*String[] finalLayerTags = layerTags;
            // mapping pins corresponding to the selected element to its description tag. Unable to directly convert Object array (which is the return type of mapToObj) to String array but can use the funny copy hack to do it!
            layerTags= Arrays.copyOf((IntStream.range(0,layerTags.length).mapToObj(k-> {

                if (byteArray[k]!=0){
                    if (finalLayerTags[k]==null){
                        return tag;
                    }
                    else {
                        return finalLayerTags[k] + ", " + tag;
                    }
                }
                else{
                    return finalLayerTags[k];
                }
            }).collect(Collectors.toList())).toArray(), layerTags.length, String[].class);*/

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
    public byte[] docToBitmap(Document doc) throws IOException {
        String img= getStringFromDocument(doc).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ", "");
        //Log.d("SVG",img);
        Bitmap svg = SVGHelper.noContext().open(img).setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
        int x = svg.getWidth();
        int y = svg.getHeight();
        int[] intArray = new int[brailleServiceObj.getDotPerLineCount() * brailleServiceObj.getDotLineCount()];
        // padding bitmap to fit to pin array size
        Bitmap svgScaled=padBitmap(svg, brailleServiceObj.getDotPerLineCount()-x, brailleServiceObj.getDotLineCount()-y);
        svg.recycle();
        // extracting only the alpha value of bitmap to convert it into a byte array
        Bitmap alphas=svgScaled.extractAlpha();
        int size = alphas.getRowBytes() * alphas.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        alphas.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        return byteArray;
    }
    // fetching the file to read from; returns file contents as String and also the file name
    public String[] getFile(int fileNumber) throws IOException, JSONException {
        File directory = new File("/sdcard/IMAGE/");
        File[] files = directory.listFiles();

        int filecount=files.length;
        if (fileNumber>= filecount)
            fileSelected=0;
        else if (fileNumber<0)
            fileSelected=filecount-1;

        File myExternalFile= new File("/sdcard/IMAGE/", files[fileSelected].getName());
        String myData = "";

        FileInputStream fis = new FileInputStream(myExternalFile);
        DataInputStream in = new DataInputStream(fis);
        BufferedReader br =
                new BufferedReader(new InputStreamReader(in));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            myData = myData + strLine;
        }
        in.close();

        JSONObject reader = new JSONObject(myData);
        JSONObject sys  = reader.getJSONObject("data");
        image = sys.getString("graphic").substring(26);

        byte[] data = image.getBytes("UTF-8");
        data = Base64.decode(data, Base64.DEFAULT);
        image = new String(data, "UTF-8");
        return new String[]{image, files[fileSelected].getName().substring(0, files[fileSelected].getName().length()-5)};
    }

    //similar to getFile. To be used when TTS read out of file name is required. Could possibly replace getFile entirely
    public void changeFile(int fileNumber) throws JSONException, IOException {
        presentLayer=0;
        String[] output=getFile(fileNumber);
        image=output[0];
        speaker("Opening file "+ output[1]);
        brailleServiceObj.display(data);
        return;
    }
    // get fresh copy of the file void of previously made changes
    public Document getfreshDoc() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(image));
        //Log.d("STRING", image);
        Document doc = builder.parse(is);
        return doc;
    }

    // find which pin the finger position corresponds to
    public Integer[] pinCheck(float x, float y){
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

    // TTS speaker. Probably needs a little more work on flushing and/or selecting whether to continue playing
    public void speaker(String text){
        tts.speak (text, TextToSpeech.QUEUE_FLUSH, null, "000000");
        return;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            //Log.d("GESTURE!","In here!");
            return true;
        }
        return super.onTouchEvent(event);
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
        Integer [] pins=pinCheck(event.getX(), event.getY());
        try{
            // Speak out detailed description based on finger location
            speaker(tags.get(1)[pins[1]][pins[0]]);
        }
        catch(RuntimeException ex){
            Log.d(TAG, String.valueOf(ex));
        }
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
        Integer [] pins=pinCheck(event.getX(), event.getY());
        try{
            // Speak out label tags based on finger location
            speaker(tags.get(0)[pins[1]][pins[0]]);
        }
        catch(RuntimeException ex){
            Log.d(TAG, String.valueOf(ex));
        }
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

    public void pingsPlayer(int file){
        //set up MediaPlayer
        MediaPlayer mp = new MediaPlayer();

        try {
            mp=MediaPlayer.create(getApplicationContext(), file);
            mp.start();

        } catch (Exception e) {
            Log.d("ERROR", e.toString());
        }


    }
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
    }

    /*
    //Audio player for media files. Not being used currently
    public void audioPlayer(String path, String fileName){
        //set up MediaPlayer
        MediaPlayer mp = new MediaPlayer();

        try {
            mp.setDataSource(path + File.separator + fileName);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            Log.d("ERROR", e.toString());
        }
    }
    */



}