package com.example.hw;


import static java.util.Map.entry;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.media.MediaPlayer;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.view.GestureDetectorCompat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.image.MakeRequest;
import com.image.MapRequestFormat;
import com.image.PhotoRequestFormat;
import com.image.ResponseFormat;
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

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, MediaPlayer.OnCompletionListener {
    static final String TAG = MainActivity.class.getSimpleName();
    private BrailleDisplay brailleServiceObj = null;
    //final private Handler mHandler = new Handler();
    private byte[][] data = null; // byte array used to reset pins

    //variable to change whether TTS tags fill the object or are assigned only to the raised edges.
    boolean labelFill=true;
    int layercount; // number of layers found in svg
    String image;// used to store svg in string format

    //presentLayer: The layer to be displayed when pins are raised;
    //fileSelected: file index from the list of files in specified target directory.
    int presentLayer=0, fileSelected=0;

    // keyCode of confirm button as per current standard
    int confirmButton = 504;

    // short and long descriptions of objects in the present layer
    private ArrayList<String[][]> tags;
    //private ArrayList<byte[][]> dataLayers;

    static TextToSpeech tts;
    private GestureDetectorCompat mDetector;

    // ongoing server request
    private Call<ResponseFormat> ongoingCall;




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
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String s) {

                        }

                        @Override
                        public void onDone(String s) {
                            //Log.d("CHECKING!", s);
                            // plays ping when TTS readout is completed based on utteranceId
                            if (s.equals("ping")){
                                pingsPlayer(R.raw.start);
                            }
                        }

                        @Override
                        public void onError(String s) {

                        }
                    });
                }
            }});

        brailleServiceObj.registerMotionEventHandler(new BrailleDisplay.MotionEventHandler() {
            @Override
            public boolean handleMotionEvent(MotionEvent e) {
                /*// Observed limits of IR outputs for the pin array. Might need tweaking for more accurate mapping of finger location to pin...
                float xMin= 0;
                float xMax= 1920;
                float yMin=23;
                float yMax=1080;
                // Calculating pin based on position
                int pinX= (int) (Math.ceil((e.getX()-xMin+0.000001)/((xMax-xMin)/brailleServiceObj.getDotPerLineCount()))-1);
                int pinY= (int) Math.ceil((e.getY()-yMin+0.000001)/((yMax-yMin)/brailleServiceObj.getDotLineCount()))-1;
                //Log.d(TAG, String.valueOf(e.getX())+","+String.valueOf(e.getY())+ " ; "+ pinX+","+pinY);
                */
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

        ((Button) findViewById(R.id.zeros)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.zeros)).hasFocus() &&
                        keyEvent.getKeyCode()==confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                    brailleServiceObj.display(data);
                }
                return false;
            }
        });

        ((Button) findViewById(R.id.ones)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.ones)).hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
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
                return false;
            }
        });

        ((Button) findViewById(R.id.getMap)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (((Button) findViewById(R.id.getMap)).hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                try {
                    Double latitude= Double.parseDouble(((EditText) findViewById(R.id.latitude)).getText().toString());
                    Double longitude= Double.parseDouble(((EditText) findViewById(R.id.longitude)).getText().toString());
                    getMap(latitude, longitude);

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                catch (NumberFormatException e){
                    //speaker("Invalid coordinates");
                }
            }
                return false;
            }
        });

        findViewById(R.id.getMap).setEnabled(false);
        findViewById(R.id.longitude).setEnabled(false);
        findViewById(R.id.latitude).setEnabled(false);

        Switch debugSwitch = (Switch) findViewById(R.id.debugViewSwitch);
        debugSwitch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (debugSwitch.hasFocus() &&
                        keyEvent.getKeyCode()== confirmButton &&
                        keyEvent.getAction()== KeyEvent.ACTION_DOWN){
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
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            Map<Integer, String> keyMapping = new HashMap<Integer, String>() {{
                put(421, "UP");
                put(420, "DOWN");
                // shortcut access for demo
                put (503, "MAP1");
                put (499, "MAP2");
                put (498, "MAP3");
                put (497, "MAP4");
                put (500, "MAP5");

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
                case "MAP1":
                        Log.d(TAG, event.toString());
                        //speaker("Technologie HumanWare, Longueuil");
                        getMap(45.54646, -73.49546);
                        return true;
                case "MAP2":
                        Log.d(TAG, event.toString());
                        //speaker("Technologie HumanWare, Drummondville");
                        getMap(45.887950, -72.539620);
                        return true;
                case "MAP3":
                        Log.d(TAG, event.toString());
                        //speaker("Technologie HumanWare, Europe");
                        getMap(52.29810,  -0.62327);
                        return true;
                case "MAP4":
                        Log.d(TAG, event.toString());
                        //speaker("McGill Metro Station");
                        getMap( 45.504111, -73.5715456);
                        return true;
                case "MAP5":
                        Log.d(TAG, event.toString());
                        //speaker("Hilton Americas-Houston");
                        getMap(29.75155505, -95.36065968437319);
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

        // the TTS tags are fetched in a separate thread.
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    getDescriptions(doc);
                    //Log.d("DESCRIPTIONS", "Description loaded");
                    // This ping plays when the descriptions (i.e. TTS labels) are laoded.
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
        Log.d("LOG", "Opening file: "+files[fileSelected].getName());
        /*
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inJustDecodeBounds = true;
        Log.d("FILES", files[fileSelected].getName());
        Bitmap bitmap = BitmapFactory.decodeFile("/sdcard/IMAGE/client/"+files[fileSelected].getName());
        // This works for other file types (png, avif) as well despite being specified as jpeg
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
         */

        String base64 = "data:"+ getMimeType(files[fileSelected].getName())+";base64,"+ Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        Integer[] dims= new Integer[] {bitmap.getWidth(), bitmap.getHeight()};
        PhotoRequestFormat req= new PhotoRequestFormat();
        req.setValues(base64, dims);

        // Uncomment the following lines for logging http requests
        //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        //logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
        //Need next 2 lines when server response is slow
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS);

        //httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://image.a11y.mcgill.ca/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();

        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.makePhotoRequest(req);
        image= makeServerCall(call);
        // The regex expression in replaceFirst removes everything following the '.' i.e. .jpg, .png etc.
        return new String[]{image, files[fileSelected].getName().replaceFirst("\\.[^.]*$", "")};
    }

    public String getMap(Double lat, Double lon) throws JSONException {
        presentLayer=0;
        MapRequestFormat req= new MapRequestFormat();
        req.setValues(lat, lon);
        // Uncomment the following lines for logging http requests
        //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        //logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        //httpClient.addInterceptor(logging);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://image.a11y.mcgill.ca/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.makeMapRequest(req);
        //speaker("Opening map");
        brailleServiceObj.display(data);
        image= makeServerCall(call);
        return image;
    }


    public String makeServerCall(Call<ResponseFormat> call){
        // Cancelling any ongoing requests that haven't been completed
        if (ongoingCall!=null){
            ongoingCall.cancel();
        }
        pingsPlayer(R.raw.image_request_sent);
        // Disabling the up button when request is in progress to prevent catastrophic fails
        findViewById(R.id.ones).setEnabled(false);
        call.enqueue(new Callback<ResponseFormat>() {
            @Override
            public void onResponse(Call<ResponseFormat> call, Response<ResponseFormat> response) {
                try {
                    ResponseFormat resource= response.body();
                    ResponseFormat.Rendering[] renderings = resource.renderings;
                    image= (renderings[0].data.graphic).replaceFirst("data:.+,", "");
                    //Log.d("RESPONSE", image);
                    byte[] data = new byte[0];
                    data = image.getBytes("UTF-8");
                    data = Base64.decode(data, Base64.DEFAULT);
                    image = new String(data, "UTF-8");
                    pingsPlayer(R.raw.image_results_arrived);
                    // Enabling the up button again when the response has been received.
                    findViewById(R.id.ones).setEnabled(true);
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                // This occurs when there is no rendering returned
                catch (ArrayIndexOutOfBoundsException| NullPointerException e){
                    speaker("Request failed!");
                }
            }

            //onFailure is called both when a request is cancelled (i.e. interrupted with another request)
            // AND when it fails to give a valid response
            @Override
            public void onFailure(Call<ResponseFormat> call, Throwable t) {
                // Ensure that a request was cancelled before reading out 'Request failed' TTS
                // This text is not read out when a request is cancelled as there is expected to be
                // an ongoing request and can be confused as a result of that request.
                // Causes interrupted requests to die silently!
                if (!call.isCanceled()){
                    speaker("Request failed!");
                }
            }
        });
        // Saving the in-progress call to allow interruption if needed
        ongoingCall=call;
        return image;
    }

    //similar to getFile. To be used when TTS read out of file name is required. Could possibly replace getFile entirely
    public void changeFile(int fileNumber) throws JSONException, IOException {
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
    public void speaker(String text, String... utterId){
        tts.speak (text, TextToSpeech.QUEUE_FLUSH, null, utterId.length > 0 ? utterId[0]  : "000000");
        return;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            int action = event.getActionMasked();
            if (action==MotionEvent.ACTION_UP)
            {
                Integer [] pins=pinCheck(event.getX(), event.getY());
                try{
                    // Speak out label tags based on finger location and ping when detailed description is available
                    if ((tags.get(1)[pins[1]][pins[0]]!=null) && (tags.get(1)[pins[1]][pins[0]].trim().length() > 0))
                    {
                        //Log.d("CHECKING!", tags.get(1)[pins[1]][pins[0]]);
                        speaker(tags.get(0)[pins[1]][pins[0]], "ping");
                    }
                    else{
                        speaker(tags.get(0)[pins[1]][pins[0]]);
                    }
                }
                catch(RuntimeException ex){
                    Log.d(TAG, String.valueOf(ex));
                }
            }
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
        /*Integer [] pins=pinCheck(event.getX(), event.getY());
        try{
            // Speak out label tags based on finger location
            speaker(tags.get(0)[pins[1]][pins[0]]);
        }
        catch(RuntimeException ex){
            Log.d(TAG, String.valueOf(ex));
        }*/
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

    // plays audio from resource file. Has provision for other playing indicator tones if required
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