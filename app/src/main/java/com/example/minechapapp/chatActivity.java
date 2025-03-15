package com.example.minechapapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.minechapapp.adapters.MensajeAdapter;
import com.example.minechapapp.models.MensajeModel;
import java.util.ArrayList;
import java.util.List;

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

        // Enlazar vistas definidas en el XML
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        imgPreview = findViewById(R.id.imgPreview);
        btnEmoji = findViewById(R.id.btnEmoji);

        // Obtener el nombre del usuario enviado desde UsuariosActivos
        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        if (nombreUsuario != null) {
            tvNombreUsuario.setText(nombreUsuario);
        } else {
            tvNombreUsuario.setText("Chat");
        }

        // Inicializar lista y adaptador para los mensajes
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes);
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);

        // Configurar el selector de imágenes con ActivityResultLauncher
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

        // Listener para el botón de selección de imágenes (emoji)
        btnEmoji.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*"); // Solo imágenes
            imagePickerLauncher.launch(intent);
        });

        // Listener para el botón de enviar mensaje
        btnEnviar.setOnClickListener(v -> {
            String mensajeTexto = editMensaje.getText().toString().trim();

            if (!mensajeTexto.isEmpty()) {
                // Agregar el mensaje a la lista y notificar el cambio al adaptador
                listaMensajes.add(new MensajeModel(mensajeTexto, true)); // true indica mensaje enviado
                mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);

                // Desplazar el RecyclerView al último mensaje
                recyclerMensajes.post(() -> recyclerMensajes.scrollToPosition(listaMensajes.size() - 1));

                // Limpiar el campo de texto
                editMensaje.setText("");
            } else {
                Toast.makeText(this, "Escribe un mensaje primero", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
