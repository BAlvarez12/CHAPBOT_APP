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

import java.util.ArrayList;
import java.util.List;
import com.example.minechapapp.adapters.MensajeAdapter;
import com.example.minechapapp.models.MensajeModel;

public class chatActivity extends AppCompatActivity {

    private TextView tvNombreUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar;
    private ImageView imgPreview;
    private ImageButton btnEmoji;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Enlazar vistas con el XML
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        imgPreview = findViewById(R.id.imgPreview);
        btnEmoji = findViewById(R.id.btnEmoji);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);

        // Obtener el nombre del usuario desde la intención
        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario != null) {
            tvNombreUsuario.setText(nombreUsuario);
        }

        // Inicializar la lista de mensajes y el adaptador
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes);

        // Configurar RecyclerView
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);

        // Configurar el selector de imágenes
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            imgPreview.setImageURI(imageUri);
                            imgPreview.setVisibility(View.VISIBLE); // Mostrar imagen seleccionada
                        }
                    }
                }
        );

        // Listener para el botón de selección de imágenes
        btnEmoji.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*"); // Solo imágenes
            imagePickerLauncher.launch(intent);
        });

        // Listener para el botón de enviar mensaje
        btnEnviar.setOnClickListener(v -> {
            String mensajeTexto = editMensaje.getText().toString().trim();

            if (!mensajeTexto.isEmpty()) {
                // Agregar el mensaje a la lista
                listaMensajes.add(new MensajeModel(mensajeTexto, true)); // true = enviado

                // Notificar al adaptador del cambio
                mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);

                // Asegurar que el RecyclerView se desplace al último mensaje
                recyclerMensajes.post(() -> recyclerMensajes.scrollToPosition(listaMensajes.size() - 1));

                // Limpiar el campo de entrada
                editMensaje.setText("");
            } else {
                Toast.makeText(this, "Escribe un mensaje primero", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
