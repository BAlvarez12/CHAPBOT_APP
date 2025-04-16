package com.example.minechapapp;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.firebase.firestore.Query;
import java.io.File;
import java.io.IOException;
import java.util.*;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
public class GrupoActivity extends AppCompatActivity {
    private static final String TIPO_CHAT_GRUPAL_ID = "97XeeFNzro7xurmKwKeh";
    private TextView tvNombreGrupo;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji, btnAudio, btnBack;
    private ImageView imgPreview;
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
                            imgPreview.setVisibility(View.VISIBLE);
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
            subirAudioASupabase(new File(audioFilePath));
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

    private long getDuracionAudio(String filePath) {
        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(filePath);
            player.prepare();
            int duration = player.getDuration();
            player.release();
            return duration;
        } catch (Exception e) {
            return 0;
        }
    }
    private String convertirDuracion(long milisegundos) {
        int segundos = (int) (milisegundos / 1000);
        int minutos = segundos / 60;
        segundos %= 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutos, segundos);
    }
    private void guardarMensajeAudio(String audioUrl, String duracion) {
        Map<String, Object> mensajeData = new HashMap<>();
        mensajeData.put("chat_id", chatId);
        mensajeData.put("audio_url", audioUrl);
        mensajeData.put("duracion", duracion);
        mensajeData.put("usuario_id", currentUserId);
        mensajeData.put("fecha_creado", FieldValue.serverTimestamp());
        db.collection("notificacion").add(mensajeData)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> updateChat = new HashMap<>();
                    updateChat.put("ultimo_mensaje", "🎤 Audio");
                    updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());
                    db.collection("chats").document(chatId).update(updateChat);
                })
                .addOnFailureListener(e -> showToast("Error al guardar audio"));
    }
    private void subirAudioASupabase(File audioFile) {
        String supabaseUrl = "https://vlfuswavnjmkucepynxb.supabase.co";
        String supabaseBearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsZnVzd2F2bmpta3VjZXB5bnhiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM3MzkxMzMsImV4cCI6MjA1OTMxNTEzM30.xenpXe10Op6aADd2MHHKQcBAH0GoiVyvKdG3i_8w65k";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(supabaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SupabaseService service = retrofit.create(SupabaseService.class);
        RequestBody requestBody = RequestBody.create(MediaType.parse("audio/3gp"), audioFile);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", audioFile.getName(), requestBody);
        Call<ResponseBody> call = service.uploadAudio(supabaseBearerToken, audioFile.getName(), part);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String audioUrl = supabaseUrl + "/storage/v1/object/public/minechap/" + audioFile.getName();
                    long duracion = getDuracionAudio(audioFile.getAbsolutePath());
                    String duracionTexto = convertirDuracion(duracion);
                    listaMensajes.add(new MensajeModel(audioUrl, duracionTexto, true, new Date()));
                    mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                    recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);

                    guardarMensajeAudio(audioUrl, duracionTexto);
                } else {
                    showToast("Error al subir audio: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                showToast("Fallo al conectar con Supabase");
            }
        });
    }

    private void obtenerNombreUsuarioActual() {
        db.collection("usuarios").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    nombreActualUsuario = documentSnapshot.getString("nombre");
                    if (TextUtils.isEmpty(nombreActualUsuario)) nombreActualUsuario = "Tú";
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
        if (mensajeTexto.isEmpty() && imageUriSeleccionada == null) {
            showToast("Debes escribir un mensaje o enviar una imagen");
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
            imgPreview.setVisibility(View.GONE);
            imageUriSeleccionada = null;
        }
    }

    private void guardarMensajeEnFirestore(String mensajeTexto) {
        Map<String, Object> mensajeData = new HashMap<>();
        mensajeData.put("chat_id", chatId);
        mensajeData.put("mensaje", mensajeTexto);
        mensajeData.put("usuario_id", currentUserId);
        mensajeData.put("nombre_usuario", nombreActualUsuario);
        mensajeData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(mensajeData);
    }

    private void subirImagenAFirestore(Uri imagenUri) {
        showToast("Función para subir imágenes aún no implementada");
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
                        boolean esEnviado = usuarioId != null && usuarioId.equals(currentUserId);
                        String audioUrl = doc.getString("audio_url");
                        String duracion = doc.getString("duracion");

                        if (audioUrl != null && duracion != null) {
                            listaMensajes.add(new MensajeModel(audioUrl, duracion, esEnviado, fecha));
                        } else {
                            String mensaje = doc.getString("mensaje");
                            listaMensajes.add(new MensajeModel(mensaje, esEnviado, nombreUsuario, fecha));
                        }
                    }
                    mensajeAdapter.notifyDataSetChanged();
                    recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                });
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

    private void showToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
