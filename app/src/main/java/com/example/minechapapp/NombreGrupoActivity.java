package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class NombreGrupoActivity extends AppCompatActivity {

    private EditText etGroupName;
    private Button btnConfirm;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<String> participantes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupo_nombre);

        etGroupName = findViewById(R.id.etGroupName);
        btnConfirm = findViewById(R.id.btnConfirm);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        participantes = getIntent().getStringArrayListExtra("participantes");
        if (participantes == null) {
            participantes = new ArrayList<>();
        }

        if (!participantes.contains(currentUserId)) {
            participantes.add(0, currentUserId);
        }

        Log.d("NombreGrupoActivity", "Participantes recibidos: " + participantes);

        btnConfirm.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (TextUtils.isEmpty(groupName)) {
                Toast.makeText(NombreGrupoActivity.this, "Ingresa el nombre del grupo", Toast.LENGTH_SHORT).show();
                return;
            }

            crearChatGrupal(groupName, participantes);
        });
    }

    private void crearChatGrupal(String groupName, List<String> participantes) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("tipo_chat", "grupal");
        chatData.put("nombre_grupo", groupName);
        chatData.put("participantes", participantes);
        chatData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(NombreGrupoActivity.this, "Chat grupal creado", Toast.LENGTH_SHORT).show();
                    Log.d("NombreGrupoActivity", "Chat grupal creado con ID: " + documentReference.getId());
                    Intent intent = new Intent(NombreGrupoActivity.this, GrupoActivity.class);
                    intent.putExtra("chatId", documentReference.getId());
                    intent.putExtra("nombreGrupo", groupName);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NombreGrupoActivity.this, "Error al crear el chat grupal", Toast.LENGTH_SHORT).show();
                    Log.e("NombreGrupoActivity", "Error al crear chat grupal", e);
                });
    }
}
