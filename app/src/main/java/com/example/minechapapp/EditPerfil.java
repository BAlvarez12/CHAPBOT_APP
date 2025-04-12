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

public class EditPerfil extends AppCompatActivity {

    private ImageView imgPerfil;
    private Button btnCambiarFoto, btnGuardar;
    private EditText etNombreUsuario;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_perfil);

        // Inicializar vistas
        imgPerfil = findViewById(R.id.imgPerfil);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        etNombreUsuario = findViewById(R.id.etNombreUsuario);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Cargar datos del usuario actual
        cargarDatosUsuario();

        // Botón para cambiar la foto de perfil
        btnCambiarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirGaleria();
            }
        });

        // Botón para guardar cambios
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCambios();
            }
        });
    }

    // Método para cargar los datos del usuario actual
    private void cargarDatosUsuario() {
        String userId = mAuth.getCurrentUser().getUid();

        // Obtener los datos del usuario desde Firestore
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Obtener el nombre de usuario
                            String nombreUsuario = documentSnapshot.getString("nombre");
                            etNombreUsuario.setText(nombreUsuario);

                            // Obtener la foto de perfil en Base64
                            String fotoBase64 = documentSnapshot.getString("fotoBase64");

                            // Mostrar la foto de perfil
                            if (fotoBase64 != null && !fotoBase64.isEmpty()) {
                                Bitmap bitmap = convertirBase64ABitmap(fotoBase64);
                                imgPerfil.setImageBitmap(bitmap);
                            } else {
                                // Esto no debería ocurrir, ya que el usuario debe subir una foto al registrarse
                                imgPerfil.setImageResource(R.drawable.default_profile_1);
                                Toast.makeText(EditPerfil.this, "Error: No se encontró la foto de perfil", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditPerfil.this, "Error al cargar los datos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Método para convertir Base64 a Bitmap
    private Bitmap convertirBase64ABitmap(String base64) {
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Método para abrir la galería y seleccionar una imagen
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
            imgPerfil.setImageURI(imageUri); // Mostrar la imagen seleccionada
        }
    }

    // Método para guardar los cambios (nombre y foto de perfil)
    private void guardarCambios() {
        String nuevoNombre = etNombreUsuario.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar el nuevo nombre en Firestore
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("usuarios").document(userId)
                .update("nombre", nuevoNombre)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(EditPerfil.this, "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditPerfil.this, "Error al actualizar el nombre: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Subir la nueva foto de perfil (si se seleccionó una imagen)
        if (imageUri != null) {
            try {
                // Convertir la imagen a Base64
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                String fotoBase64 = convertirBitmapABase64(bitmap);

                // Guardar la foto en Firestore
                db.collection("usuarios").document(userId)
                        .update("fotoBase64", fotoBase64)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(EditPerfil.this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(EditPerfil.this, "Error al actualizar la foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Método para convertir Bitmap a Base64
    private String convertirBitmapABase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Comprimir al 70% de calidad
        byte[] imageData = baos.toByteArray();
        return Base64.encodeToString(imageData, Base64.DEFAULT);
    }
}