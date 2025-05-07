package com.example.chapbot;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImagenChats {
    private final Context context;
    private final String chatId;
    private final String usuarioId;
    private final String nombreUsuario;
    private final FirebaseFirestore db;
    private final SupabaseService supabase;
    private static final String SUPABASE_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsZnVzd2F2bmpta3VjZXB5bnhiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM3MzkxMzMsImV4cCI6MjA1OTMxNTEzM30.xenpXe10Op6aADd2MHHKQcBAH0GoiVyvKdG3i_8w65k";
    private static final String SUPABASE_URL = "https://vlfuswavnjmkucepynxb.supabase.co/";
    private boolean esGrupo;



    public ImagenChats(Context context, String chatId, String usuarioId, String nombreUsuario, boolean esGrupo) {
        this.context = context;
        this.chatId = chatId;
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.esGrupo = esGrupo;
        this.db = FirebaseFirestore.getInstance();
        this.supabase = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseService.class);
    }

    public ImagenChats(Context context, String chatId, String usuarioId, String nombreUsuario) {
        this.context = context;
        this.chatId = chatId;
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.db = FirebaseFirestore.getInstance();
        this.supabase = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseService.class);
    }

    public void enviarImagen(Uri uri) {
        Uri uploadUri = copiarACacheSiEsNecesario(uri);
        if (uploadUri == null) return;

        File file = new File(uploadUri.getPath());
        RequestBody req = RequestBody.create(MediaType.parse("image/jpeg"), file);
        String fileName = UUID.randomUUID().toString() + ".jpg";
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, req);

        supabase.uploadFile(SUPABASE_TOKEN, fileName, part).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                if (r.isSuccessful()) {
                    String imageUrl = SUPABASE_URL + "storage/v1/object/public/minechap/" + fileName;
                    guardarMensajeEnFirestore(imageUrl);
                } else {
                    Toast.makeText(context, "Error subiendo imagen: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> c, Throwable t) {
                Toast.makeText(context, "Fallo al conectar con Supabase: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarMensajeEnFirestore(String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("chat_id", chatId);
        data.put("tipo", "image");
        data.put("image_url", imageUrl);
        data.put("usuario_id", usuarioId);
        data.put("nombre_usuario", nombreUsuario);
        data.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("ultimo_mensaje", "[Imagen]");
                    update.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());
                    db.collection("chats").document(chatId).update(update);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error guardando imagen", Toast.LENGTH_SHORT).show();
                });
    }

    private Uri copiarACacheSiEsNecesario(Uri uri) {
        if (!"content".equals(uri.getScheme())) return uri;

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            File tmp = new File(context.getCacheDir(), "upload_" + UUID.randomUUID() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
            }
            return Uri.fromFile(tmp);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error preparando imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
