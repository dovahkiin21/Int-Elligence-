package com.example.oculus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;//for another widget

public class MenuActivity extends AppCompatActivity {

    Button objectDetecBtn,textReconBtn,voiceComBtn,settingsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        objectDetecBtn=findViewById(R.id.objectDetc);
        textReconBtn=findViewById(R.id.textRecon);
        voiceComBtn=findViewById(R.id.voiceCom);
        settingsBtn=findViewById(R.id.settings);

        objectDetecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent objDetector = new Intent(MenuActivity.this,ObjectDetection.class);
                startActivity(objDetector);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }

        });
        textReconBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent txtRecognition = new Intent(MenuActivity.this,OcrCaptureActivity.class);
                startActivity(txtRecognition);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }

        });
        voiceComBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }
}
