package com.example.minechapapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minechapapp.adapters.ChatAdap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatsFragmento extends Fragment {

    private static final String TAG = "ChatsFragmento";
    private RecyclerView recyclerView;
    private ChatAdap chatAdap;
    private List<Chat_individual> listaDeChats;
    private FirebaseFirestore db;
    private String currentUserId;

    private final Set<String> chatsCargados = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragmento_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaDeChats = new ArrayList<>();
        chatAdap = new ChatAdap(listaDeChats);
        recyclerView.setAdapter(chatAdap);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null && getArguments().containsKey("uid")) {
            currentUserId = getArguments().getString("uid");
            Log.d(TAG, "UID obtenido de argumentos: " + currentUserId);
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "UID obtenido de FirebaseAuth: " + currentUserId);
        } else {
            Log.d(TAG, "No se encontró UID");
            Toast.makeText(getContext(), "No se encontró usuario logueado", Toast.LENGTH_SHORT).show();
            return view;
        }
        escucharCambiosEnChats();

        return view;
    }

    private void escucharCambiosEnChats() {
        // Limpiar la lista para empezar desde cero
        listaDeChats.clear();
        chatsCargados.clear();
        chatAdap.notifyDataSetChanged();

        db.collection("chats")
                .whereEqualTo("usuario_a", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error en snapshot (usuario_a): ", e);
                        return;
                    }
                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                final DocumentSnapshot doc = dc.getDocument();
                                final String chatId = doc.getId();

                                if (!TextUtils.isEmpty(chatId) && !chatsCargados.contains(chatId)) {
                                    chatsCargados.add(chatId);
                                    final String otherUserId = doc.getString("usuario_b");
                                    if (!TextUtils.isEmpty(otherUserId)) {
                                        cargarChat(chatId, otherUserId);
                                    }
                                }
                            }
                        }
                    }
                });

        db.collection("chats")
                .whereEqualTo("usuario_b", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error en snapshot (usuario_b): ", e);
                        return;
                    }
                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                final DocumentSnapshot doc = dc.getDocument();
                                final String chatId = doc.getId();

                                if (!TextUtils.isEmpty(chatId) && !chatsCargados.contains(chatId)) {
                                    chatsCargados.add(chatId);
                                    final String otherUserId = doc.getString("usuario_a");
                                    if (!TextUtils.isEmpty(otherUserId)) {
                                        cargarChat(chatId, otherUserId);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void cargarChat(final String chatId, final String otherUserId) {
        // Consulta el último mensaje usando orden DESCENDING y agregando también __name__
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.DESCENDING)
                .orderBy("__name__", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String ultimoMensaje = "Sin mensajes";
                    Date fechaMensaje = null;

                    if (!querySnapshot.isEmpty()) {
                        final DocumentSnapshot messageDoc = querySnapshot.getDocuments().get(0);

                        if (messageDoc.contains("mensaje")) {
                            ultimoMensaje = messageDoc.getString("mensaje");
                        }
                        if (messageDoc.contains("fecha_creado")) {
                            fechaMensaje = messageDoc.getDate("fecha_creado");
                        }
                    }

                    String fechaStr = "";
                    if (fechaMensaje != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        fechaStr = sdf.format(fechaMensaje);
                    }
                    final String finalUltimoMensaje = ultimoMensaje;
                    final String finalFechaStr = fechaStr;

                    db.collection("usuarios")
                            .document(otherUserId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                String nombreUsuario = "Usuario desconocido";
                                if (userDoc.exists() && userDoc.contains("nombre")) {
                                    nombreUsuario = userDoc.getString("nombre");
                                }
                                Chat_individual chat = new Chat_individual(
                                        chatId,
                                        otherUserId,
                                        nombreUsuario,
                                        finalUltimoMensaje,
                                        finalFechaStr
                                );
                                listaDeChats.add(chat);
                                chatAdap.notifyDataSetChanged();
                                Log.d(TAG, "Chat agregado: " + nombreUsuario + " - " + finalUltimoMensaje);
                            })
                            .addOnFailureListener(err ->
                                    Log.e(TAG, "Error al obtener datos del usuario " + otherUserId, err)
                            );
                })
                .addOnFailureListener(err ->
                        Log.e(TAG, "Error al consultar último mensaje para chat " + chatId, err)
                );
    }
}
