package com.example.chapbot.helpers;

import android.content.Context;
import android.util.Log;
import com.example.chapbot.render;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ControladorNotificaciones {

    public static void enviarMensaje(Context context, String chatId, String mensajeTexto, String currentUserId, String receiverId, String nombreUsuario) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("chat_id", chatId);
        messageData.put("mensaje", mensajeTexto);
        messageData.put("usuario_id", currentUserId);
        messageData.put("usuario_id_destino", receiverId);
        messageData.put("nombre_usuario", nombreUsuario);
        messageData.put("fecha_creado", FieldValue.serverTimestamp());

        db.collection("notificacion").add(messageData)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> updateChat = new HashMap<>();
                    updateChat.put("ultimo_mensaje", mensajeTexto);
                    updateChat.put("ultimo_mensaje_timestamp", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).update(updateChat);

                    db.collection("usuarios").document(receiverId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String tokenDestinatario = documentSnapshot.getString("token");
                                    if (tokenDestinatario != null) {
                                        Log.d("RENDER_CALL", "Invocando render...");
                                        new render().render(tokenDestinatario, "Nuevo mensaje", mensajeTexto, chatId);
                                    } else {
                                        Log.e("RENDER_CALL", "Token receptor es null");
                                    }
                                } else {
                                    Log.e("RENDER_CALL", "Documento receptor no encontrado");
                                }
                            })
                            .addOnFailureListener(e -> Log.e("RENDER_CALL", "Error obteniendo token", e));
                })
                .addOnFailureListener(e -> Log.e("RENDER_CALL", "Error al guardar mensaje", e));
    }
}
