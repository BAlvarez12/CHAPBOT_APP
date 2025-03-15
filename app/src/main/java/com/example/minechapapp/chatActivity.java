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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class chatActivity extends AppCompatActivity {

    private TextView tvNombreUsuario;
    private TextView tvEstadoUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar;
    private ImageView imgPreview;
    private ImageButton btnEmoji;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;
    private String receiverId;

    private String chatId;
    private String usuarioA;
    private String usuarioB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEstadoUsuario = findViewById(R.id.tvEstadoUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        imgPreview = findViewById(R.id.imgPreview);
        btnEmoji = findViewById(R.id.btnEmoji);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        receiverId = getIntent().getStringExtra("USER_ID");

        if (nombreUsuario != null) {
            tvNombreUsuario.setText(nombreUsuario);
        } else {
            tvNombreUsuario.setText("Chat");
        }
        if (currentUserId.compareTo(receiverId) < 0) {
            usuarioA = currentUserId;
            usuarioB = receiverId;
        } else {
            usuarioA = receiverId;
            usuarioB = currentUserId;
        }

        chatId = generarChatId(usuarioA, usuarioB);

        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes);
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);

        db = FirebaseFirestore.getInstance();

        loadMessages();
        loadLastMessageStatus();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            imgPreview.setImageURI(imageUri);
                            imgPreview.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        btnEmoji.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*"); // Solo imágenes
            imagePickerLauncher.launch(intent);
        });

        btnEnviar.setOnClickListener(v -> {
            String mensajeTexto = editMensaje.getText().toString().trim();
            if (!mensajeTexto.isEmpty()) {
                listaMensajes.add(new MensajeModel(mensajeTexto, true));
                mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);

                checkOrCreateChatAndSendMessage(mensajeTexto);

                editMensaje.setText("");
            } else {
                Toast.makeText(chatActivity.this, "Escribe un mensaje primero", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkOrCreateChatAndSendMessage(String mensajeTexto) {
        DocumentReference chatRef = db.collection("chats").document(chatId);
        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("tipo_chat", "individual");
                chatData.put("usuario_a", usuarioA);
                chatData.put("usuario_b", usuarioB);
                chatData.put("fecha_creado", FieldValue.serverTimestamp());

                chatRef.set(chatData)
                        .addOnSuccessListener(aVoid -> {

                            guardarMensajeEnNotificacion(mensajeTexto);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(chatActivity.this, "Error al crear el chat", Toast.LENGTH_SHORT).show();
                        });
            } else {
                guardarMensajeEnNotificacion(mensajeTexto);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(chatActivity.this, "Error al consultar el chat", Toast.LENGTH_SHORT).show();
        });
    }

    private void guardarMensajeEnNotificacion(String mensajeTexto) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("chat_id", chatId);
        messageData.put("mensaje", mensajeTexto);
        messageData.put("usuario_id", currentUserId);
        messageData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(messageData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(chatActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
                    loadLastMessageStatus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(chatActivity.this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (e.getMessage() != null && e.getMessage().contains("FAILED_PRECONDITION")) {
                            Toast.makeText(chatActivity.this,
                                    "Se requiere un índice para 'chat_id' y 'fecha_creado'. " +
                                            "Crea un índice compuesto en la consola de Firebase.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(chatActivity.this, "Error al cargar mensajes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    listaMensajes.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String mensaje = doc.getString("mensaje");
                            String usuarioId = doc.getString("usuario_id");
                            boolean esEnviado = (usuarioId != null && usuarioId.equals(currentUserId));
                            listaMensajes.add(new MensajeModel(mensaje, esEnviado));
                        }
                        mensajeAdapter.notifyDataSetChanged();
                        recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                    } else {
                        mensajeAdapter.notifyDataSetChanged();
                        Toast.makeText(chatActivity.this, "No hay mensajes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLastMessageStatus() {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String userMessageId = doc.getString("usuario_id");
                        if (userMessageId != null && userMessageId.equals(currentUserId)) {
                            tvEstadoUsuario.setText("Enviado por ti");
                        } else {
                            tvEstadoUsuario.setText("Recibido");
                        }
                    } else {
                        tvEstadoUsuario.setText("Sin mensajes");
                    }
                })
                .addOnFailureListener(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("FAILED_PRECONDITION")) {
                        tvEstadoUsuario.setText("Se requiere índice (chat_id, fecha_creado)");
                    } else {
                        tvEstadoUsuario.setText("Error al cargar estado");
                    }
                });
    }

    private String generarChatId(String id1, String id2) {
        return id1 + "_" + id2;
    }
}
