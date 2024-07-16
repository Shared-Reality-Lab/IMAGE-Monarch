package ca.mcgill.a11y.image;

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import static ca.mcgill.a11y.image.BaseActivity.channelSubscribed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.http.HttpResponseCache;
import android.os.BrailleDisplay;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import com.scand.svg.SVGHelper;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.CacheResponse;
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
import okhttp3.Cache;
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
    static Integer zoomVal=100;
    static float[] dims=new float[]{0,0, 0, 0};
    static Integer presentLayer = -1, presentTarget = 0;
    static boolean labelFill=true;
    static boolean ttsEnabled=true;
    static boolean zoomingIn=false, zoomingOut=false;
    static Integer layerCount=0;
    static BrailleDisplay brailleServiceObj = null;
    static BrailleDisplay.MotionEventHandler handler;
    private static Call<ResponseFormat> ongoingCall;
    static TextToSpeech tts;
    static Context context;
    static View view;
    static String zoomBox = "";
    static int confirmButton = 504;
    static int backButton = 503;
    public static final int DISK_CACHE_SIZE = 10 * 1024 * 1024;

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
            }});
    }

    //check mode and display appropriate layer
    public static void displayGraphic(int keyCode, String mode){
        try{
            DataAndMethods.ttsEnabled=true;
            DataAndMethods.displayOn= true;
            if(keyCode ==confirmButton){
                DataAndMethods.presentLayer++;
                if (DataAndMethods.presentLayer>=DataAndMethods.layerCount+1)
                    DataAndMethods.presentLayer=0;
            }
            else{
                DataAndMethods.presentLayer--;
                if (DataAndMethods.presentLayer<0)
                    DataAndMethods.presentLayer= DataAndMethods.layerCount;
            }
            brailleServiceObj.display(DataAndMethods.getBitmaps(DataAndMethods.getfreshDoc(), DataAndMethods.presentLayer, true));

        }catch(IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
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

    // fetching the file to read from; returns file contents as String and also the file name
    //public static String[] getFile(int fileNumber) throws IOException, JSONException {
    public static String[] getFile() throws IOException, JSONException {
        // Uncomment the following lines for logging http requests
        //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        //logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                //Need next 2 lines when server response is slow
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .cache(getCache());

        //httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_url))
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();

        MakeRequest makereq= retrofit.create(MakeRequest.class);
        Call<ResponseFormat> call= makereq.checkForUpdates(context.getString(R.string.server_url)+"display/"+channelSubscribed);
        image= makeServerCall(call);
        //DataAndMethods.presentLayer--;
        //DataAndMethods.displayGraphic(DataAndMethods.confirmButton, "Exploration");
        // The regex expression in replaceFirst removes everything following the '.' i.e. .jpg, .png etc.
        return new String[]{image, ""};
    }

    public static void setImageDims() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));
        dims[2] = width;
        dims[3] =height;
        zoomBox = dims[0]+" "+ dims[1] +" "+ width+ " "+ height;
    }
    public static Cache getCache() {
        File cacheDir = new File(context.getCacheDir(), "cache");
        Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
        return cache;
    }

    public static String makeServerCall(Call<ResponseFormat> call){
        // Cancelling any ongoing requests that haven't been completed
        if (ongoingCall!=null){
            ongoingCall.cancel();
        }
        //pingsPlayer(R.raw.image_request_sent);
        call.enqueue(new Callback<ResponseFormat>() {
            @Override
            public void onResponse(Call<ResponseFormat> call, Response<ResponseFormat> response) {
                try {
                    if (response.raw().networkResponse().code() != HttpURLConnection.HTTP_NOT_MODIFIED || image == null) {
                        ResponseFormat resource = response.body();
                        //ResponseFormat.Rendering[] renderings = resource.renderings;
                        image = (resource.graphic).replaceFirst("data:.+,", "");
                        //Log.d("RESPONSE", image);
                        byte[] data = image.getBytes("UTF-8");
                        data = Base64.decode(data, Base64.DEFAULT);
                        image = new String(data, "UTF-8");
                        // Log.d("IMAGE", image);
                        // gets viewBox dims for current image
                        setImageDims();
                        setDefaultLayer(resource.layer);
                        DataAndMethods.displayGraphic(DataAndMethods.confirmButton, "Exploration");
                        //pingsPlayer(R.raw.image_results_arrived);
                        //Reset layer count to 0 before new file is loaded
                        //layerCount = 0;
                        //count guidance targets

                        // Enabling the up button again when the response has been received.
                        //view.findViewById(R.id.ones).setEnabled(true);
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
                if (!call.isCanceled()){
                    pingsPlayer(R.raw.image_error);
                }
            }
        });
        // Saving the in-progress call to allow interruption if needed
        ongoingCall=call;
        return image;
    }

    public static void setDefaultLayer(String layerInput) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
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
        else{
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


    public static void zoom(Integer[] pins, String mode) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));

        if (zoomingIn){
            zoomVal+= 25;
            node.setAttribute("viewBox", zoomer(width, height, zoomVal, pins));
            if (mode.equals("Exploration"))
                brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
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
            }
        }
        return;
    }

    public static void zoomTo(Integer[] pins, Integer zoomValue, String mode) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element node = (Element)((NodeList)xPath.evaluate("/svg", doc, XPathConstants.NODESET)).item(0);
        Float width= Float.valueOf(node.getAttribute("width"));
        Float height= Float.valueOf(node.getAttribute("height"));

        if (zoomVal >= 100){
            zoomVal = zoomValue;
            node.setAttribute("viewBox", zoomer(width, height, zoomVal, pins));
            if (mode.equals("Exploration"))
                brailleServiceObj.display(getBitmaps(doc, presentLayer, false));
        }
        else{
            speaker("Oops! Cannot zoom out further");
        }
        return;
    }
    public static String zoomer(float width, float height, int zoomVal, Integer[] pins){
        float[] press=new float[]{0, 0};
        float sWidth=dims[0], sHeight=dims[1], fWidth=dims[2], fHeight=dims[3];

        float scalingFactor, widthNew= fWidth - sWidth, heightNew= fHeight- sHeight;
        //int bufferPins;


        scalingFactor= brailleServiceObj.getDotPerLineCount()/widthNew;
        //Log.d("SCALE", String.valueOf(scalingFactor));
        press[0] = (pins[0]/scalingFactor) + dims[0];
        scalingFactor = brailleServiceObj.getDotLineCount()/heightNew;
        press[1] = (pins[1]/scalingFactor) + dims[1];

        Log.d("PINS", pins[0]+", "+pins[1]);
        Log.d("PRESS", press[0]+","+press[1]);
        float zoomWidth= width/((float)zoomVal/100);
        float zoomHeight= height/((float)zoomVal/100);
        Log.d("DIMS", dims[0] +", "+dims[1]+", "+dims[2]+", "+dims[3] );
        Log.d("SCALE", zoomWidth+","+zoomHeight);
        scalingFactor = zoomWidth/brailleServiceObj.getDotPerLineCount();
        dims[0] = press[0] - (scalingFactor * (pins[0]));
        dims[2] = dims[0] + zoomWidth;
        scalingFactor = zoomHeight / brailleServiceObj.getDotLineCount();
        dims [1] = press[1] - (scalingFactor * (pins[1]));
        dims[3] = dims[1] + zoomHeight;
        //Log.d("SCALE", String.valueOf(brailleServiceObj.getDotPerLineCount()));

        if (dims[0]<0){ //|| dims[1]<0 || dims[2]> width || dims[3] >height){
            dims[0] =0;
            dims[2] = dims[0] + zoomWidth;
        }
        else if (dims[2]>width){
            dims[2] = width;
            dims[0] = dims[2] -zoomWidth;
        }
        if (dims[1]<0){
            dims[1] =0;
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
