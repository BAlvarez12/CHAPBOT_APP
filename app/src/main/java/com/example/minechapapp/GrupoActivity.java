package com.example.minechapapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class GrupoActivity extends AppCompatActivity {
    private static final String TIPO_CHAT_GRUPAL_ID = "97XeeFNzro7xurmKwKeh";
    private TextView tvNombreGrupo;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji, btnAudio, btnBack;
    private ImageView imgPreview, imgGrupoPerfil;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;
    private FirebaseFirestore db;
    private String currentUserId, chatId, nombreGrupo, nombreActualUsuario;
    private Uri imageUriSeleccionada;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String audioFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inicializarComponentes();
        configurarRecyclerMensajes();
        configurarPickImagen();
        configurarGrabacionAudio();

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
        cargarFotoPerfilGrupo(chatId);
        loadMessages();

        btnEmoji.setOnClickListener(v -> seleccionarImagen());
        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void inicializarComponentes() {
        tvNombreGrupo = findViewById(R.id.tvNombreUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnAudio = findViewById(R.id.btnAudio);
        btnEmoji = findViewById(R.id.btnEmoji);
        imgPreview = findViewById(R.id.imgPreview);
        btnBack = findViewById(R.id.btnBack);
        imgGrupoPerfil = findViewById(R.id.imgPerfilUsuario); // Asegúrate de tener este ID en el XML
    }

    private void cargarFotoPerfilGrupo(String chatId) {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String base64 = documentSnapshot.getString("foto_grupo_base64");
                    if (base64 != null && !base64.isEmpty()) {
                        try {
                            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(decoded));
                            imgGrupoPerfil.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("Error al cargar imagen del grupo");
                        }
                    }
                })
                .addOnFailureListener(e -> showToast("No se pudo obtener la imagen"));
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

    private void configurarGrabacionAudio() {
        btnAudio.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!tienePermisosDeAudio()) {
                        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);
                        return false;
                    }
                    iniciarGrabacionAudio();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isRecording) {
                        detenerGrabacionAudio();
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

    private void iniciarGrabacionAudio() {
        try {
            String fileName = "AUDIO_" + System.currentTimeMillis() + ".3gp";
            File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File audioFile = new File(audioDir, fileName);
            audioFilePath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            showToast("🎙 Grabando...");
        } catch (IOException e) {
            showToast("Error al iniciar grabación");
        }
    }

    private void detenerGrabacionAudio() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            showToast("Grabación finalizada");
        } catch (Exception e) {
            showToast("Error al detener grabación");
        }
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
        if (mensajeTexto.isEmpty()) {
            showToast("Escribe un mensaje");
            return;
        }

        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("chat_id", chatId);
        mensaje.put("mensaje", mensajeTexto);
        mensaje.put("usuario_id", currentUserId);
        mensaje.put("nombre_usuario", nombreActualUsuario);
        mensaje.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(mensaje);
        editMensaje.setText("");
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
                        String mensaje = doc.getString("mensaje");
                        String usuarioId = doc.getString("usuario_id");
                        String nombreUsuario = doc.getString("nombre_usuario");
                        Date fecha = doc.getDate("fecha_creado");

                        boolean esEnviado = usuarioId != null && usuarioId.equals(currentUserId);
                        listaMensajes.add(new MensajeModel(mensaje, esEnviado, nombreUsuario, fecha));
                    }
                    mensajeAdapter.notifyDataSetChanged();
                    recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                });
    }

    private void showToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
