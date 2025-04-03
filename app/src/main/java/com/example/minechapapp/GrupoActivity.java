package com.example.minechapapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GrupoActivity extends AppCompatActivity {

    private static final String TAG = "GrupoActivity";
    private static final String TIPO_CHAT_GRUPAL_ID = "97XeeFNzro7xurmKwKeh";

    private TextView tvNombreGrupo;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji;
    private ImageView imgPreview;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;

    private FirebaseFirestore db;
    private String currentUserId, chatId, nombreGrupo, nombreActualUsuario;
    private Uri imageUriSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inicializarComponentes();
        configurarRecyclerMensajes();
        configurarPickImagen();

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatId = getIntent().getStringExtra("chatId");
        nombreGrupo = getIntent().getStringExtra("nombreGrupo");

        if (TextUtils.isEmpty(chatId)) {
            showToast("Error: ID del chat no proporcionado");
            finish();
            return;
        }

        Log.d(TAG, "Chat grupal iniciado, chatId: " + chatId);
        tvNombreGrupo.setText(!TextUtils.isEmpty(nombreGrupo) ? nombreGrupo : "Chat grupal");

        obtenerNombreUsuarioActual();
        verificarChatGrupal();
        loadMessages();

        btnEmoji.setOnClickListener(v -> seleccionarImagen());
        btnEnviar.setOnClickListener(v -> enviarMensaje());
    }

    private void inicializarComponentes() {
        tvNombreGrupo = findViewById(R.id.tvNombreUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnEmoji = findViewById(R.id.btnEmoji);
        imgPreview = findViewById(R.id.imgPreview);
    }

    private void configurarRecyclerMensajes() {
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes, true);
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);
    }

    private void configurarPickImagen() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUriSeleccionada = result.getData().getData();
                        if (imageUriSeleccionada != null) {
                            imgPreview.setImageURI(imageUriSeleccionada);
                            imgPreview.setVisibility(ImageView.VISIBLE);
                        }
                    }
                }
        );
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void obtenerNombreUsuarioActual() {
        db.collection("usuarios").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        nombreActualUsuario = documentSnapshot.getString("nombre");
                        Log.d(TAG, "Nombre del usuario actual: " + nombreActualUsuario);
                    } else {
                        nombreActualUsuario = "Tú";
                        Log.w(TAG, "Documento de usuario no existe");
                    }
                })
                .addOnFailureListener(e -> {
                    nombreActualUsuario = "Tú";
                    Log.e(TAG, "Error al obtener el nombre de usuario", e);
                    showToast("Error al obtener el nombre de usuario");
                });
    }

    private void verificarChatGrupal() {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showToast("Chat no encontrado");
                        finish();
                        return;
                    }
                    String tipoChat = documentSnapshot.getString("tipo_chat");
                    if (!TIPO_CHAT_GRUPAL_ID.equals(tipoChat)) {
                        showToast("Tipo de chat desconocido");
                        finish();
                    } else {
                        Log.d(TAG, "Chat grupal verificado correctamente");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al verificar el chat grupal", e);
                    showToast("Error al verificar chat grupal");
                    finish();
                });
    }

    private void enviarMensaje() {
        String mensajeTexto = editMensaje.getText().toString().trim();

        if (mensajeTexto.isEmpty() && imageUriSeleccionada == null) {
            showToast("Debes escribir un mensaje o enviar una imagen");
            return;
        }
        if (TextUtils.isEmpty(nombreActualUsuario)) {
            showToast("No se encontró tu nombre de usuario");
            return;
        }

        if (!mensajeTexto.isEmpty()) {
            listaMensajes.add(new MensajeModel(mensajeTexto, true, nombreActualUsuario, new Date()));
            mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
            recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
            guardarMensajeEnFirestore(mensajeTexto);
            editMensaje.setText("");
        }

        if (imageUriSeleccionada != null) {
            subirImagenAFirestore(imageUriSeleccionada);
            imgPreview.setVisibility(ImageView.GONE);
            imageUriSeleccionada = null;
        }
    }

    private void guardarMensajeEnFirestore(String mensajeTexto) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("chat_id", chatId);
        messageData.put("mensaje", mensajeTexto);
        messageData.put("usuario_id", currentUserId);
        messageData.put("nombre_usuario", nombreActualUsuario);
        messageData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(messageData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Mensaje de texto enviado"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar el mensaje en Firestore", e);
                    showToast("Error al enviar el mensaje");
                });
    }

    private void subirImagenAFirestore(Uri imagenUri) {
        showToast("Función para enviar imágenes aún no implementada");
    }

    private void loadMessages() {
        Log.d(TAG, "Cargando mensajes del chatId: " + chatId);

        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error al obtener mensajes", e);
                        showToast("Error al cargar mensajes: " + e.getMessage());
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "No hay mensajes en el chat");
                        mensajeAdapter.notifyDataSetChanged();
                        return;
                    }

                    listaMensajes.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String mensaje = doc.getString("mensaje");
                        String usuarioId = doc.getString("usuario_id");
                        String nombreUsuario = doc.getString("nombre_usuario");

                        if (usuarioId == null) usuarioId = "Desconocido";
                        if (nombreUsuario == null) nombreUsuario = "Usuario desconocido";

                        boolean esEnviado = usuarioId.equals(currentUserId);

                        Date fecha = doc.getTimestamp("fecha_creado") != null
                                ? doc.getTimestamp("fecha_creado").toDate()
                                : new Date();

                        listaMensajes.add(new MensajeModel(mensaje, esEnviado, nombreUsuario, fecha));
                    }

                    mensajeAdapter.notifyDataSetChanged();
                    recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                });
    }

    private void showToast(String mensaje) {
        Toast.makeText(GrupoActivity.this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
