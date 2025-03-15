package com.example.minechapapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class chatActivity extends AppCompatActivity {

    private static final String TIPO_CHAT_INDIVIDUAL_ID = "NCm3QCIsKw8MjjHycvm5";

    private TextView tvNombreUsuario, tvEstadoUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji;
    private ImageView imgPreview;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;

    private FirebaseFirestore db;
    private String currentUserId, receiverId, usuarioA, usuarioB, chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inicializarComponentes();
        configurarRecyclerView();

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Extras enviados desde el adaptador
        receiverId = getIntent().getStringExtra("USER_ID");
        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");

        if (TextUtils.isEmpty(receiverId)) {
            showToast("Error: receiverId no proporcionado");
            finish();
            return;
        }

        tvNombreUsuario.setText(!TextUtils.isEmpty(nombreUsuario) ? nombreUsuario : "Chat");

        if (currentUserId.compareTo(receiverId) < 0) {
            usuarioA = currentUserId;
            usuarioB = receiverId;
        } else {
            usuarioA = receiverId;
            usuarioB = currentUserId;
        }
        chatId = generarChatId(usuarioA, usuarioB);

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
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnEnviar.setOnClickListener(v -> enviarMensaje());
    }

    private void inicializarComponentes() {
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEstadoUsuario = findViewById(R.id.tvEstadoUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        imgPreview = findViewById(R.id.imgPreview);
        btnEmoji = findViewById(R.id.btnEmoji);
    }

    private void configurarRecyclerView() {
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes, false);
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);
    }

    private void enviarMensaje() {
        String mensajeTexto = editMensaje.getText().toString().trim();

        if (mensajeTexto.isEmpty()) {
            showToast("Escribe un mensaje primero");
            return;
        }

        listaMensajes.add(new MensajeModel(mensajeTexto, true, null));
        mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
        recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);

        checkOrCreateChatAndSendMessage(mensajeTexto);

        editMensaje.setText("");
    }

    private void checkOrCreateChatAndSendMessage(String mensajeTexto) {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        crearChatIndividual(() -> guardarMensajeEnNotificacion(mensajeTexto));
                    } else {
                        guardarMensajeEnNotificacion(mensajeTexto);
                    }
                })
                .addOnFailureListener(e -> showToast("Error al consultar el chat"));
    }

    private void crearChatIndividual(Runnable callback) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("tipo_chat", TIPO_CHAT_INDIVIDUAL_ID);
        chatData.put("usuario_a", usuarioA);
        chatData.put("usuario_b", usuarioB);
        chatData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).set(chatData)
                .addOnSuccessListener(aVoid -> callback.run())
                .addOnFailureListener(e -> showToast("Error al crear el chat individual"));
    }

    private void guardarMensajeEnNotificacion(String mensajeTexto) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("chat_id", chatId);
        messageData.put("mensaje", mensajeTexto);
        messageData.put("usuario_id", currentUserId);
        messageData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(messageData)
                .addOnFailureListener(e -> showToast("Error al enviar el mensaje"));
    }

    private void loadMessages() {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        showToast("Error al cargar mensajes: " + e.getMessage());
                        return;
                    }

                    listaMensajes.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String mensaje = doc.getString("mensaje");
                            String usuarioId = doc.getString("usuario_id");

                            if (usuarioId == null) continue;

                            boolean esEnviado = usuarioId.equals(currentUserId);
                            listaMensajes.add(new MensajeModel(mensaje, esEnviado, null));
                        }

                        mensajeAdapter.notifyDataSetChanged();
                        recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                    } else {
                        mensajeAdapter.notifyDataSetChanged();
                        showToast("No hay mensajes");
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
                .addOnFailureListener(e -> tvEstadoUsuario.setText("Error al cargar estado"));
    }

    private String generarChatId(String id1, String id2) {
        return id1 + "_" + id2;
    }

    private void showToast(String message) {
        Toast.makeText(chatActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
