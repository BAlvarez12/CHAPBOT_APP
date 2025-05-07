package com.example.chapbot;
import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class render {

    public void render(String receptorToken, String titulo, String contenido, String idChat) {
        OkHttpClient render = new OkHttpClient();

        JSONObject datos = new JSONObject();
        try {
            datos.put("token", receptorToken);
            datos.put("titulo", titulo);
            datos.put("mensaje", contenido);
            datos.put("chatId", idChat);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestBody cuerpoSolicitud = RequestBody.create(
                datos.toString(),
                MediaType.parse("application/json")
        );

        Request solicitud = new Request.Builder()
                .url("https://minechap-app-backend.onrender.com/enviarNotificacion")
                .post(cuerpoSolicitud)
                .build();

        new Thread(() -> {
            try (Response respuesta = render.newCall(solicitud).execute()) {
                if (respuesta.isSuccessful()) {
                    Log.d("NOTIF_RENDER", "Notificación enviada");
                } else {
                    Log.e("NOTIF_RENDER", "Error al enviar notificación: " + respuesta.code());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}