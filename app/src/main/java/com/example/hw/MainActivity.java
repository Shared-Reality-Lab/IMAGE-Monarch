package com.example.hw;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.BrailleDisplay;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();
    private BrailleDisplay brailleServiceObj = null;
    final private Handler mHandler = new Handler();
    private byte[][] data = null;
    //private byte[][] dataRead = null;
    int[] intArray;
    int layercount;
    String image;
    int presentLayer=0;

    private ArrayList[][] tags;

    private ArrayList<byte[][]> dataLayers;




    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);

        data = new byte[brailleServiceObj.getDotLineCount()][];
        //dataRead = new byte[brailleServiceObj.getDotLineCount()][];
        tags = new ArrayList [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(data[i], (byte) 0x00);
            //dataRead[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            //Arrays.fill(dataRead[i], (byte) 0x00);
            for (int j=0; j< brailleServiceObj.getDotPerLineCount(); ++j){
                tags[i][j]=new ArrayList<String>();
            }
        }

        int[] ids = im.getInputDeviceIds();
        for(int i = 0; i < ids.length;++i) {
            Log.d(TAG, "id: " + ids[i] + ":" + im.getInputDevice(ids[i]).getName());
        }

        String filename = "layers.json";
        File myExternalFile;
        String myData = "";
        myExternalFile = new File("/sdcard/IMAGE/", filename);

        /*
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            Log.d("SVG ERROR", "Permission not granted!");
        }
        else{
            Log.d("SVG", "Permission granted!");
        }
         */

        try {
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

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+image));
            Document doc = builder.parse(is);

            NodeList nodeslist = doc.getElementsByTagName("*");;
            //Log.d("XML", String.valueOf(nodeslist.getLength()));
            layercount=getLayerCount(nodeslist);
            dataLayers= new ArrayList();
            //Log.d("XML", String.valueOf(layercount));
            int presentLayer=1;


            for (int i=1; i<=layercount; i++){
                dataLayers.add(createBitmaps(doc, i));
                is = new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+image));
                doc = builder.parse(is);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }




        /*
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {

                if(status != TextToSpeech.SUCCESS){
                    Log.e("error", "Initialization Failed!"+status);
                }


            }});
        Log.e("Engine Info " , String.valueOf(tts.getEngines()));
        */
        /*for (TextToSpeech.EngineInfo engines : tts.getEngines()) {
            Log.d("Engine Info " , engines.toString());
        }*/




        brailleServiceObj.registerMotionEventHandler(new BrailleDisplay.MotionEventHandler() {
            @Override
            public boolean handleMotionEvent(MotionEvent e) {
                float xMin= 0;
                float xMax= 1920;
                float yMin=23;
                float yMax=1080;
                int pinX= (int) (Math.ceil((e.getX()-xMin+0.000001)/((xMax-xMin)/brailleServiceObj.getDotPerLineCount()))-1);
                int pinY= (int) Math.ceil((e.getY()-yMin+0.000001)/((yMax-yMin)/brailleServiceObj.getDotLineCount()))-1;
                //Log.d(TAG, String.valueOf(e.getX())+","+String.valueOf(e.getY())+ " ; "+ pinX+","+pinY);
                try{
                    Log.d(TAG, String.valueOf(tags[pinY][pinX]));
                }
                catch(RuntimeException ex){

                }



                return false;
                //return true;
            }
        });

        ((Button) findViewById(R.id.zeros)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int j = 0; j < data.length; ++j) {
                    Arrays.fill(data[j], (byte) 0x00);
                }
                brailleServiceObj.display(data);

            }
        });


        ((Button) findViewById(R.id.ones)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                brailleServiceObj.display(dataLayers.get(presentLayer));
                ++presentLayer;
                if (presentLayer==layercount)
                    presentLayer=0;
            }
        });

        Switch debugSwitch = (Switch) findViewById(R.id.debugViewSwitch);
        debugSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                brailleServiceObj.setDebugView(debugSwitch.isChecked());
            }
        });


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, event.toString());
        return false;
    }

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

    public int getLayerCount(NodeList nodeslist){
        int layers=0;
        for(int i = 0 ; i < nodeslist.getLength() ; i ++){
            Node node = nodeslist.item(i);
            NamedNodeMap attrs = node.getAttributes();
            for(int j = 0 ; j < attrs.getLength() ; j ++) {
                Attr attribute = (Attr)attrs.item(j);
                if (attribute.getName().equals("data-image-layer"))
                {
                    ++layers;
                }

            }
        }
        if (layers==0)
            layers = 1;
        else
            ++layers;
        return layers;
    }

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

    public byte[][] createBitmaps(Document doc, int presentLayer) throws IOException {
        int layer=0;
        NodeList nodeslist = doc.getElementsByTagName("*");;
        for(int i = 0 ; i < nodeslist.getLength() ; i ++){
            Node node = nodeslist.item(i);
            NamedNodeMap attrs = node.getAttributes();
            int j=0, k=0;
            while (j<attrs.getLength()) {
                Attr attribute = (Attr)attrs.item(j);
                //Log.d("Layers!", attribute.getName());
                if (attribute.getName().equals("data-image-layer"))
                {
                    ++layer;
                    if (layer!= presentLayer && presentLayer!=layercount)
                    {
                        ((Element)node).setAttribute("display","none");
                    }
                }
                ++j;

            }
        }
        String img= getStringFromDocument(doc).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ", "");
        Bitmap svg = SVGHelper.noContext().open(img).setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
        int x = svg.getWidth();
        int y = svg.getHeight();
        intArray = new int[brailleServiceObj.getDotPerLineCount() * brailleServiceObj.getDotLineCount()];
        Bitmap svgScaled=padBitmap(svg, brailleServiceObj.getDotPerLineCount()-x, brailleServiceObj.getDotLineCount()-y);
        svg.recycle();
        svgScaled.getPixels(intArray, 0, brailleServiceObj.getDotPerLineCount(), 0, 0, brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount());

        byte[][] dataRead = new byte[brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int j = 0; j < dataRead.length; ++j) {
            for (int k= 0; k< brailleServiceObj.getDotPerLineCount(); ++k){
                if (intArray[j* brailleServiceObj.getDotPerLineCount()+k] == 0) {
                    dataRead[j][k]=(byte) 0x00;
                    //Log.d("ONES", "Here");
                }
                else{
                    dataRead[j][k]=(byte) 0x01;
                    //tags[j][k].add("Objects");
                }
            }
        }
        //Log.d("LAYERS!", String.valueOf(dataRead));

        return dataRead;
    }


}