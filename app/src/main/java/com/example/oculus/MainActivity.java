package com.example.oculus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle; // hehe
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainMenu = new Intent(MainActivity.this,MenuActivity.class);
                startActivity(mainMenu);
                finish();
            }
        },SPLASH_TIME_OUT);
    }
}
