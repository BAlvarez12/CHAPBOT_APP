package com.example.minechapapp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 1000;
    private Handler handler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);
        Log.d(TAG, "onCreate: Iniciando SplashActivity...");

        crearCanalDeNotificacion();

        handler = new Handler(Looper.getMainLooper());
        splashRunnable = this::goToNextScreen;

        handler.postDelayed(splashRunnable, SPLASH_DURATION);
    }

    private void crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String canalId = "MINECHAP_CHANNEL";
            CharSequence nombre = "Notificaciones MineChap";
            String descripcion = "Canal para mensajes tipo chat";

            NotificationChannel canal = new NotificationChannel(
                    canalId,
                    nombre,
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.setDescription(descripcion);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(canal);

            Log.d(TAG, "Canal de notificación creado");
        }
    }

    private void goToNextScreen() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (handler != null && splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }
    }
}
