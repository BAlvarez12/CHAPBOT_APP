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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.google.firebase.firestore.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.media.MediaPlayer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.io.InputStream;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
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
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String audioFilePath;
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
                        if (imageUri != null) {
                            prepareAndUploadImage(imageUri);
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
        cargarFotoPerfilGrupo(); // NUEVO
        loadMessages();
        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnBack.setOnClickListener(v -> onBackPressed());
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
    private void prepareAndUploadImage(Uri uri) {
        // 1) Prepara el archivo en cache si viene de content://
        Uri uploadUri = uri;
        if ("content".equals(uri.getScheme())) {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                File tmp = new File(getCacheDir(), "upload_" + UUID.randomUUID() + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }
                uploadUri = Uri.fromFile(tmp);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error preparando imagen: " + e.getMessage());
                return;
            }
        }

        // 2) Crea MultipartBody.Part
        File file = new File(uploadUri.getPath());
        RequestBody req = RequestBody.create(
                okhttp3.MediaType.parse("image/jpeg"), file);
        String fileName = UUID.randomUUID().toString() + ".jpg";
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, req);

        // 3) Llama a SupabaseService con tu token y URL constantes
        SupabaseService service = RetrofitClient
                .getInstance()
                .create(SupabaseService.class);
        Call<ResponseBody> call = service.uploadFile(
                SUPABASE_TOKEN,
                fileName,
                part
        );

        // 4) Ejecuta la llamada y guarda el mensaje en Firestore
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                if (r.isSuccessful()) {
                    String imageUrl = SUPABASE_URL
                            + "/storage/v1/object/public/minechap/" + fileName;
                    saveImageMessage(imageUrl);
                } else {
                    showToast("Error subiendo imagen: " + r.code());
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> c, Throwable t) {
                showToast("Fallo al conectar: " + t.getMessage());
            }
        });
    }

    private void saveImageMessage(String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("chat_id", chatId);
        data.put("tipo", "image");
        data.put("image_url", imageUrl);
        data.put("usuario_id", currentUserId);
        data.put("nombre_usuario", nombreActualUsuario);
        data.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion")
                .add(data)
                .addOnFailureListener(e -> showToast("Error guardando imagen"));
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
            File audioDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            File audioFile = new File(audioDir, "AUDIO_" + System.currentTimeMillis() + ".3gp");
            audioFilePath = audioFile.getAbsolutePath();
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            showToast("🎙 Grabando...");
        } catch (IOException e) {
            e.printStackTrace();
            showToast("❌ Error al iniciar grabación: " + e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            showToast("❌ Estado ilegal: " + e.getMessage());
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

    private void subirAudioASupabase(File audioFile) {
        String supabaseUrl = SUPABASE_URL;
        String supabaseBearerToken = SUPABASE_TOKEN;
        String fileName = "audio/" + audioFile.getName();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(supabaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SupabaseService service = retrofit.create(SupabaseService.class);
        RequestBody requestBody = RequestBody.create(MediaType.parse("audio/3gp"), audioFile);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", audioFile.getName(), requestBody);
        Call<ResponseBody> call = service.uploadFile(supabaseBearerToken, fileName, part);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String audioUrl = SUPABASE_URL + "/storage/v1/object/public/minechap/" + fileName;
                    long duracion = getDuracionAudio(audioFile.getAbsolutePath());
                    String duracionTexto = convertirDuracion(duracion);
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
        if (imageUriSeleccionada != null) {
            subirImagenASupabase(imageUriSeleccionada);
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
    private void guardarMensajeAudio(String audioUrl, String duracion) {
        Map<String, Object> mensajeData = new HashMap<>();
        mensajeData.put("chat_id", chatId);
        mensajeData.put("audio_url", audioUrl);
        mensajeData.put("duracion", duracion);
        mensajeData.put("usuario_id", currentUserId);
        mensajeData.put("nombre_usuario", nombreActualUsuario);
        mensajeData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(mensajeData)
                .addOnSuccessListener(documentReference -> showToast("Audio enviado"))
                .addOnFailureListener(e -> showToast("Error al guardar audio"));
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
    private void subirImagenASupabase(Uri imagenUri) {
        try {
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            InputStream inputStream = getContentResolver().openInputStream(imagenUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();
            RequestBody requestFile = RequestBody.create(bytes, okhttp3.MediaType.parse("image/jpeg"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);
            SupabaseService service = RetrofitClient
                    .getInstance()
                    .create(SupabaseService.class);
            Call<ResponseBody> call = service.uploadFile(SUPABASE_TOKEN, fileName, body); // USO CORRECTO
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String imageUrl = SUPABASE_URL + "/storage/v1/object/public/minechap/" + fileName;
                        guardarMensajeImagenEnFirestore(imageUrl);
                    } else {
                        showToast("Error al subir imagen: " + response.code());
                    }
                    imageUriSeleccionada = null;
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showToast("Fallo en la carga: " + t.getMessage());
                    imageUriSeleccionada = null;
                }
            });

        } catch (Exception e) {
            showToast("Error leyendo imagen: " + e.getMessage());
        }
    }
    private void guardarMensajeImagenEnFirestore(String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("chat_id", chatId);
        data.put("usuario_id", currentUserId);
        data.put("nombre_usuario", nombreActualUsuario);
        data.put("imagen_url", imageUrl);
        data.put("fecha_creado", FieldValue.serverTimestamp());
        db.collection("notificacion")
                .add(data)
                .addOnSuccessListener(doc -> {
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
                        boolean esEnviado = usuarioId != null && usuarioId.equals(currentUserId);
                        if (doc.contains("audio_url")) {
                            String audioUrl = doc.getString("audio_url");
                            String duracion = doc.getString("duracion");
                            listaMensajes.add(new MensajeModel(audioUrl, duracion, esEnviado, fecha));
                        }
                        else if (doc.contains("image_url")) {
                            String imageUrl = doc.getString("image_url");
                            listaMensajes.add(new MensajeModel(imageUrl, esEnviado, true, fecha));
                        }
                        else {
                            String mensaje = doc.getString("mensaje");
                            listaMensajes.add(new MensajeModel(mensaje, esEnviado, nombreUsuario, fecha));
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
}
