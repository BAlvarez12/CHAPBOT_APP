package com.example.minechapapp;

import android.app.Activity;
import android.Manifest;
import android.os.Build;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.UUID;
import java.io.InputStream;
import java.io.FileOutputStream;
import com.example.minechapapp.ImagenChats;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;
public class chatActivity extends AppCompatActivity {
    private static final String TIPO_CHAT_INDIVIDUAL_ID = "NCm3QCIsKw8MjjHycvm5";
    private TextView tvGrabando;
    private TextView tvNombreUsuario, tvEstadoUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji, btnAudio;
    private ImageView imgPerfilUsuario;
    private FrameLayout contenedorBotonEnviar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;
    private FirebaseFirestore db;
    private ImageButton btnBack;
    private String currentUserId, receiverId, usuarioA, usuarioB, chatId;
    private ListenerRegistration mensajesListener;
    private String nombreActualUsuario;
    private boolean permisoToastMostrado = false;

    private SupabaseService supabase;
    private static final String SUPABASE_TOKEN =
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsZnVzd2F2bmpta3VjZXB5bnhiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM3MzkxMzMsImV4cCI6MjA1OTMxNTEzM30.xenpXe10Op6aADd2MHHKQcBAH0GoiVyvKdG3i_8w65k";
    private static final String SUPABASE_URL   = "https://vlfuswavnjmkucepynxb.supabase.co/";
    private static final int RC_PICK_IMAGE = 300;
    private AudioGrabacion audioGrabacion;
    private ImagenChats imagenChats;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        supabase = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseService.class);;

        permisoToastMostrado = getSharedPreferences("PreferenciasMineChap", MODE_PRIVATE)
                .getBoolean("permiso_audio_mostrado", false);

        inicializarComponentes();
        configurarRecyclerView();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("USER_ID");
        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        nombreActualUsuario = nombreUsuario;

        if (TextUtils.isEmpty(receiverId)) {
            showToast("Error: receiverId no proporcionado");
            finish();
            return;
        }

        tvNombreUsuario.setText(!TextUtils.isEmpty(nombreUsuario) ? nombreUsuario : "Chat");

        // Cargar foto de perfil desde Base64
        cargarFotoPerfil(receiverId);

        if (currentUserId.compareTo(receiverId) < 0) {
            usuarioA = currentUserId;
            usuarioB = receiverId;
        } else {
            usuarioA = receiverId;
            usuarioB = currentUserId;
        }

        chatId = generarChatId(usuarioA, usuarioB);
        audioGrabacion = new AudioGrabacion(this, chatId, currentUserId, nombreActualUsuario);
        imagenChats = new ImagenChats(this, chatId, currentUserId, nombreActualUsuario);

        // Remover UID de eliminado_por si está presente
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> eliminados = (List<String>) doc.get("eliminado_por");
                        if (eliminados != null && eliminados.contains(currentUserId)) {
                            db.collection("chats").document(chatId)
                                    .update("eliminado_por", FieldValue.arrayRemove(currentUserId))
                                    .addOnSuccessListener(aVoid -> {
                                        // Cargamos mensajes después de restaurar chat
                                        loadMessages();
                                        loadLastMessageStatus();
                                    });
                        } else {
                            loadMessages();
                            loadLastMessageStatus();
                        }
                    } else {
                        loadMessages();
                        loadLastMessageStatus();
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Error al verificar eliminación previa");
                    loadMessages();
                    loadLastMessageStatus();
                });

        Animation clickAnimation = AnimationUtils.loadAnimation(this, R.anim.click);
        btnAudio.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!tienePermisosDeAudio()) {
                        requestPermissions(new String[]{
                                android.Manifest.permission.RECORD_AUDIO
                        }, 200);
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

        btnEnviar.setOnClickListener(v -> {
            v.startAnimation(clickAnimation);
            enviarMensaje();
        });

        btnBack.setOnClickListener(v -> onBackPressed());

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, 200);
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            imagenChats.enviarImagen(imageUri);
                        }
                    }
                }
        );

        btnEmoji.setOnClickListener(v -> {
            // 1) elegimos qué permiso pedir según versión Android
            String permisoGaleria = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_IMAGES
                    : Manifest.permission.READ_EXTERNAL_STORAGE;

            // 2) chequeamos si ya está concedido
            if (ContextCompat.checkSelfPermission(this, permisoGaleria)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ permisoGaleria }, RC_PICK_IMAGE);
            } else {
                // 3) si ya está, abrimos la galería
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            }
        });



        editMensaje.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    mostrarBotonAudio();
                } else {
                    mostrarBotonEnviar();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void cargarFotoPerfil(String userId) {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fotoBase64 = documentSnapshot.getString("fotoBase64");
                        if (fotoBase64 != null && !fotoBase64.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(fotoBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                // Aplicar el fondo redondo
                                imgPerfilUsuario.setBackgroundResource(R.drawable.imagen_redonda);
                                imgPerfilUsuario.setImageBitmap(decodedByte);

                                // Esto es importante para que se vea redondo
                                imgPerfilUsuario.setClipToOutline(true);
                            } catch (Exception e) {
                                showToast("Error al procesar la imagen");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Error al cargar foto de perfil");
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_PICK_IMAGE) {
            // Respuesta al permiso de galería
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Ya tenemos permiso: abrimos la galería
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            } else {
                showToast("Debes aceptar el permiso para acceder a la galería");
            }
        } else if (requestCode == 200) {
            // Tu lógica actual de permiso de audio
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!permisoToastMostrado) {
                    showToast("Permiso concedido, mantén presionado para grabar");
                    permisoToastMostrado = true;
                    getSharedPreferences("PreferenciasMineChap", MODE_PRIVATE)
                            .edit()
                            .putBoolean("permiso_audio_mostrado", true)
                            .apply();
                }
            } else {
                showToast("Debes aceptar el permiso para grabar audio");
            }
        }
    }

    private boolean tienePermisosDeAudio() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void mostrarBotonEnviar() {
        if (btnEnviar.getVisibility() != View.VISIBLE) {
            btnAudio.animate()
                    .alpha(0f).scaleX(0.7f).scaleY(0.7f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(200)
                    .withEndAction(() -> {
                        btnAudio.setVisibility(View.GONE);
                        btnEnviar.setAlpha(0f);
                        btnEnviar.setScaleX(0.7f);
                        btnEnviar.setScaleY(0.7f);
                        btnEnviar.setVisibility(View.VISIBLE);
                        btnEnviar.animate()
                                .alpha(1f).scaleX(1f).scaleY(1f)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .setDuration(200)
                                .start();
                    }).start();
        }
    }

    private void mostrarBotonAudio() {
        if (btnAudio.getVisibility() != View.VISIBLE) {
            btnEnviar.animate()
                    .alpha(0f).scaleX(0.7f).scaleY(0.7f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(200)
                    .withEndAction(() -> {
                        btnEnviar.setVisibility(View.GONE);
                        btnAudio.setAlpha(0f);
                        btnAudio.setScaleX(0.7f);
                        btnAudio.setScaleY(0.7f);
                        btnAudio.setVisibility(View.VISIBLE);
                        btnAudio.animate()
                                .alpha(1f).scaleX(1f).scaleY(1f)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .setDuration(200)
                                .start();
                    }).start();
        }
    }

    private void inicializarComponentes() {
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEstadoUsuario = findViewById(R.id.tvEstadoUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        tvGrabando = findViewById(R.id.tvGrabando);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnAudio = findViewById(R.id.btnAudio);
        imgPerfilUsuario = findViewById(R.id.imgPerfilUsuario);
        btnEmoji = findViewById(R.id.btnEmoji);
        contenedorBotonEnviar = findViewById(R.id.contenedorBotonEnviar);
        btnBack = findViewById(R.id.btnBack);

    }
    private void configurarRecyclerView() {
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(this, listaMensajes, false);
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);
    }
    private void enviarMensaje() {
        String mensajeTexto = editMensaje.getText().toString().trim();
        if (mensajeTexto.isEmpty()) {
            showToast("Escribe un mensaje primero");
            return;
        }
        listaMensajes.add(new MensajeModel(mensajeTexto, true, new Date()));
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
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> updateChat = new HashMap<>();
                    updateChat.put("ultimo_mensaje", mensajeTexto);
                    updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).update(updateChat);
                })
                .addOnFailureListener(e -> showToast("Error al enviar el mensaje"));
    }

    private void loadMessages() {
        mensajesListener = db.collection("notificacion")
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
                            String usuarioId = doc.getString("usuario_id");
                            Date fecha = doc.getDate("fecha_creado");
                            if (usuarioId == null) continue;
                            boolean esEnviado = usuarioId.equals(currentUserId);
                            String tipo = doc.getString("tipo");
                            if ("image".equals(tipo)) {
                                String imageUrl = doc.getString("image_url");
                                listaMensajes.add(new MensajeModel(imageUrl, esEnviado, fecha, true));
                            } else {
                                String audioUrl = doc.getString("audio_url");
                                String duracion = doc.getString("duracion");
                                if (audioUrl != null && duracion != null) {
                                    listaMensajes.add(new MensajeModel(audioUrl, duracion, esEnviado, fecha));
                                } else {
                                    String mensaje = doc.getString("mensaje");
                                    listaMensajes.add(new MensajeModel(mensaje, esEnviado, fecha));
                                }
                            }
                        }
                        mensajeAdapter.notifyDataSetChanged();
                        recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                    } else {
                        mensajeAdapter.notifyDataSetChanged();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mensajesListener != null) {
            mensajesListener.remove();
            mensajesListener = null;
        }
    }

}