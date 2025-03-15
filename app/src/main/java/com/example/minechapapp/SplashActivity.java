package com.example.minechapapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2000; // 2 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Log para confirmar que se inicia la SplashActivity
        Log.d(TAG, "onCreate: Iniciando SplashActivity...");

        // (Opcional) Mensaje rápido para verificar visualmente
        Toast.makeText(this, "Splash iniciada", Toast.LENGTH_SHORT).show();

        // Usa un Handler con Looper.getMainLooper()
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, Login.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
