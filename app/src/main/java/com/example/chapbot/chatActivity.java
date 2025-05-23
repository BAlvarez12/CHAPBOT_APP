package com.example.chapbot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chapbot.helpers.ControladorNotificaciones;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class chatActivity extends AppCompatActivity {
    private static final String TIPO_CHAT_INDIVIDUAL_ID = "NCm3QCIsKw8MjjHycvm5";
    private static final int RC_PICK_IMAGE = 300;

    private TextView tvNombreUsuario, tvEstadoUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar, btnEmoji, btnBack;
    private ImageView imgPerfilUsuario;
    private FrameLayout contenedorBotonEnviar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private MensajeAdapter mensajeAdapter;
    private FirebaseFirestore db;
    private String currentUserId, receiverId, usuarioA, usuarioB, chatId;
    private ListenerRegistration mensajesListener;
    private String nombreActualUsuario;
    private ImagenChats imagenChats;
    private Chatbot chatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("USER_ID");
        chatbot = new Chatbot(receiverId, currentUserId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 500);
            }
        }

        db = FirebaseFirestore.getInstance();

        inicializarComponentes();
        configurarRecyclerView();
        loadMessages();
        tvNombreUsuario.setText("Cargando…");
        db.collection("usuarios").document(receiverId)
                .get()
                .addOnSuccessListener(doc -> {
                    String nombre = doc.getString("nombre");
                    tvNombreUsuario.setText(!TextUtils.isEmpty(nombre) ? nombre : "Chat");
                    nombreActualUsuario = nombre;
                })
                .addOnFailureListener(e -> tvNombreUsuario.setText("Chat"));


        cargarFotoPerfil(receiverId);
        if (currentUserId.compareTo(receiverId) < 0) {
            usuarioA = currentUserId; usuarioB = receiverId;
        } else {
            usuarioA = receiverId; usuarioB = currentUserId;
        }
        chatId = usuarioA + "_" + usuarioB;
        imagenChats = new ImagenChats(this, chatId, currentUserId, nombreActualUsuario);
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> eliminados = doc.get("eliminado_por", List.class);
                        if (eliminados != null && eliminados.contains(currentUserId)) {
                            db.collection("chats").document(chatId)
                                    .update("eliminado_por", FieldValue.arrayRemove(currentUserId))
                                    .addOnFailureListener(e ->
                                            showToast("Error restaurando chat"));
                        }
                    }
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) imagenChats.enviarImagen(uri);
                    }
                }
        );

        btnEnviar.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.click);
            v.startAnimation(anim);
            procesarYEnviar();
        });

        btnBack.setOnClickListener(v -> onBackPressed());
        btnEmoji.setOnClickListener(v -> {
            String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_IMAGES
                    : Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, permiso)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permiso}, RC_PICK_IMAGE);
            } else {
                Intent pick = new Intent(Intent.ACTION_PICK);
                pick.setType("image/*");
                imagePickerLauncher.launch(pick);
            }
        });

        editMensaje.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                if (!s.toString().trim().isEmpty()) mostrarBotonEnviar();
                else btnEnviar.setVisibility(FrameLayout.GONE);
            }
        });
    }

    private void procesarYEnviar() {
        String texto = editMensaje.getText().toString().trim();
        if (texto.isEmpty()) {
            showToast("Escribe un mensaje primero");
            return;
        }
        List<MensajeModel> tmp = new ArrayList<>(mensajeAdapter.getListaMensajes());
        tmp.add(new MensajeModel(texto, true, new Date()));
        mensajeAdapter.updateList(tmp);
        recyclerMensajes.scrollToPosition(tmp.size() - 1);
        editMensaje.setText("");

        chatbot.procesarMensaje(texto, (respuesta, completo, pedido) -> {
            List<MensajeModel> tmp2 = new ArrayList<>(mensajeAdapter.getListaMensajes());

            String tag = "[IMAGEN_BASE64] ";
            if (respuesta.startsWith(tag)) {
                String b64 = respuesta.substring(tag.length());
                tmp2.add(new MensajeModel(b64, /*esEnviado=*/false, new Date(), /*isImage=*/true));
            } else {
                tmp2.add(new MensajeModel(respuesta, /*esEnviado=*/false, new Date()));
            }

            runOnUiThread(() -> {
                mensajeAdapter.updateList(tmp2);
                recyclerMensajes.scrollToPosition(tmp2.size() - 1);
            });

            if (completo && pedido != null) {
            }
            checkOrCreateChatAndSendMessage(respuesta);
        });
    }


    private void inicializarComponentes() {
        tvNombreUsuario      = findViewById(R.id.tvNombreUsuario);
        tvEstadoUsuario      = findViewById(R.id.tvEstadoUsuario);
        recyclerMensajes     = findViewById(R.id.recyclerMensajes);
        editMensaje          = findViewById(R.id.editMensaje);
        btnEnviar            = findViewById(R.id.btnEnviar);
        imgPerfilUsuario     = findViewById(R.id.imgPerfilUsuario);
        btnEmoji             = findViewById(R.id.btnEmoji);
        contenedorBotonEnviar= findViewById(R.id.contenedorBotonEnviar);
        btnBack              = findViewById(R.id.btnBack);
    }

    private void configurarRecyclerView() {
        mensajeAdapter = new MensajeAdapter(
                this, new ArrayList<>(), false);
        recyclerMensajes.setLayoutManager(
                new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(mensajeAdapter);
    }

    private void mostrarBotonEnviar() {
        if (btnEnviar.getVisibility() != FrameLayout.VISIBLE) {
            btnEnviar.setAlpha(0f);
            btnEnviar.setScaleX(0.7f);
            btnEnviar.setScaleY(0.7f);
            btnEnviar.setVisibility(FrameLayout.VISIBLE);
            btnEnviar.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(200).start();
        }
    }

    private void enviarMensaje() {
        String texto = editMensaje.getText().toString().trim();
        if (texto.isEmpty()) {
            showToast("Escribe un mensaje primero");
            return;
        }
        // Optimistic UI
        List<MensajeModel> temp = new ArrayList<>(mensajeAdapter.getListaMensajes());
        temp.add(new MensajeModel(texto, true, new Date()));
        mensajeAdapter.updateList(temp);
        recyclerMensajes.scrollToPosition(temp.size() - 1);

        editMensaje.setText("");
        // Firestore write
        checkOrCreateChatAndSendMessage(texto);
    }

    private void checkOrCreateChatAndSendMessage(String mensajeTexto) {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        crearChatIndividual(() -> enviarNotificacion(mensajeTexto));
                    } else {
                        actualizarUltimoMensaje(mensajeTexto);
                    }
                })
                .addOnFailureListener(e -> showToast("Error al consultar el chat"));
    }

    private void crearChatIndividual(Runnable onDone) {
        Map<String,Object> data = new HashMap<>();
        data.put("tipo_chat", TIPO_CHAT_INDIVIDUAL_ID);
        data.put("usuario_ventas", receiverId);
        data.put("usuario_cliente", currentUserId);
        data.put("fecha_creado", FieldValue.serverTimestamp());
        db.collection("chats").document(chatId)
                .set(data)
                .addOnSuccessListener(a -> onDone.run())
                .addOnFailureListener(e -> showToast("Error al crear chat"));
    }

    private void actualizarUltimoMensaje(String mensajeTexto) {
        Map<String,Object> upd = new HashMap<>();
        upd.put("ultimo_mensaje", mensajeTexto);
        upd.put("ultimo_mensaje_tipo", "texto");
        upd.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());
        db.collection("chats").document(chatId)
                .update(upd)
                .addOnSuccessListener(a -> enviarNotificacion(mensajeTexto))
                .addOnFailureListener(e -> showToast("Error actualizando chat"));
    }

    private void enviarNotificacion(String mensajeTexto) {
        ControladorNotificaciones.enviarMensaje(
                this, chatId, mensajeTexto,
                currentUserId, receiverId,
                tvNombreUsuario.getText().toString()
        );
    }

    private void loadMessages() {
        mensajesListener = db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.ASCENDING)
                .addSnapshotListener((snaps, e) -> {
                    if (e != null) {
                        showToast("Error al cargar: " + e.getMessage());
                        return;
                    }
                    List<MensajeModel> lista = new ArrayList<>();
                    if (snaps != null) {
                        for (DocumentSnapshot d : snaps.getDocuments()) {
                            String uid = d.getString("usuario_id");
                            Date fecha = d.getDate("fecha_creado");
                            boolean esEnviado = uid != null && uid.equals(currentUserId);
                            String tipo = d.getString("tipo");
                            if ("image".equals(tipo)) {
                                lista.add(new MensajeModel(d.getString("image_url"),
                                        esEnviado, fecha, true));
                            } else if ("audio".equals(tipo)) {
                                lista.add(new MensajeModel(
                                        d.getString("audio_url"),
                                        d.getString("duracion"),
                                        esEnviado, fecha
                                ));
                            } else {
                                lista.add(new MensajeModel(
                                        d.getString("mensaje"), esEnviado, fecha));
                            }
                        }
                    }
                    mensajeAdapter.updateList(lista);
                    recyclerMensajes.scrollToPosition(lista.size() - 1);
                });
    }

    private void cargarFotoPerfil(String userId) {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String b64 = doc.getString("fotoBase64");
                    if (!TextUtils.isEmpty(b64)) {
                        byte[] decoded = Base64.decode(b64, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        imgPerfilUsuario.setBackgroundResource(R.drawable.imagen_redonda);
                        imgPerfilUsuario.setImageBitmap(bmp);
                        imgPerfilUsuario.setClipToOutline(true);
                    }
                });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (mensajesListener != null) mensajesListener.remove();
    }

    @Override public void onBackPressed() {
        startActivity(new Intent(this, inicioActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        super.onBackPressed();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
