package com.example.textrecogniionm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStorageState;

public class MainActivity extends AppCompatActivity {
    private Button detectBtn,speak;
    private TextView display;
//    private ImageView imagedis;
    private TextureView textureView;
    private Bitmap imageBitmap;
    private TextToSpeech mTTS;
    static final int REQUEST_IMAGE_CAPTURE = 1;    //from the android dev website
    private int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"};
    String flash = "OFF";
    File file;
    Random rand = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        imagedis = findViewById(R.id.image);
        speak = findViewById(R.id.speak);
        detectBtn = findViewById(R.id.detect_image);
        textureView = findViewById(R.id.view_findrer);
        display = findViewById(R.id.text_display);
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

        getSupportActionBar().hide();
        textureView = (TextureView) findViewById(R.id.view_findrer);
        if(allPermissionsGranted()){
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);
        }

//        captureBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//
//            }
//        });

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                detectTextFromImg();
                file.delete();
            }

        });

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

    }


    private void speak(){
        String text = display.getText().toString();
        mTTS.setPitch(1);
        mTTS.setSpeechRate(1);
        mTTS.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }

    @Override
    protected void onDestroy() {
        if(mTTS !=null){
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();
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

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).
                setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setFlashMode(FlashMode.valueOf(flash)).build();   ///setflashMode()  this controls flash
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);
        findViewById(R.id.capture_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                file = null;
//                File file1 = new File()
//                file = new File("/sdcard/Images/test_image.jpg");   ///creating an img file at a random location
                //                file.deleteOnExit();
//                file = new File(getExternalStorageDirectory(), "Image"+ rand.nextInt(10) +".jpg");  /////////////////to save in sd card external
                file = new File(getFilesDir(), "Image"+ rand.nextInt(10) +".jpg");
//                imgCap.takePicture(new ImageCapture.OnImageCapturedListener() {
//                    @Override
//                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
////                        super.onCaptureSuccess(image, rotationDegrees);
//                        imagedis.setImageBitmap(toBitmap(image.getImage()));
//
//                    }
//                });
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

            }
        });

        CameraX.bindToLifecycle(this,preview,imgCap);///here we are binding preview and imagecaptured to th lifecycle of the activity

    }
    private void detectTextFromImg() {    ////firebase method to get text from bitmap image, refer documentation for reference
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

    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

}






