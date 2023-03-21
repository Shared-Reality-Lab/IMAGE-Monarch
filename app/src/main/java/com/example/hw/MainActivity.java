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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.scand.svg.SVGHelper;

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();
    private BrailleDisplay brailleServiceObj = null;
    final private Handler mHandler = new Handler();
    private byte[][] data = null;
    private byte[][] dataRead = null;
    int[] intArray;

    private ArrayList[][] tags;






    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        brailleServiceObj = (BrailleDisplay) getSystemService(BrailleDisplay.BRAILLE_DISPLAY_SERVICE);

        data = new byte[brailleServiceObj.getDotLineCount()][];
        dataRead = new byte[brailleServiceObj.getDotLineCount()][];
        tags = new ArrayList [brailleServiceObj.getDotLineCount()][brailleServiceObj.getDotPerLineCount()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(data[i], (byte) 0x00);
            dataRead[i] = new byte[brailleServiceObj.getDotPerLineCount()];
            Arrays.fill(dataRead[i], (byte) 0x00);
            for (int j=0; j< brailleServiceObj.getDotPerLineCount(); ++j){
                tags[i][j]=new ArrayList<String>();
            }
        }

        int[] ids = im.getInputDeviceIds();
        for(int i = 0; i < ids.length;++i) {
            //im.getInputDevice(ids[i]).getName();
            Log.d(TAG, "id: " + ids[i] + ":" + im.getInputDevice(ids[i]).getName());
        }

        String filename = "circle.svg";
        File myExternalFile;
        //String myData = "";
        myExternalFile = new File("/sdcard/IMAGE/", filename);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            Log.d("SVG ERROR", "Permission not granted!");
        }
        else{
            Log.d("SVG", "Permission granted!");
        }

        /*
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
        } catch (IOException e) {
            Log.d("SVG ERROR", String.valueOf(e));

        }
        */


        try {
            Bitmap svg = SVGHelper.noContext().open("<svg width=\"96\" height=\"40\" viewbox=\"0 0 96 40\"> \n" +
                    "  <circle cx=\"48\" cy=\"20\" r=\"20\" fill=\"black\"/> <circle cx=\"0\" cy=\"20\" r=\"20\" fill=\"black\"/>\n" +
                    "</svg>").setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
            //Bitmap svg = SVGHelper.noContext().open(myExternalFile).setRequestBounds(brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount()).getBitmap();
            Log.d("SVG", "SVG converted!");
            Log.d("SVG", "Size"+ svg.getWidth()+","+svg.getHeight());
            /*
            int size = svg.getRowBytes() * svg.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);
            svg.copyPixelsToBuffer(byteBuffer);
            byte[] byteArray = byteBuffer.array();
             */
            /*
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            svg.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            svg.recycle();
            */

            Log.d("SVG VALS", String.valueOf(svg));
            int x = svg.getWidth();
            int y = svg.getHeight();
            intArray = new int[brailleServiceObj.getDotPerLineCount() * brailleServiceObj.getDotLineCount()];
            Bitmap svgScaled=padBitmap(svg, brailleServiceObj.getDotPerLineCount()-x, brailleServiceObj.getDotLineCount()-y);
            svg.recycle();
            svgScaled.getPixels(intArray, 0, brailleServiceObj.getDotPerLineCount(), 0, 0, brailleServiceObj.getDotPerLineCount(), brailleServiceObj.getDotLineCount());
            //Log.d("SVG SIZE", svgScaled.getWidth()+ ", "+svgScaled.getHeight());
            //svg.getPixels(intArray, 0, x, 0, 0, x, y);
            int numZeros = 0;

            for (int i = 0; i < intArray.length; i++) {
                if (intArray[i] == 0) {
                    numZeros++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int j = 0; j < dataRead.length; ++j) {
            for (int k= 0; k< brailleServiceObj.getDotPerLineCount(); ++k){
                if (intArray[j* brailleServiceObj.getDotPerLineCount()+k] == 0) {
                    dataRead[j][k]=(byte) 0x00;
                    //Log.d("ONES", "Here");
                }
                else{
                    dataRead[j][k]=(byte) 0x01;
                    tags[j][k].add("Circle");
                }
            }
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
        //Log.d("PINARRAY", String.valueOf(brailleServiceObj.getDotLineCount())+","+String.valueOf(brailleServiceObj.getDotPerLineCount()));



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
                /*
                for (int j = 0; j < data.length; ++j) {
                    for (int k= 0; k< brailleServiceObj.getDotPerLineCount(); ++k){
                        if (intArray[j* brailleServiceObj.getDotPerLineCount()+k] == 0) {
                            data[j][k]=(byte) 0x00;
                            //Log.d("ONES", "Here");
                        }
                        else{
                            data[j][k]=(byte) 0x01;
                        }
                    }
                }*/
                brailleServiceObj.display(dataRead);

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


}