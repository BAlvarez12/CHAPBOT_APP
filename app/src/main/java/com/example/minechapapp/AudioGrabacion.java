package com.example.minechapapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import okhttp3.*;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

public class AudioGrabacion {
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String audioFilePath;
    private final Context context;
    private final FirebaseFirestore db;
    private final String chatId;
    private final String userId;
    private final String nombreUsuario;
    private final SupabaseService supabase;
    private final String supabaseUrl = "https://vlfuswavnjmkucepynxb.supabase.co";
    private final String supabaseBearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsZnVzd2F2bmpta3VjZXB5bnhiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM3MzkxMzMsImV4cCI6MjA1OTMxNTEzM30.xenpXe10Op6aADd2MHHKQcBAH0GoiVyvKdG3i_8w65k";

    public AudioGrabacion(Context context, String chatId, String userId, String nombreUsuario) {
        this.context = context;
        this.chatId = chatId;
        this.userId = userId;
        this.nombreUsuario = nombreUsuario;
        this.db = FirebaseFirestore.getInstance();

        supabase = new Retrofit.Builder()
                .baseUrl(supabaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseService.class);
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording() {
        try {
            String fileName = "AUDIO_" + System.currentTimeMillis() + ".3gp";
            File audioDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
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
            Toast.makeText(context, "🎙 Grabando...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al iniciar grabación", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;

                File audioFile = new File(audioFilePath);
                uploadAudioToSupabase(audioFile);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error al detener grabación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadAudioToSupabase(File audioFile) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("audio/3gp"), audioFile);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", audioFile.getName(), requestBody);

        supabase.uploadFile(supabaseBearer, audioFile.getName(), part)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            String audioUrl = supabaseUrl + "/storage/v1/object/public/minechap/" + audioFile.getName();
                            long duracion = getDuracionAudio(audioFile.getAbsolutePath());
                            String duracionTexto = convertirDuracion(duracion);
                            guardarMensajeAudio(audioUrl, duracionTexto);
                        } else {
                            Toast.makeText(context, "Error al subir audio: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        t.printStackTrace();
                        Toast.makeText(context, "Fallo al conectar con Supabase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarMensajeAudio(String audioUrl, String duracion) {
        Map<String, Object> mensajeData = new HashMap<>();
        mensajeData.put("chat_id", chatId);
        mensajeData.put("audio_url", audioUrl);
        mensajeData.put("duracion", duracion);
        mensajeData.put("usuario_id", userId);
        mensajeData.put("nombre_usuario", nombreUsuario);
        mensajeData.put("fecha_creado", FieldValue.serverTimestamp());
        mensajeData.put("tipo", "audio"); // ✅ Agregado

        db.collection("notificacion").add(mensajeData)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> updateChat = new HashMap<>();
                    updateChat.put("ultimo_mensaje", "🎤 Audio");
                    updateChat.put("ultimo_mensaje_tipo", "audio"); // ✅ Agregado
                    updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).update(updateChat);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error al guardar audio", Toast.LENGTH_SHORT).show()
                );
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

    public File descargarAudioDesdeUrl(String url, String nombreArchivo) throws IOException {
        URL audioUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) audioUrl.openConnection();
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error al descargar el archivo: " + connection.getResponseMessage());
        }

        InputStream inputStream = connection.getInputStream();
        File archivo = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), nombreArchivo);
        FileOutputStream outputStream = new FileOutputStream(archivo);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
        connection.disconnect();

        return archivo;
    }
}
