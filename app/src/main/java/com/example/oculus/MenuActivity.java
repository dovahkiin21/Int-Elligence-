package com.example.oculus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    Button objectDetecBtn,textReconBtn,voiceComBtn,settingsBtn;
    Vibrator vibrator;              //for haptic feedback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        objectDetecBtn=findViewById(R.id.objectDetc);
        textReconBtn=findViewById(R.id.textRecon);
        voiceComBtn=findViewById(R.id.voiceCom);
        settingsBtn=findViewById(R.id.settings);

        objectDetecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               vibrator.vibrate(70);          //haptic feedback
                Intent objDetector = new Intent(MenuActivity.this,ObjectDetection.class);
                //Intents ObjectDetection Activity
                startActivity(objDetector);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }

        });
        textReconBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(70);
                Intent txtRecognition = new Intent(MenuActivity.this,OcrCaptureActivity.class);
                //Intents to Te OcrCaptureActivity
                startActivity(txtRecognition);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }

        });
        voiceComBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(150);
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(150);
            }
        });

    }
}
