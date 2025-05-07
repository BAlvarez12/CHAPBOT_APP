package com.example.chapbot;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.Query;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

import android.Manifest;
import android.os.Build;


public class GrupoActivity extends AppCompatActivity {
    private static final String TIPO_CHAT_GRUPAL_ID = "97XeeFNzro7xurmKwKeh";

    private static final String SUPABASE_URL   = "https://vlfuswavnjmkucepynxb.supabase.co";
    private static final String SUPABASE_TOKEN =
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsZnVzd2F2bmpta3VjZXB5bnhiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM3MzkxMzMsImV4cCI6MjA1OTMxNTEzM30.xenpXe10Op6aADd2MHHKQcBAH0GoiVyvKdG3i_8w65k";
    private TextView tvNombreGrupo;
    private ImageView imgPerfilUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji, btnAudio, btnBack;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;
    private FirebaseFirestore db;
    private String currentUserId, chatId, nombreGrupo, nombreActualUsuario;
    private Uri imageUriSeleccionada;
    private AudioGrabacion audioGrabacion;

    private boolean permisoToastMostrado = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        permisoToastMostrado = getSharedPreferences("PreferenciasMineChap", MODE_PRIVATE)
                .getBoolean("permiso_audio_mostrado", false);
        inicializarComponentes();
        configurarRecyclerMensajes();
        configurarPickImagen();
        configurarGrabacionAudio();
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && nombreActualUsuario != null) {
                            ImagenChats imagenChats = new ImagenChats(
                                    GrupoActivity.this,
                                    chatId,
                                    currentUserId,
                                    nombreActualUsuario,
                                    true
                            );
                            imagenChats.enviarImagen(imageUri);
                        } else {
                            showToast("Error: nombre de usuario no disponible o imagen nula.");
                        }
                    }
                }
        );

        btnEmoji.setOnClickListener(v -> {
            String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_IMAGES
                    : Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(this, permiso)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ permiso }, 300);
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            }
        });
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatId = getIntent().getStringExtra("chatId");
        nombreGrupo = getIntent().getStringExtra("nombreGrupo");
        if (TextUtils.isEmpty(chatId)) {
            showToast("Error: ID del chat no proporcionado");
            finish();
            return;
        }
        tvNombreGrupo.setText(!TextUtils.isEmpty(nombreGrupo) ? nombreGrupo : "Chat grupal");
        obtenerNombreUsuarioActual();
        verificarChatGrupal();
        cargarFotoPerfilGrupo();
        loadMessages();
        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnBack.setOnClickListener(v -> {
            new android.os.Handler().postDelayed(() -> onBackPressed(), 50);
        });

    }
    private void inicializarComponentes() {
        tvNombreGrupo = findViewById(R.id.tvNombreUsuario);
        imgPerfilUsuario = findViewById(R.id.imgPerfilUsuario); // NUEVO
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnAudio = findViewById(R.id.btnAudio);
        btnEmoji = findViewById(R.id.btnEmoji);
        btnBack = findViewById(R.id.btnBack);
    }
    private void cargarFotoPerfilGrupo() {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String base64 = documentSnapshot.getString("foto_grupo_base64");
                    if (base64 != null && !base64.isEmpty()) {
                        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        imgPerfilUsuario.setImageBitmap(bitmap);
                    }
                })
                .addOnFailureListener(e -> showToast("Error al cargar foto del grupo"));
    }

    private void configurarRecyclerMensajes() {
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(this, listaMensajes, true);
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

    private void configurarGrabacionAudio() {
        btnAudio.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!tienePermisosDeAudio()) {
                        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);
                        return false;
                    }
                    audioGrabacion.startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (audioGrabacion.isRecording()) {
                        audioGrabacion.stopRecording();
                    }
                    return true;
            }
            return false;
        });

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);
        }

        editMensaje.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) mostrarBotonAudio();
                else mostrarBotonEnviar();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }
    private boolean tienePermisosDeAudio() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    private void mostrarBotonEnviar() {
        btnAudio.setVisibility(View.GONE);
        btnEnviar.setVisibility(View.VISIBLE);
    }

    private void mostrarBotonAudio() {
        btnEnviar.setVisibility(View.GONE);
        btnAudio.setVisibility(View.VISIBLE);
    }

    private void obtenerNombreUsuarioActual() {
        db.collection("usuarios").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    nombreActualUsuario = documentSnapshot.getString("nombre");
                    if (TextUtils.isEmpty(nombreActualUsuario)) nombreActualUsuario = "Tú";
                    audioGrabacion = new AudioGrabacion(this, chatId, currentUserId, nombreActualUsuario);
                    enviarMensajeInicialSiEsNecesario();
                })

                .addOnFailureListener(e -> {
                    nombreActualUsuario = "Tú";
                    showToast("Error al obtener el nombre de usuario");
                });
    }
    private void verificarChatGrupal() {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String tipoChat = documentSnapshot.getString("tipo_chat");
                    if (!TIPO_CHAT_GRUPAL_ID.equals(tipoChat)) {
                        showToast("Tipo de chat incorrecto");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Error al verificar el tipo de chat");
                    finish();
                });
    }
    private void enviarMensaje() {
        String mensajeTexto = editMensaje.getText().toString().trim();
        if (imageUriSeleccionada != null) {
            ImagenChats imagenChats = new ImagenChats(
                    GrupoActivity.this,
                    chatId,
                    currentUserId,
                    nombreActualUsuario,
                    true
            );
            imagenChats.enviarImagen(imageUriSeleccionada);
            imageUriSeleccionada = null;
            return;
        }
        if (mensajeTexto.isEmpty()) {
            showToast("Debes escribir un mensaje o enviar una imagen");
            return;
        }
        listaMensajes.add(new MensajeModel(mensajeTexto, true, nombreActualUsuario, new Date()));
        mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
        recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
        guardarMensajeEnFirestore(mensajeTexto);
        editMensaje.setText("");
    }
    private void guardarMensajeEnFirestore(String mensajeTexto) {
        Map<String, Object> mensajeData = new HashMap<>();
        mensajeData.put("chat_id", chatId);
        mensajeData.put("mensaje", mensajeTexto);
        mensajeData.put("usuario_id", currentUserId);
        mensajeData.put("nombre_usuario", nombreActualUsuario);
        mensajeData.put("fecha_creado", FieldValue.serverTimestamp());

        Map<String, Object> updateChat = new HashMap<>();
        updateChat.put("ultimo_mensaje", mensajeTexto);
        updateChat.put("ultimo_mensaje_tipo", "texto");
        updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).update(updateChat)
                .addOnSuccessListener(aVoid -> {
                    // ✅ Luego de actualizar el chat, se guarda el mensaje
                    db.collection("notificacion").add(mensajeData);
                })
                .addOnFailureListener(e -> showToast("Error al actualizar el chat"));
    }

    private void guardarMensajeImagenEnFirestore(String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("chat_id", chatId);
        data.put("usuario_id", currentUserId);
        data.put("nombre_usuario", nombreActualUsuario);
        data.put("imagen_url", imageUrl);
        data.put("fecha_creado", FieldValue.serverTimestamp());
        data.put("tipo", "image"); // 🔹 importante

        db.collection("notificacion")
                .add(data)
                .addOnSuccessListener(doc -> {
                    // ✅ ACTUALIZAMOS EL CHAT
                    Map<String, Object> updateChat = new HashMap<>();
                    updateChat.put("ultimo_mensaje", "📷 Imagen");
                    updateChat.put("ultimo_mensaje_tipo", "imagen");
                    updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).update(updateChat);
                })
                .addOnFailureListener(e -> {
                    showToast("Error guardando imagen en chat");
                });
    }

    private void loadMessages() {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        showToast("Error al cargar mensajes");
                        return;
                    }

                    listaMensajes.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String usuarioId = doc.getString("usuario_id");
                        String nombreUsuario = doc.getString("nombre_usuario");
                        Date fecha = doc.getDate("fecha_creado");
                        String tipo = doc.getString("tipo"); // ✅ Ahora sí podemos usar "tipo"

                        boolean esEnviado = usuarioId != null && usuarioId.equals(currentUserId);

                        if (doc.contains("audio_url")) {
                            String audioUrl = doc.getString("audio_url");
                            String duracion = doc.getString("duracion");
                            listaMensajes.add(new MensajeModel(audioUrl, duracion, esEnviado, nombreUsuario, fecha));

                        } else if ("image".equals(tipo)) {
                            String imageUrl = doc.getString("image_url");
                            listaMensajes.add(new MensajeModel(imageUrl, esEnviado, nombreUsuario, fecha, true)); // ✅ constructor de imagen

                        } else {
                            String mensaje = doc.getString("mensaje");
                            listaMensajes.add(new MensajeModel(mensaje, esEnviado, nombreUsuario, fecha)); // ✅ constructor de texto
                        }
                    }

                    mensajeAdapter.notifyDataSetChanged();
                    recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                });
    }

    private void showToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
    private void enviarMensajeInicialSiEsNecesario() {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        String mensaje = "Grupo creado por " + nombreActualUsuario;
                        Map<String, Object> mensajeData = new HashMap<>();
                        mensajeData.put("chat_id", chatId);
                        mensajeData.put("mensaje", mensaje);
                        mensajeData.put("usuario_id", currentUserId);
                        mensajeData.put("nombre_usuario", nombreActualUsuario);
                        mensajeData.put("fecha_creado", FieldValue.serverTimestamp());
                        db.collection("notificacion").add(mensajeData)
                                .addOnSuccessListener(documentReference -> {
                                    Map<String, Object> updateChat = new HashMap<>();
                                    updateChat.put("ultimo_mensaje", mensaje);
                                    updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());
                                    db.collection("chats").document(chatId)
                                            .update(updateChat)
                                            .addOnSuccessListener(aVoid -> {
                                                loadMessages();
                                            });
                                });
                    }
                });
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GrupoActivity.this, inicioActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        super.onBackPressed();
    }
}
