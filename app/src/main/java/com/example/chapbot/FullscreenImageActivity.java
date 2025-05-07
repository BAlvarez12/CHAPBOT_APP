package com.example.chapbot;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullscreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Usamos el layout que ya modificaste (antes configuración)
        setContentView(R.layout.activity_configuration_screen);

        // Referenciamos el ImageView fullscreen
        ImageView imgFull = findViewById(R.id.imgFullScreen);

        // Cargamos la URL que vienen en el Intent
        String url = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (url != null) {
            Glide.with(this)
                    .load(url)
                    .into(imgFull);
        }

        // Al tocar la imagen, cerramos la Activity
        imgFull.setOnClickListener(v -> finish());
    }
}
