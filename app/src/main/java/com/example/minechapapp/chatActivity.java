package com.example.minechapapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import android.view.View;


import android.util.Log;



import java.util.ArrayList;

public class chatActivity extends AppCompatActivity {

    private TextView tvNombreUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar;
    private ImageView imgPreview;

    private ArrayList<String> listaMensajes = new ArrayList<>();
    private mensajeAdap mensajeAdapter;
    private ImageButton btnEmoji; // Declara el botón
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        imgPreview = findViewById(R.id.imgPreview);
        btnEmoji = findViewById(R.id.btnEmoji);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        imgPreview.setImageURI(imageUri);
                        imgPreview.setVisibility(View.VISIBLE); // Para ver imagen
                    }
                }
        );


        btnEmoji.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*"); // Solo imágenes
            imagePickerLauncher.launch(intent);
        });

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);

        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        tvNombreUsuario.setText(nombreUsuario);

        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        mensajeAdapter = new mensajeAdap(listaMensajes);
        recyclerMensajes.setAdapter(mensajeAdapter);

        btnEnviar.setOnClickListener(v -> {
            String mensaje = editMensaje.getText().toString().trim();

            if (!mensaje.isEmpty()) {
                listaMensajes.add(mensaje);
                mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);

                editMensaje.setText("");
            } else {
                Toast.makeText(this, "Escribe un mensaje primero", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
