package com.example.minechapapp;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
public class EditPerfil extends AppCompatActivity {
    private ImageView imgPerfil;
    private Button btnCambiarFoto, btnGuardar;
    private EditText etNombreUsuario;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String fotoActualBase64 = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_perfil);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        etNombreUsuario = findViewById(R.id.etNombreUsuario);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cargarDatosUsuario();
        btnCambiarFoto.setOnClickListener(v -> abrirGaleria());
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }
    private void cargarDatosUsuario() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombreUsuario = documentSnapshot.getString("nombre");
                        etNombreUsuario.setText(nombreUsuario);
                        fotoActualBase64 = documentSnapshot.getString("fotoBase64");
                        if (fotoActualBase64 != null && !fotoActualBase64.isEmpty()) {
                            Bitmap bitmap = convertirBase64ABitmap(fotoActualBase64);
                            imgPerfil.setImageBitmap(bitmap);
                        } else {
                            imgPerfil.setImageResource(R.drawable.default_profile_1);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditPerfil.this, "Error al cargar los datos", Toast.LENGTH_SHORT).show());
    }
    private void abrirGaleria() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgPerfil.setImageURI(imageUri);
        }
    }
    private void guardarCambios() {
        String nuevoNombre = etNombreUsuario.getText().toString().trim();
        String userId = mAuth.getCurrentUser().getUid();

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> actualizaciones = new HashMap<>();
        actualizaciones.put("nombre", nuevoNombre);

        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                String nuevaFotoBase64 = convertirBitmapABase64(bitmap);

                if (!nuevaFotoBase64.equals(fotoActualBase64)) {
                    actualizaciones.put("fotoBase64", nuevaFotoBase64);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        db.collection("usuarios").document(userId)
                .update(actualizaciones)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditPerfil.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditPerfil.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private Bitmap convertirBase64ABitmap(String base64) {
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private String convertirBitmapABase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageData = baos.toByteArray();
        return Base64.encodeToString(imageData, Base64.DEFAULT);
    }
}
