package com.example.objectdetection2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.objectdetection2.Helper.InternetCheck;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView cv1;
    Button bt1,bt2;
    AlertDialog waitingDilogue ;
    private TextToSpeech TTS;
    ImageButton ib;
    private static final int speechInput =1000;
    String str;
    TextView tv;


    @Override
    protected void onResume() {
        super.onResume();
        cv1.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cv1.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cv1 = findViewById(R.id.cv);
        bt1 = findViewById(R.id.bt);
        bt2 = findViewById(R.id.bt3);
        ib = findViewById(R.id.ib1);
        tv = findViewById(R.id.tv);

        waitingDilogue = new SpotsDialog.Builder().setContext(this).setMessage("Please Wait")
                .setCancelable(false).build();

        cv1.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                waitingDilogue.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, cv1.getWidth() , cv1.getHeight() , false);
                cv1.stop();

                runDetector(bitmap);

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

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

       /* if(check())
        {
            cv1.start();
            cv1.captureImage();
        }*/

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cv1.start();
                cv1.captureImage();
            }
        });

        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                speak();

            }
        });

        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                voiceOutput();

            }
        });
    }

    private void voiceOutput() {

        String text = tv.getText().toString();
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

    private void speak() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL ,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE , Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT , "Hi speak something");

        try {

            startActivityForResult(intent , speechInput);

        } catch (Exception e){

            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case speechInput :
                if (requestCode == RESULT_OK && null != data){
                    //grt text array from voice input
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    tv.setText(result.get(0));
                    str = tv.getText().toString();
                }
                break;
        }
    }

    public boolean check(){
        if(str.toLowerCase().indexOf("detect") != -1){
            Log.d("Hi",str);
            return true;
        }

        else
            return false;
    }

    private void runDetector(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);


        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler();

        labeler.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        // Task completed successfully
                        // ...
                        processDataResult(labels);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });


    }

    private void processDataResult(List<FirebaseVisionImageLabel> labels) {
        for (FirebaseVisionImageLabel l : labels){

            Toast.makeText(this, "Result" + l.getText(), Toast.LENGTH_SHORT).show();
            tv.setText(l.getText());
        }
        if(waitingDilogue.isShowing())
        waitingDilogue.dismiss();
    }
}
