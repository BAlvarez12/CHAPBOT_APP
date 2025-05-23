package com.example.chapbot;

import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChatsFragmento extends Fragment {

    private static final String TAG = "ChatsFragmento";

    private RecyclerView recyclerView;
    private ChatAdap chatAdap;
    private List<Chat_individual> listaDeChats;

    private RecyclerView quickRv;
    private ChatAdapter quickAdapter;
    private List<UserMenu> quickItems;

    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration listenerChats;

    private final Set<String> chatsCargados = new HashSet<>();
    private final Map<String, DocumentSnapshot> usuariosPendientes = new HashMap<>();
    private final List<Chat_individual> bufferChatsIndividuales = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragmento_chat, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No se encontró usuario logueado", Toast.LENGTH_SHORT).show();
            return view;
        }
        currentUserId = user.getUid();

        recyclerView = view.findViewById(R.id.recyclerChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaDeChats = new ArrayList<>();
        chatAdap = new ChatAdap(listaDeChats);
        recyclerView.setAdapter(chatAdap);

        quickRv = view.findViewById(R.id.recyclerQuickMenu);
        quickRv.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        quickItems = new ArrayList<>();
        quickAdapter = new ChatAdapter(quickItems, userId -> {
            Intent i = new Intent(getActivity(), chatActivity.class);
            i.putExtra("USER_ID", userId);
            startActivity(i);
        });
        quickRv.setAdapter(quickAdapter);
        quickRv.setVisibility(View.GONE);

        db.collection("Usuario_estado_menu")
                .whereEqualTo("estado", 1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) return;
                    quickRv.setVisibility(View.VISIBLE);
                    for (DocumentSnapshot st : qs) {
                        String uid = st.getId();
                        db.collection("usuarios").document(uid)
                                .get()
                                .addOnSuccessListener(uDoc -> {
                                    String name = uDoc.getString("nombre");
                                    String photo = uDoc.getString("fotoBase64");
                                    quickItems.add(new UserMenu(uid, name, photo));
                                    quickAdapter.notifyItemInserted(quickItems.size() - 1);
                                });
                    }
                });

        escucharCambiosEnChatsOrdenados();
        configurarSwipeParaEliminar();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        escucharCambiosEnChatsOrdenados();
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

                    List<Chat_individual> nuevosChats = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String chatId = doc.getId();
                        List<String> eliminados = (List<String>) doc.get("eliminado_por");
                        if (eliminados != null && eliminados.contains(currentUserId)) continue;

                        Date timestamp = doc.getDate("ultimo_mensaje_timestamp");
                        String hora = (timestamp != null)
                                ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp)
                                : "";

                        String tipoChat = doc.getString("tipo_chat");
                        if ("97XeeFNzro7xurmKwKeh".equals(tipoChat)) {
                            String nombreGrupo = doc.getString("nombre_grupo");
                            String ultimoMensaje = obtenerPreview(
                                    doc.getString("ultimo_mensaje_tipo"),
                                    doc.getString("ultimo_mensaje")
                            );
                            Chat_individual chat = new Chat_individual(chatId, nombreGrupo, ultimoMensaje, hora);
                            chat.setTimestamp(timestamp);
                            chat.setFotoPerfilBase64(doc.getString("foto_grupo_base64"));
                            nuevosChats.add(chat);
                        } else {
                            String usuarioA = doc.getString("usuario_a");
                            String usuarioB = doc.getString("usuario_b");
                            String otherUserId = currentUserId.equals(usuarioA) ? usuarioB : usuarioA;
                            if (!TextUtils.isEmpty(otherUserId)) {
                                cargarNombreYCrearChat(chatId, otherUserId, doc, timestamp);
                            }
                        }
                    }

                    chatAdap.actualizarListaSinDuplicados(nuevosChats);
                    Collections.sort(listaDeChats, (a, b) -> {
                        Date t1 = a.getTimestamp(), t2 = b.getTimestamp();
                        return (t1 == null || t2 == null) ? 0 : t2.compareTo(t1);
                    });
                    chatAdap.notifyDataSetChanged();
                });
    }

    private String obtenerPreview(String tipo, String texto) {
        if ("imagen".equals(tipo)) return "📷 Imagen";
        if ("audio".equals(tipo))  return "🎤 Audio";
        return !TextUtils.isEmpty(texto) ? texto : "Sin mensaje";
    }

    private void cargarNombreYCrearChat(String chatId, String otherUserId,
                                        DocumentSnapshot doc, Date timestamp) {
        if (usuariosPendientes.containsKey(otherUserId)) return;
        usuariosPendientes.put(otherUserId, doc);

        db.collection("usuarios").document(otherUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String nombreUsuario = userDoc.getString("nombre");
                    String fotoBase64   = userDoc.getString("fotoBase64");
                    String ultimoMensaje = obtenerPreview(
                            doc.getString("ultimo_mensaje_tipo"),
                            doc.getString("ultimo_mensaje")
                    );
                    String hora = (timestamp != null)
                            ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp)
                            : "";

                    Chat_individual chat = new Chat_individual(
                            chatId, otherUserId,
                            nombreUsuario != null ? nombreUsuario : "Usuario",
                            ultimoMensaje, hora
                    );
                    chat.setTimestamp(timestamp);
                    chat.setFotoPerfilBase64(fotoBase64);
                    bufferChatsIndividuales.add(chat);

                    if (bufferChatsIndividuales.size() == usuariosPendientes.size()) {
                        chatAdap.actualizarListaSinDuplicados(bufferChatsIndividuales);
                        Collections.sort(listaDeChats, (a, b) ->
                                (a.getTimestamp()==null||b.getTimestamp()==null)?0:
                                        b.getTimestamp().compareTo(a.getTimestamp())
                        );
                        chatAdap.notifyDataSetChanged();
                        usuariosPendientes.clear();
                        bufferChatsIndividuales.clear();
                    }
                });
    }

    private void configurarSwipeParaEliminar() {
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                        int pos = vh.getAdapterPosition();
                        Chat_individual chat = listaDeChats.get(pos);
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Eliminar chat")
                                .setMessage("¿Seguro que deseas eliminar éste chat?")
                                .setPositiveButton("Eliminar", (d, w) ->
                                        eliminarChat(chat.getChatId()))
                                .setNegativeButton("Cancelar", (d, w) ->
                                        chatAdap.notifyItemChanged(pos))
                                .setCancelable(false)
                                .show();
                    }
                };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void eliminarChat(String chatId) {
        db.collection("chats").document(chatId)
                .update("eliminado_por", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat ocultado correctamente");

                    db.collection("notificacion")
                            .whereEqualTo("chat_id", chatId)
                            .get()
                            .addOnSuccessListener(qs -> {
                                for (DocumentSnapshot d : qs) d.getReference().delete();
                                Log.d(TAG, "Mensajes del chat eliminados");
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error eliminando mensajes", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error ocultando chat", e));
    }
}
