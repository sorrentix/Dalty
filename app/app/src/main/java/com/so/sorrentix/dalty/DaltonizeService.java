package com.so.sorrentix.dalty;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import com.so.sorrentix.dalty.util.ImageResizer;
import com.so.sorrentix.dalty.util.Message;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;



/**
 * Created by sorrentix on 30/06/2016.
 */
public class DaltonizeService extends IntentService {


    private static final String TAG = "Dalty - DH";

    static{ System.loadLibrary("opencv_java3"); }

    private FileHandler fileHandler;

    public static final int PATHOLOGY_PT = 0;
    public static final int PATHOLOGY_DT = 1;
    public static final int PATHOLOGY_TT = 2;

    private double PT [][];
    private double DT[][];
    private double TT[][];
    private double BT[][];


    private int i=0;
    private int j=0;

    private int patologia=0;

    private double r=0.0;
    private double g=0.0;
    private double b=0.0;
    private double a=255.0;

    private double l=0.0;
    private double m=0.0;
    private double s=0.0;

    private double L=0.0;
    private double M=0.0;
    private double S=0.0;

    private double R=0.0;
    private double G=0.0;
    private double B=0.0;

    private double RR=0.0;
    private double GG=0.0;
    private double BB=0.0;

    private Mat src;
    private Mat dst;

    private Bitmap bufferBitmap;
    private File newFile;


    public DaltonizeService() {
        super("Daltonize-service");

    }


    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
        PT = new double[][]{
                {0.0, 2.02344, -2.52581},
                {0.0, 1.0, 0.0},
                {0.0, 0.0, 1.0}
        };

        DT =  new double[][]{
                {1.423197232849630,  -0.889952950341190,   1.775573701165630},
                {0.675585745777957,  -0.422038224770236,   2.827886880605507},
                {0.002673515164406,  -0.005044273480993,   0.999140991920655},
        };

        TT = new double[][]{
                {0.954513350492442,    -0.047194810276966,      2.748724340881226},
                {-0.004474421007726,    0.965430146500889,      0.888357061641570},
                {-0.012517640592267,    0.073122627552972,     -0.011617246993332}
        };

        BT = new double[3][3];

        fileHandler = new FileHandler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Extract the receiver passed into the service
        int noti_icon = intent.getIntExtra(Message.NOTI_ICON,0);
        Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle("Servizio Dalty")
                .setContentText("Service is active and is processing the image")
                .setSmallIcon(noti_icon)
                .build();

        Log.d(TAG,""+noti.toString());
        startForeground(101, noti);
        ResultReceiver rec = intent.getParcelableExtra(Message.RECEIVER_TAG);
        // Extract additional values from the bundle
        String imagePath = intent.getStringExtra(Message.IMG_PATH);
        int pat = intent.getIntExtra(Message.PATHOLOGY,0);


        bufferBitmap = ImageResizer.checkAndRotate(imagePath,ImageResizer.decodeSampledBitmapFromFile(imagePath,1280,1280,null));

        if(bufferBitmap==null) {
            rec.send(Activity.RESULT_CANCELED, new Bundle());
            return;
        }

        if (pat<3 && pat>=0) patologia = pat;
        Log.d(TAG,"Patologia"+patologia);

        src = new Mat(bufferBitmap.getHeight(),bufferBitmap.getWidth(),CvType.CV_8U, new Scalar(4));
        dst = new Mat(bufferBitmap.getHeight(),bufferBitmap.getWidth(),CvType.CV_8U, new Scalar(4));

        Utils.bitmapToMat(bufferBitmap,src);
        Utils.bitmapToMat(bufferBitmap,dst);



        //Based on the pathology I choose the color conversion matrix
        for(i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                if (patologia == 0)
                    BT[i][j] = PT[i][j];
                else if (patologia == 1)
                    BT[i][j] = DT[i][j];
                else
                    BT[i][j] = TT[i][j];
            }
        }


        double[] rgb;
        for(i=0; i<src.height(); i++){
            for(j=0; j<src.width(); j++){

                rgb = src.get(i,j);

                //For every pixel in the image, extract channels
                r = rgb[0];
                g = rgb[1];
                b = rgb[2];
            //    a = rgb[3];

                //Convert from rgb to lms
                L = (17.8824 * r)   + (43.5161 * g)  + (4.11935 * b);
                M = (3.45565 * r)   + (27.1554 * g)  + (3.86714 * b);
                S = (0.0299566 * r) + (0.184309 * g) + (1.46709 * b);

                //Modify lms in order to simulate the pathology
                l = L*BT[0][0] + M*BT[0][1] + S*BT[0][2];
                m = L*BT[1][0] + M*BT[1][1] + S*BT[1][2];
                s = L*BT[2][0] + M*BT[2][1] + S*BT[2][2];

                //Convert from lms with pathology to rbg with pathology
                R = (0.0809444479 * l) + (-0.130504409 * m) + (0.116721066 * s);
                G = (-0.0102485335 * l) + (0.0540193266 * m) + (-0.113614708 * s);
                B = (-0.000365296938 * l) + (-0.00412161469 * m) + (0.693511405 * s);

                // Clamp values
                if(R<0)
                    R=0.0;
                else if(R>255)
                    R=255.0;

                if(G<0)
                    G=0.0;
                else if(G>255)
                    G=255.0;

                if(B<0)
                    B=0.0;
                else if(B>255)
                    B=255.0;

                /*(acr->imageData+acr->widthStep*i)[j*3] = (unsigned char)B;
                (acr->imageData+acr->widthStep*i)[j*3+1] = (unsigned char)G;
                (acr->imageData+acr->widthStep*i)[j*3+2] = (unsigned char)R;*/


                //Obtain error matrix E by subtracting rgb with pathology to rgb without pathology
                R = r - R;
                G = g - G;
                B = b - B;



                // Shift colors towards visible spectrum (apply error modifications)
                //Obtain Emod which is then added to the original value
                if(patologia==0){

                    RR = (0.0 * R) + (0.0 * G) + (0.0 * B);
                    GG = (0.7 * R) + (1.0 * G) + (0.0 * B);
                    BB = (0.7 * R) + (0.0 * G) + (1.0 * B);
				/*
				RR = (0.0 * R) + (0.0 * G) + (0.0 * B);
				GG = (g/r * R) + (1.0 * G) + (0.0 * B);
				BB = (b/r * R) + (0.0 * G) + (1.0 * B);
				*/

                } else if(patologia == 1) {


                    RR = (1.0 * R) + (0.5 * G) + (0.0 * B);
                    GG = (0.0 * R) + (0.0 * G) + (0.0 * B);
                    BB = (0.0 * R) + (0.5 * G) + (1.0 * B);

                }
                else {

                    RR = (1.0 * R) + (0.0 * G) + (0.7 * B);
                    GG = (0.0 * R) + (1.0 * G) + (0.7 * B);
                    BB = (0.0 * R) + (0.0 * G) + (0.0 * B);

                }


                //Add compensation to original values
                //Add Emod to rgb in order to create a daltonized version of the img
                R = RR + r;
                G = GG + g;
                B = BB + b;

                // Clamp values
                if(R<0)
                    R=0.0;
                else if(R>255)
                    R=255.0;

                if(G<0)
                    G=0.0;
                else if(G>255)
                    G=255.0;

                if(B<0)
                    B=0.0;
                else if(B>255)
                    B=255.0;

//                Log.d(TAG,dst.toString()+"| dst.width: "+dst.width()+"j:"+j+" | dst.height:"+dst.height()+"i:"+i+" |");
                dst.put(i,j, R, G, B,255.0);

/*                (dst->imageData+dst->widthStep*i)[j*3] = (unsigned char)B;
                (dst->imageData+dst->widthStep*i)[j*3+1] = (unsigned char)G;
                (dst->imageData+dst->widthStep*i)[j*3+2] = (unsigned char)R;*/

                // per controllare cosa vede il tricromatico anomalo
                /*L = (17.8824 * R)   + (43.5161 * G)  + (4.11935 * B);
                M = (3.45565 * R)   + (27.1554 * G)  + (3.86714 * B);
                S = (0.0299566 * R) + (0.184309 * G) + (1.46709 * B);

                l = L*BT[0][0] + M*BT[0][1] + S*BT[0][2];
                m = L*BT[1][0] + M*BT[1][1] + S*BT[1][2];
                s = L*BT[2][0] + M*BT[2][1] + S*BT[2][2];


                R = (0.0809444479 * l) + (-0.130504409 * m) + (0.116721066 * s);
                G = (-0.0102485335 * l) + (0.0540193266 * m) + (-0.113614708 * s);
                B = (-0.000365296938 * l) + (-0.00412161469 * m) + (0.693511405 * s);

                // Clamp values
                if(R<0)
                    R=0.0;
                else if(R>255)
                    R=255.0;

                if(G<0)
                    G=0.0;
                else if(G>255)
                    G=255.0;

                if(B<0)
                    B=0.0;
                else if(B>255)
                    B=255.0;

                (mid->imageData+mid->widthStep*i)[j*3] = (unsigned char)B;
                (mid->imageData+mid->widthStep*i)[j*3+1] = (unsigned char)G;
                (mid->imageData+mid->widthStep*i)[j*3+2] = (unsigned char)R;*/
            }
        }

        Bitmap destBitmap = Bitmap.createBitmap(bufferBitmap.getWidth(),bufferBitmap.getHeight(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(dst,destBitmap);



        newFile = fileHandler.getOutputMediaFile(FileHandler.MEDIA_TYPE_IMAGE);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(newFile);

            //Imgcodecs.imwrite(fileHandler.getUriFromFile(newFile).getPath(),m);
            destBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            //out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // To send a message to the Activity, create a pass a Bundle
        Bundle bundle = new Bundle();
        bundle.putString(Message.IMG_PATH, fileHandler.getUriFromFile(newFile).getPath());
        // Here we call send passing a resultCode and the bundle of extras
        rec.send(Activity.RESULT_OK, bundle);

    }


}
