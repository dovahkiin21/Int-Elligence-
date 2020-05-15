package com.example.textrecognitionwithgraphicoverlay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import java.util.List;
import java.util.Locale;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    AlertDialog waitingDialog;
    private TextToSpeech mTTS;
    Button btnCapture,btnSpeak;
    TextView display;

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init dialog

        waitingDialog = new SpotsDialog.Builder().setCancelable(false).setMessage("Just a moment...").setContext(this).build();
        display = findViewById(R.id.display);
        cameraView = findViewById(R.id.camera_view);
        btnCapture = findViewById(R.id.btn_capture);
        btnSpeak = findViewById(R.id.btn_speak);

        ///for text to speech
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

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display.setText(null);
                cameraView.start();
                cameraView.toggleFlash();
                cameraView.captureImage();
            }
        });

        //Event Camera View
        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {


            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                //show dialog
                waitingDialog.show();
                //Process Image
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getMeasuredHeight(),false);
                cameraView.stop();
                detectTextFromImg(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {


            }
        });

    }

    private void detectTextFromImg(Bitmap bitmap) {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        Task<FirebaseVisionText> result = detector.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayTextResult(firebaseVisionText);
                Log.d("ERROR","NONE");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("ERROR",e.getMessage());
            }
        });

    }

    private void displayTextResult(FirebaseVisionText firebaseVisionText) {

        List<FirebaseVisionText.TextBlock> blockList=firebaseVisionText.getTextBlocks();
        if (blockList.size()==0){
            Log.d("TEXT","DETECTED");
            Toast.makeText(this, "No Text Detected", Toast.LENGTH_SHORT).show();
            display.setText("Oops!,No Text Detected!");
        }
        else{
            for(FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()){
                String text = block.getText();
                display.setText(text);

            }
        }
        waitingDialog.dismiss();
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

}
