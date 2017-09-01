package com.so.sorrentix.dalty;


import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by sorrentix on 23/06/2016.
 */
public class DaltonizationHandler extends AsyncTask<String,Integer,Bitmap> {

    private static final String TAG = "Dalty - DH";

    static{ System.loadLibrary("opencv_java3"); }


    public static final int PATOLOGIA_PT = 0;
    public static final int PATOLOGIA_DT = 1;
    public static final int PATOLOGIA_TT = 2;

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
    private Activity activityContext;

    public DaltonizationHandler(MainActivity activity){
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

        src = new Mat();
        dst = new Mat();

        activityContext = activity;
    }


    public void daltonize(Bitmap bitmap, int pat){
            bufferBitmap = bitmap;
            if (pat<3 && pat>=0) patologia = pat;
            this.execute("Prova");
    }

    public Bitmap daltonizeAlgorithm(){


        Utils.bitmapToMat(bufferBitmap,src);
        Utils.bitmapToMat(bufferBitmap,dst);


        //In base alla patologia scelgo la matrice di conversione dei colori
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

                //Per ogni pixel dell'img estraggo i valori dei canali
                r = rgb[0];
                g = rgb[1];
                b = rgb[2];

                //Converto da rgb a lms
                L = (17.8824 * r)   + (43.5161 * g)  + (4.11935 * b);
                M = (3.45565 * r)   + (27.1554 * g)  + (3.86714 * b);
                S = (0.0299566 * r) + (0.184309 * g) + (1.46709 * b);

                //Modifico lms in modo da simulare la patologia corretta
                l = L*BT[0][0] + M*BT[0][1] + S*BT[0][2];
                m = L*BT[1][0] + M*BT[1][1] + S*BT[1][2];
                s = L*BT[2][0] + M*BT[2][1] + S*BT[2][2];

                //Converto da lms con patologia a rbg con patologia
                R = (0.0809444479 * l) + (-0.130504409 * m) + (0.116721066 * s);
                G = (-0.0102485335 * l) + (0.0540193266 * m) + (-0.113614708 * s);
                B = (-0.000365296938 * l) + (-0.00412161469 * m) + (0.693511405 * s);

                // Clamp values - verifico che rispettino i limiti dello standard
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


                //Sottraggo rgb con patologia a rgb senza patologia ottenendo una error matrix E
                R = r - R;
                G = g - G;
                B = b - B;



                // Shift colors towards visible spectrum (apply error modifications)
                //Calcolo  Emod che sarà poi aggiunta al valore originale
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
                //Aggiungo Emod a rgb per creare la versione daltonizzata dell'img
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

                Log.d(TAG,dst.toString()+"| dst.width: "+dst.width()+"j:"+j+" | dst.height:"+dst.height()+"i:"+i+" |");
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
            publishProgress(Math.round(i*100/src.height()));
        }

        Bitmap destBitmap = Bitmap.createBitmap(bufferBitmap);
        Utils.matToBitmap(dst,destBitmap);
        return destBitmap;

    }


    @Override
    protected Bitmap doInBackground(String... params) {
        return daltonizeAlgorithm();
    }

    @Override
    protected void onPreExecute() {
        if(activityContext instanceof MainActivity){
            ((MainActivity) activityContext).scene1TOscene2();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if(activityContext instanceof MainActivity){
            ((MainActivity) activityContext).increaseProgress(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        /*if(activityContext instanceof MainActivity){
            ((MainActivity) activityContext).scene2TOscene3(result);
        }*/
        //TODO salvare l'immagine mentre l'utente sta visualizzando quella a video
    }

}


