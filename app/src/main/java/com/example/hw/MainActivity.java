package com.example.hw;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.TypedArrayUtils;

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
import java.util.Locale;
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

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();
    private BrailleDisplay brailleServiceObj = null;
    //final private Handler mHandler = new Handler();
    private byte[][] data = null;
    //private byte[][] dataRead = null;
    int[] intArray;
    int layercount;
    String image;
    int presentLayer=0, fileSelected=0;

    //private ArrayList[][] tags;
    private String[][] tags;

    //private ArrayList<byte[][]> dataLayers;

    static TextToSpeech tts;




    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);

        // Arrays for pin down and TTS tags. Can use forceRefresh() instead of data??
        data = new byte[brailleServiceObj.getDotLineCount()][];
        tags = new String [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(data[i], (byte) 0x00);
        }

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
                if (status == TextToSpeech.SUCCESS) {

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
                    // Speak out TTS tags based on finger location
                    speaker(tags[pinY][pinX]);
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
                    brailleServiceObj.display(getBitmaps(getfreshDoc(), presentLayer+1));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                }
                // Update present layer count
                ++presentLayer;
                if (presentLayer==layercount)
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
        switch (keyCode) {
            // Navigating between files
            case 421:
                try {
                    ++fileSelected;
                    presentLayer=0;
                    String[] output;
                    output=getFile(fileSelected);
                    image=output[0];
                    speaker("Opening file "+ output[1]);
                    brailleServiceObj.display(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                return true;
            case 420:
                try {
                    --fileSelected;
                    presentLayer=0;
                    String[] output;
                    output=getFile(fileSelected);
                    image=output[0];
                    speaker("Opening file "+ output[1]);
                    brailleServiceObj.display(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            default:
                Log.d(TAG, event.toString());
                return false;
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
        XPath xPath = XPathFactory.newInstance().newXPath();
        // get list of layers
        NodeList nodeslist = (NodeList)xPath.evaluate("//*[@data-image-layer]", doc, XPathConstants.NODESET);
        //Log.d("XPATH", String.valueOf(nodeslist.getLength()));
        layercount=nodeslist.getLength()+1;
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            // hide layers which are not the present layer
            if ((i+1)!= presentLayer && presentLayer!=layercount) {
                ((Element)node).setAttribute("display","none");
            }
            // TTS output of layer description
            if ((i+1)==presentLayer){
                //Log.d("GETTING TAGS", String.valueOf(nodeslist.getLength()));
                    String tag;
                    //Log.d("GETTING TAGS", node.getNodeName());
                    if (((Element)node).hasAttribute("aria-describedby")) {
                        tag= doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent();
                        //Log.d("GETTING TAGS", (doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent()));
                        }
                    else{
                        tag=((Element)node).getAttribute("aria-description");
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
        getDescriptions(doc);
        // get bitmap of present layer
        byte[] byteArray= docToBitmap(doc);
        //Log.d("BITMAP", Arrays.toString(byteArray));

        // reshape byte array into 2D array to match pin array dimensions
        byte[][] dataRead = new byte[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            dataRead[i]=Arrays.copyOfRange(byteArray, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
        }
        return dataRead;
    }
    public void getDescriptions(Document doc) throws XPathExpressionException, IOException {
        //Log.d("GETTING TAGS", "Here!");
        XPath xPath = XPathFactory.newInstance().newXPath();
        // query elements that are in the present layer AND have element level descriptions (NOT layer level descriptions)
        NodeList nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and not(self::*[@data-image-layer]) and (self::*[@aria-describedby] or self::*[@aria-description])]", doc, XPathConstants.NODESET);
        String[] layerTags=new String[brailleServiceObj.getDotPerLineCount()*brailleServiceObj.getDotLineCount()];
        //Log.d("GETTING TAGS", String.valueOf(nodeslist.getLength()));
        // intially hiding all elements filtered in the previous stage
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("display", "none");
        }
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            String tag;
            Node node = nodeslist.item(i);
            // fetching the tag for each element 
            if (((Element)node).hasAttribute("aria-describedby")) {
                tag= doc.getElementById(((Element) node).getAttribute("aria-describedby")).getTextContent();
            }
            else{
                tag=((Element)node).getAttribute("aria-description");
            }
            // showing the element whose tag is stored to obtain its bitmap mapping
            ((Element)node).removeAttribute("display");
            byte[] byteArray= docToBitmap(doc);
            String[] finalLayerTags = layerTags;
            // mapping pins corresponding to the selected element to its description tag. Making the funny copy to convert object array to string array!
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
            }).collect(Collectors.toList())).toArray(), layerTags.length, String[].class);
            // hiding element again so we can move on to the next element
            ((Element)node).setAttribute("display", "none");
        }
            // undoing changes made at this stage...
            for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
                Node node = nodeslist.item(i);
                ((Element)node).removeAttribute("display");
            }
            // converting string array into 2D array that maps to the pins
            for (int i = 0; i < data.length; ++i) {
                tags[i]=Arrays.copyOfRange(layerTags, i*brailleServiceObj.getDotPerLineCount(), (i+1)*brailleServiceObj.getDotPerLineCount());
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
        intArray = new int[brailleServiceObj.getDotPerLineCount() * brailleServiceObj.getDotLineCount()];
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
    // fetching the file to read from
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
    // get fresh copy of the file void of previously made changes
    public Document getfreshDoc() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+image));
        Document doc = builder.parse(is);
        return doc;
    }

    // TTS speaker. Probably needs a little more work on flushing and/or selecting whether to continue playing
    public void speaker(String text){
        tts.speak (text, TextToSpeech.QUEUE_FLUSH, null, "000000");
        return;
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