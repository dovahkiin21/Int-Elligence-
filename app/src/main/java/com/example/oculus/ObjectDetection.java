package com.example.oculus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import java.util.List;
import java.util.Locale;

import dmax.dialog.SpotsDialog;

public class ObjectDetection extends AppCompatActivity {

    private CameraView cameraView;
    private Button detectBtn;
    private AlertDialog waitingDialog;
    private TextToSpeech mTTS;
    private TextView display;

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
        setContentView(R.layout.activity_object_detection);
        detectBtn = findViewById(R.id.button_detect);
        display = findViewById(R.id.display_labels);
        cameraView = findViewById(R.id.cameraView);
        //to set waiting dialog
        waitingDialog = new SpotsDialog.Builder().setContext(this).setMessage("Please Wait")
                .setCancelable(false).build();
        ///text to speech
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

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display.setText(null);
                cameraView.start();
                cameraView.toggleFlash();
                cameraView.captureImage();
            }
        });

        //Event Camera View - Initializing Camera
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
                detectObjectFromImg(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

    }

    private void detectObjectFromImg(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionOnDeviceImageLabelerOptions options = new FirebaseVisionOnDeviceImageLabelerOptions.Builder()
         .setConfidenceThreshold(0.60f)
         .build();
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
     .getOnDeviceImageLabeler(options);
        labeler.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        try {
                            dispalyLabels(labels);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("ERROR","CAN'T LABEL");
                    }
                });


    }

    private void dispalyLabels(List<FirebaseVisionImageLabel> labels) throws InterruptedException {
        for (FirebaseVisionImageLabel label: labels) {
            String text = label.getText();
            display.setText(text);
            speak();
            Thread.sleep(2000);
        }
        waitingDialog.dismiss();
    }

    ;
     ///text to speech options
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
