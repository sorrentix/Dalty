package com.so.sorrentix.dalty;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.so.sorrentix.dalty.util.ImageResizer;
import com.so.sorrentix.dalty.util.Message;


public class MainActivity extends AppCompatActivity implements MyResultReceiver.Receiver, MediaScannerConnection.OnScanCompletedListener {

    private static final String TAG = "Dalty - MainActivity";


    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int RESULT_LOAD_IMG = 101;
    private static final int MAX_IMG_WIDTH = 1280;
    private static final int MAX_IMG_HEIGHT = 1280;
    private static final int REQUEST_WRITE_READ_EXTERNAL_STORAGE = 1;
    //attributes to handle the file persistence
    private Uri fileUri;
    private FileHandler fileHandler;
    private String imgDecodableString;
    private String bufferedPath;

    //attributes to handle the views
    private FrameLayout sceneRoot;
    private View scene1;
    private View scene2;
    private View scene3;
    private ProgressBar mProgressBar;
    private ImageView imageView;

    private int pathologySelected;

    //receiver to handle communication with intentservice 
    public MyResultReceiver mReceiver;

    //Lighter version of the state pattern implemented
    private static final int STATE1 = 1001;
    private static final int STATE2 = 1002;
    private static final int STATE3 = 1003;
    private int appState = STATE1;

    //flags to handle the app flow
    private boolean flagPause;
    private boolean flagResume;
    private boolean flagResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        checkPermission();


        sceneRoot = (FrameLayout) findViewById(R.id.scene_root);
        scene1 = getLayoutInflater().inflate(R.layout.scene1, sceneRoot);


        pathologySelected = DaltonizeService.PATHOLOGY_PT;
        Log.e(TAG, "- onCreate - pathologySelected" + pathologySelected);
        populateSpinner();

        fileHandler = new FileHandler();
        mReceiver = new MyResultReceiver(new Handler());
        mReceiver.setReceiver(this);

        Log.d(TAG, "onCreate - flagPause:" + flagPause + " flagResume:" + flagResume + " flagResult:" + flagResult);
    }


    //Callback for the button CAMERA
    public void image_from_camera(View view) {
        Log.v(TAG, "image_from_camera - clicked");

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = fileHandler.getOutputMediaFileUri(FileHandler.MEDIA_TYPE_IMAGE); // create a file to save the image
        Log.e(TAG, "" + fileUri);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }


    //Callback for the button GALLERY
    public void image_from_gallery(View view) {
        Log.v(TAG, "image_from_gallery - clicked");

        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                Log.d(TAG, "camera result ok" + fileUri);
                // Image captured and saved to fileUri specified in the Intent
                if (fileUri == null) {
                    Toast.makeText(this, "There was a problem while saving the image, please try again", Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(this, "Image saved to:\n" + fileUri, Toast.LENGTH_LONG).show();
                launchDaltonizeService(fileUri.getPath());
                MediaScannerConnection.scanFile(this, new String[]{fileUri.getPath()}, null, this);

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                Log.d(TAG, "User cancelled the image capture");
            } else {
                // Image capture failed, advise user
                Toast.makeText(this, "Image capture failed, try again", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == RESULT_LOAD_IMG) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "gallery result ok");
                try {
                    if (data != null) {
                        fileUri = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        // Get the cursor
                        Cursor cursor = getContentResolver().query(fileUri,
                                filePathColumn, null, null, null);
                        // Move to first row
                        if (cursor != null) {
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            imgDecodableString = cursor.getString(columnIndex);
                            cursor.close();
                        }

                    } else {
                        Log.d(TAG, "cursor null");
                        Toast.makeText(this, "Something went wrong ", Toast.LENGTH_LONG)
                                .show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    Toast.makeText(this, "Something went wrong  ", Toast.LENGTH_LONG)
                            .show();
                }

                launchDaltonizeService(imgDecodableString);

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image selection
                Log.d(TAG, "User cancelled the gallery image selection");
            } else {
                // Image selection failed, advise user
                Toast.makeText(this, "Image selection failed, try again", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void scene1TOscene2() {
        Log.v(TAG, "scene1 to scene2");
        appState = STATE2;
        sceneRoot.removeAllViews();
        scene2 = getLayoutInflater().inflate(R.layout.scene2, sceneRoot);
        mProgressBar = (ProgressBar) findViewById(R.id.progres_bar);
    }

    public void increaseProgress(Integer x) {
        mProgressBar.setProgress(x);
    }


    public void scene2TOscene3(String path) {
        Log.v(TAG, "scene2 to scene3");
        appState = STATE3;
        sceneRoot.removeAllViews();
        scene3 = getLayoutInflater().inflate(R.layout.scene3, sceneRoot);
        MediaScannerConnection.scanFile(this, new String[]{path}, null, this);

    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        final String p = path;
        if(appState == STATE3) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //stuff that updates ui
                    imageView = (ImageView) findViewById(R.id.bitmap_preview);
                    imageView.setImageBitmap(ImageResizer.decodeSampledBitmapFromFile(p, MAX_IMG_WIDTH, MAX_IMG_HEIGHT, null));
                }
            });
        }
    }


    public void scene3TOscene1() {
        Log.v(TAG, "scene3 to scene1");
        appState = STATE1;
        sceneRoot.removeAllViews();
        scene1 = getLayoutInflater().inflate(R.layout.scene1, sceneRoot);
        populateSpinner();
    }


    public void back_button_action(View v) {
        Log.v(TAG, "back_button_action - clicked");
        scene3TOscene1();
    }


    @Override
    public void onBackPressed() {
        if (appState == STATE3) {
            scene3TOscene1();
        } else super.onBackPressed();
    }


    private void populateSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.patologia_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinner_values, android.R.layout.simple_spinner_item);
        // Apply the adapter to the spinner
        if (spinner != null) {
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.e(TAG, "- onItemSelected - position: " + position + " id:" + id);
                    pathologySelected = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    pathologySelected = DaltonizeService.PATHOLOGY_PT;
                }
            });
        }
    }


    public void launchDaltonizeService(String path) {
        // Construct our Intent specifying the Service
        scene1TOscene2();

        //Had to do this way due to a problem with R.java
        Intent i = new Intent(this, DaltonizeService.class);
        // Add extras to the bundle
        //TODO MODIFICARE I CONTENUTI CON DELLE COSTANTI
        Log.e(TAG, "- launchDaltonizeService - pathologySelected: " + pathologySelected);
        i.putExtra(Message.RECEIVER_TAG, mReceiver);
        i.putExtra(Message.IMG_PATH, path);
        i.putExtra(Message.PATHOLOGY, pathologySelected);
        i.putExtra(Message.NOTI_ICON, R.drawable.ic_stat_daltyicon);
        // Start the service
        startService(i);
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "entered in onReceiveResult");
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "onReceiveResult - flagUpdate:" + flagPause + " flagResume:" + flagResume + " flagResult:" + flagResult);

            String newImagePath = resultData.getString(Message.IMG_PATH);
            flagResult = true;
            if (flagPause)
                bufferedPath = newImagePath;
            else {
                flagResult = false;
                scene2TOscene3(newImagePath);
            }
        } else if (!flagPause) {
            Toast.makeText(this, "Error while processing the image, please try again", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mReceiver.setReceiver(this);
        flagResume = true;
        flagPause = false;
        if (flagResult) {
            scene2TOscene3(bufferedPath);
            flagResult = false;
        }
        Log.d(TAG, "onResume - flagPause:" + flagPause + " flagResume:" + flagResume + " flagResult:" + flagResult);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mReceiver.setReceiver(null);
        flagPause = true;
        flagResume = false;
        Log.d(TAG, "onPause - flagPause:" + flagPause + " flagResume:" + flagResume + " flagResult:" + flagResult);

    }


    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_WRITE_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    checkPermission();
                }
        }
    }

}