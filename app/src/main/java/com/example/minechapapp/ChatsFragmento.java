package com.example.minechapapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatsFragmento extends Fragment {

    private static final String TAG = "ChatsFragmento";
    private RecyclerView recyclerView;
    private ChatAdap chatAdap;
    private List<Chat_individual> listaDeChats;
    private FirebaseFirestore db;
    private String currentUserId;
    private final Set<String> chatsCargados = new HashSet<>();
    private ListenerRegistration listenerChats;

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            escucharCambiosEnChatsOrdenados();
            configurarSwipeParaEliminar();
        } else {
            Toast.makeText(getContext(), "No se encontró usuario logueado", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerChats != null) listenerChats.remove();
    }

    private void escucharCambiosEnChatsOrdenados() {
        chatsCargados.clear();
        if (listenerChats != null) listenerChats.remove();

        listenerChats = db.collection("chats")
                .orderBy("ultimo_mensaje_timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e(TAG, "Error escuchando chats: ", e);
                        return;
                    }
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String chatId = doc.getId();
                        String tipoChat = doc.getString("tipo_chat");
                        List<String> eliminados = (List<String>) doc.get("eliminado_por");
                        if (eliminados != null && eliminados.contains(currentUserId)) continue;
                        Date timestamp = doc.getDate("ultimo_mensaje_timestamp");
                        String hora = (timestamp != null) ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp) : "";
                        if ("97XeeFNzro7xurmKwKeh".equals(tipoChat)) {
                            String nombreGrupo = doc.getString("nombre_grupo");
                            String ultimoMensaje = "";
                            if (doc.contains("audio_url")) {
                                ultimoMensaje = "🎤 Audio";
                            }
                            String textoMensaje = doc.getString("ultimo_mensaje");
                            if (!TextUtils.isEmpty(textoMensaje)) {
                                ultimoMensaje = textoMensaje;
                            }
                            if (TextUtils.isEmpty(ultimoMensaje)) {
                                ultimoMensaje = "Sin mensaje";
                            }
                            Chat_individual chat = new Chat_individual(chatId, nombreGrupo, ultimoMensaje, hora);
                            chat.setTimestamp(timestamp);
                            chat.setFotoPerfilBase64(doc.getString("foto_grupo_base64"));
                            chatAdap.actualizarListaSinDuplicados(Collections.singletonList(chat));
                        } else {
                            String usuarioA = doc.getString("usuario_a");
                            String usuarioB = doc.getString("usuario_b");
                            String otherUserId = currentUserId.equals(usuarioA) ? usuarioB : usuarioA;
                            if (!TextUtils.isEmpty(otherUserId)) {
                                cargarNombreYCrearChat(chatId, otherUserId, doc, timestamp);
                            }
                        }
                    }
                    Collections.sort(listaDeChats, (a, b) -> {
                        Date t1 = a.getTimestamp();
                        Date t2 = b.getTimestamp();
                        if (t1 == null || t2 == null) return 0;
                        return t2.compareTo(t1);
                    });
                    chatAdap.notifyDataSetChanged();
                });
    }

    private void cargarNombreYCrearChat(String chatId, String otherUserId, DocumentSnapshot doc, Date timestamp) {
        db.collection("usuarios").document(otherUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String nombreUsuario = userDoc.getString("nombre");
                    String fotoBase64 = userDoc.getString("fotoBase64");

                    String ultimoMensaje = "";
                    if (doc.contains("audio_url")) {
                        ultimoMensaje = "🎤 Audio";
                    }
                    String textoMensaje = doc.getString("ultimo_mensaje");
                    if (!TextUtils.isEmpty(textoMensaje)) {
                        ultimoMensaje = textoMensaje;
                    }
                    if (TextUtils.isEmpty(ultimoMensaje)) {
                        ultimoMensaje = "Sin mensaje";
                    }

                    String hora = (timestamp != null) ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp) : "";

                    Chat_individual chat = new Chat_individual(
                            chatId,
                            otherUserId,
                            nombreUsuario != null ? nombreUsuario : "Usuario",
                            ultimoMensaje,
                            hora
                    );
                    chat.setTimestamp(timestamp);
                    chat.setFotoPerfilBase64(fotoBase64);

                    chatAdap.actualizarListaSinDuplicados(Collections.singletonList(chat));

                    Collections.sort(listaDeChats, (a, b) -> {
                        Date t1 = a.getTimestamp();
                        Date t2 = b.getTimestamp();
                        if (t1 == null || t2 == null) return 0;
                        return t2.compareTo(t1);
                    });

                    chatAdap.notifyDataSetChanged();
                });
    }


    private void configurarSwipeParaEliminar() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Chat_individual chat = listaDeChats.get(position);
                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar chat")
                        .setMessage("¿Estás seguro que deseas eliminar este chat?")
                        .setPositiveButton("Eliminar", (dialog, which) -> eliminarChat(chat.getChatId()))
                        .setNegativeButton("Cancelar", (dialog, which) -> chatAdap.notifyItemChanged(position))
                        .setCancelable(false)
                        .show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void eliminarChat(String chatId) {
        db.collection("chats").document(chatId)
                .update("eliminado_por", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat ocultado correctamente");

                    db.collection("notificacion")
                            .whereEqualTo("chat_id", chatId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot) {
                                    doc.getReference().delete();
                                }
                                Log.d(TAG, "Todos los mensajes del chat eliminados");
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar mensajes del chat", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al ocultar el chat", e));
    }
}