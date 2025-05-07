package com.example.chapbot;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NombreGrupoActivity extends AppCompatActivity {

    private EditText etGroupName;
    private Button btnConfirm;
    private ImageView imgGrupo;
    private TextView btnAgregarFoto;
    private Uri imageUri;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<String> participantes;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    imgGrupo.setImageURI(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupo_nombre);

        etGroupName = findViewById(R.id.etGroupName);
        btnConfirm = findViewById(R.id.btnConfirm);
        imgGrupo = findViewById(R.id.imgGrupo);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        participantes = getIntent().getStringArrayListExtra("participantes");
        if (participantes == null) participantes = new ArrayList<>();
        if (!participantes.contains(currentUserId)) participantes.add(0, currentUserId);

        btnAgregarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnConfirm.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (TextUtils.isEmpty(groupName)) {
                Toast.makeText(this, "Ingresa el nombre del grupo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (imageUri != null) {
                convertirImagenYCrearGrupo(groupName, participantes);
            } else {
                crearChatGrupal(groupName, participantes, null);
            }
        });
    }

    private void convertirImagenYCrearGrupo(String groupName, List<String> participantes) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // calidad reducida para menor peso
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            crearChatGrupal(groupName, participantes, encodedImage);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show();
            crearChatGrupal(groupName, participantes, null);
        }
    }

    private void crearChatGrupal(String groupName, List<String> participantes, String imagenBase64) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("tipo_chat", "97XeeFNzro7xurmKwKeh");
        chatData.put("nombre_grupo", groupName);
        chatData.put("participantes", participantes);
        chatData.put("fecha_creado", FieldValue.serverTimestamp());
        if (imagenBase64 != null) {
            chatData.put("foto_grupo_base64", imagenBase64);
        }

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Chat grupal creado", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, GrupoActivity.class);
                    intent.putExtra("chatId", documentReference.getId());
                    intent.putExtra("nombreGrupo", groupName);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al crear el grupo", Toast.LENGTH_SHORT).show();
                    Log.e("NombreGrupoActivity", "Error al guardar grupo", e);
                });
    }
}
