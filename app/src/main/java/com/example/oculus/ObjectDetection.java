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
import android.widget.Toast;

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

    private CameraView cameraView;                                                                  //for camera
    private Button detectBtn;                                                                       //button to detect
    private AlertDialog waitingDialogue;                                                            //to display waiting dialogue
    private TextToSpeech TTS;                                                                       //variable to implement text to speech
    private TextView display;                                                                       //to display

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();                                                                         //to start camera
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();                                                                          //to stop camera (when the activity gets paused)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detection);

        detectBtn = findViewById(R.id.button_detect);
        display = findViewById(R.id.display_labels);
        cameraView = findViewById(R.id.cameraView);

        //to set waiting dialog
        waitingDialogue = new SpotsDialog.Builder().setContext(this).setMessage("Please Wait")
                .setCancelable(false).build();

        //For Camera
        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                waitingDialogue.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.getWidth() , cameraView.getHeight() , false);
                cameraView.stop();                                                                                              //to stop camera after the image gets captured

                runDetector(bitmap);

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        //Text to speech
        TTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    int result = TTS.setLanguage(Locale.ENGLISH);
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

        //To start detecting object in front of camera
        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display.setText(null);
                cameraView.start();
                cameraView.captureImage();
            }
        });

    }

    //function to give voice output
    private void voiceOutput() {

        String text = display.getText().toString();
        TTS.setPitch(1);
        TTS.setSpeechRate(1);
        TTS.speak(text,TextToSpeech.QUEUE_FLUSH,null);

    }

    @Override
    protected void onDestroy() {
        if(TTS !=null){
            TTS.stop();
            TTS.shutdown();
        }
        super.onDestroy();
    }

    private void runDetector(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);                         //To create a FirebaseVisionImage object from a Bitmap object


        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()                           //To get instance of FirebaseVisionImageLabeler
                .getOnDeviceImageLabeler();

        labeler.processImage(image)                                                                 //passing image to processImage() method
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {     //if it runs successfully the following method is called
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        // Task completed successfully
                        // ...
                        try {
                            processDataResult(labels);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {                                     //if it fails following message is displayed
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                        Toast.makeText(ObjectDetection.this, "No lable found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processDataResult(List<FirebaseVisionImageLabel> labels) throws InterruptedException {                         //to display labels

        for (FirebaseVisionImageLabel label: labels) {
            float confidence = label.getConfidence();
            if (confidence >= 0.68){
                String text = label.getText();
                String entityId = label.getEntityId();
                display.append(text + " " + "or" + " ");
            }
        }

        String string1 = display.getText().toString();                                              //to remove last "or"
        String string2 = string1.substring(0,string1.length()-1);
        string1 = string2.substring(0,string2.length()-1);
        string2 = string1.substring(0,string1.length()-1);
        display.setText(string2);

        if(waitingDialogue.isShowing())                                                             //to remove waiting dialogue
            waitingDialogue.dismiss();

        voiceOutput();                                                                              //to give voice output of detected labels

    }
}
