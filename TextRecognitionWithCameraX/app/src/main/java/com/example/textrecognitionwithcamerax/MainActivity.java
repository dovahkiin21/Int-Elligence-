package com.example.textrecognitionwithcamerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button detectBtn,speak;
    private TextView display;
    private TextureView textureView;
    private Bitmap imageBitmap;
    private TextToSpeech mTTS;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"};
    Random rand = new Random();
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        speak = findViewById(R.id.speak);
        detectBtn = findViewById(R.id.detect_image);
        textureView = findViewById(R.id.view_findrer);
        display = findViewById(R.id.text_display);

        if(allPermissionsGranted()){
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);
        }

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);
                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.d("TTS","LANGUAGE NOT SUPPORTED");
                    }else {
                        Log.d("TTS","Done!");
                    }
                } else {
                    Log.d("TTS","Initialization Failed");
                }
            }
        });

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectTextFromImg();
            }
        });
    }

    private void startCamera() {
        CameraX.unbindAll();
        Rational aspectRatio = new Rational(textureView.getWidth(),textureView.getHeight());
        Size screen = new Size(textureView.getWidth(),textureView.getHeight());

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView);
                textureView.setSurfaceTexture(output.getSurfaceTexture()); //for aspect ratio and rscreen resolution
                updateTransform();
            }
        });

///to store in external
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY).
                setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setFlashMode(FlashMode.OFF).build();   ///setflashMode()  this turn s on flash
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);
        findViewById(R.id.capture_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                file = null;
                try {
                    file = File.createTempFile("Image"+ new Random().nextInt(20) ,".jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                file.deleteOnExit();
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = "Pic Captured at" + file.getAbsolutePath();
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic Captured failed" + message;
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
                imageBitmap = (Bitmap) BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        });

        CameraX.bindToLifecycle(this,preview,imgCap);///here we are binding preview and imagecaptured to th lifecycle of the activity

    }


    private void updateTransform() {  //to update preview when phone is moved
        Matrix mx = new Matrix();
        float h = textureView.getMeasuredHeight();
        float w = textureView.getMeasuredWidth()  ;

        float cx = w / 2f;
        float cy = h / 2f;

        int rotationDgr ;
        int rotation = (int) textureView.getRotation() ;
        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }
        mx.postRotate((float)rotationDgr,cx,cy);
        textureView.setTransform(mx);
    }


    private boolean allPermissionsGranted() {   //to check if all permissions are granted
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


    private void speak(){
        String text = display.getText().toString();
        mTTS.setPitch(1);
        mTTS.setSpeechRate(1);
        mTTS.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }


    private void detectTextFromImg() {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        Task<FirebaseVisionText> result = detector.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayTextMessage(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Error",e.getMessage());
            }
        });
    }


    private void displayTextMessage(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blockList=firebaseVisionText.getTextBlocks();
        if (blockList.size()==0){
            Toast.makeText(this, "No Text Detected", Toast.LENGTH_SHORT).show();
        }
        else{
            for(FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()){
                String text = block.getText();
                display.setText(text);
                file.delete();
            }
        }

    }

}
