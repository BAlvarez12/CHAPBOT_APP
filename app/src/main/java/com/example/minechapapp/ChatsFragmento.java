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
                                DocumentSnapshot doc = dc.getDocument();
                                String chatId = doc.getId();
                                if (!chatsCargados.contains(chatId)) {
                                    chatsCargados.add(chatId);
                                    String otherUserId = doc.getString("usuario_b");
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
                                DocumentSnapshot doc = dc.getDocument();
                                String chatId = doc.getId();
                                if (!chatsCargados.contains(chatId)) {
                                    chatsCargados.add(chatId);
                                    String otherUserId = doc.getString("usuario_a");
                                    if (!TextUtils.isEmpty(otherUserId)) {
                                        cargarChat(chatId, otherUserId);
                                    }
                                }
                            }
                        }
                    }
                });
        db.collection("chats")
                .whereArrayContains("participantes", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error en snapshot (grupal): ", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                DocumentSnapshot doc = dc.getDocument();
                                String chatId = doc.getId();
                                if (!chatsCargados.contains(chatId)) {
                                    chatsCargados.add(chatId);
                                    String nombreGrupo = doc.getString("nombre_grupo");
                                    cargarChatGrupal(chatId, nombreGrupo);
                                }
                            }
                        }
                    }
                });
    }
    private void cargarChat(final String chatId, final String otherUserId) {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String ultimoMensaje = "Sin mensajes";
                    String fechaStr = "";

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot messageDoc = querySnapshot.getDocuments().get(0);

                        if (messageDoc.contains("mensaje")) {
                            ultimoMensaje = messageDoc.getString("mensaje");
                        }
                        if (messageDoc.contains("fecha_creado")) {
                            Date fechaMensaje = messageDoc.getDate("fecha_creado");
                            if (fechaMensaje != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                fechaStr = sdf.format(fechaMensaje);
                            }
                        }
                    }

                    procesarUltimoMensajeIndividual(chatId, otherUserId, ultimoMensaje, fechaStr);

                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Error al obtener último mensaje del chat: " + err.getMessage());
                });
    }
    private void procesarUltimoMensajeIndividual(String chatId, String otherUserId,
                                                 String ultimoMensaje, String fechaStr) {
        db.collection("usuarios").document(otherUserId)
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
                            ultimoMensaje,
                            fechaStr
                    );

                    listaDeChats.add(chat);
                    chatAdap.notifyDataSetChanged();
                    Log.d(TAG, "Chat individual agregado: " + nombreUsuario + " - " + ultimoMensaje);
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Error al obtener usuario: " + err.getMessage());
                });
    }
    private void cargarChatGrupal(final String chatId, final String nombreGrupo) {
        db.collection("notificacion")
                .whereEqualTo("chat_id", chatId)
                .orderBy("fecha_creado", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String ultimoMensaje = "Sin mensajes";
                    String fechaStr = "";

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot messageDoc = querySnapshot.getDocuments().get(0);

                        if (messageDoc.contains("mensaje")) {
                            ultimoMensaje = messageDoc.getString("mensaje");
                        }
                        if (messageDoc.contains("fecha_creado")) {
                            Date fechaMensaje = messageDoc.getDate("fecha_creado");
                            if (fechaMensaje != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                fechaStr = sdf.format(fechaMensaje);
                            }
                        }
                    }

                    procesarUltimoMensajeGrupal(chatId, nombreGrupo, ultimoMensaje, fechaStr);

                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Error al obtener último mensaje del grupo: " + err.getMessage());
                });
    }

    private void procesarUltimoMensajeGrupal(String chatId, String nombreGrupo,
                                             String ultimoMensaje, String fechaStr) {
        // Usamos el constructor para chat grupal (4 parámetros)
        Chat_individual chat = new Chat_individual(
                chatId,
                nombreGrupo,
                ultimoMensaje,
                fechaStr
        );

        listaDeChats.add(chat);
        chatAdap.notifyDataSetChanged();
        Log.d(TAG, "Chat grupal agregado: " + nombreGrupo + " - " + ultimoMensaje);
    }
}
