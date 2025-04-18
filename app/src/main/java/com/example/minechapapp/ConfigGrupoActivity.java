package com.example.minechapapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ConfigGrupoActivity extends AppCompatActivity {

    private ImageView imgPerfil;
    private EditText etNombreUsuario;
    private Button btnCambiarFoto, btnGuardar;

    private String chatId;
    private FirebaseFirestore db;
    private Bitmap nuevaFoto;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        nuevaFoto = BitmapFactory.decodeStream(inputStream);
                        imgPerfil.setImageBitmap(nuevaFoto);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_grupo);

        db = FirebaseFirestore.getInstance();

        chatId = getIntent().getStringExtra("chatId");
        imgPerfil = findViewById(R.id.imgPerfil);
        etNombreUsuario = findViewById(R.id.etNombreUsuario);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);

        cargarDatosGrupo();

        btnCambiarFoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosGrupo() {
        if (chatId == null) return;
        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String nombre = doc.getString("nombre_grupo");
                String base64 = doc.getString("foto_grupo_base64");

                if (nombre != null) etNombreUsuario.setText(nombre);
                if (base64 != null && !base64.isEmpty()) {
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imgPerfil.setImageBitmap(bitmap);
                }
            }
        });
    }

    private void guardarCambios() {
        if (chatId == null) return;

        String nuevoNombre = etNombreUsuario.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del grupo", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("chats").document(chatId)
                .update("nombre_grupo", nuevoNombre)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show();

                    // Retornar a GrupoActivity con el nuevo nombre
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("nuevo_nombre", nuevoNombre);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });

        if (nuevaFoto != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            nuevaFoto.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            db.collection("chats").document(chatId)
                    .update("foto_grupo_base64", base64)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
