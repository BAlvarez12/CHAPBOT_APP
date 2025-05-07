package com.example.chapbot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;


public class ChatAdap extends RecyclerView.Adapter<ChatAdap.ChatViewHolder> {
    private static final String TIPO_CHAT_INDIVIDUAL_ID = "NCm3QCIsKw8MjjHycvm5";
    private static final String TIPO_CHAT_GRUPAL_ID = "97XeeFNzro7xurmKwKeh";

    private List<Chat_individual> listaDeChats;

    public ChatAdap(List<Chat_individual> listaDeChats) {
        this.listaDeChats = listaDeChats;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vistaItem = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(vistaItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat_individual chat = listaDeChats.get(position);
        holder.tvNombre.setText(chat.getNombre() != null ? chat.getNombre() : "Usuario desconocido");
        holder.tvUltimoMensaje.setText(chat.getUltimoMensaje() != null ? chat.getUltimoMensaje() : "Sin mensaje");
        holder.tvHora.setText(chat.getHora() != null ? chat.getHora() : "");

        // Mostrar imagen de perfil desde Base64 si está disponible
        if (chat.getFotoPerfilBase64() != null && !chat.getFotoPerfilBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(chat.getFotoPerfilBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivPerfil.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.ivPerfil.setImageResource(R.drawable.default_profile_image);
            }
        } else {
            holder.ivPerfil.setImageResource(R.drawable.default_profile_image);
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            if (TIPO_CHAT_GRUPAL_ID.equals(chat.getTipoChat())) {
                Intent intent = new Intent(context, GrupoActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("nombreGrupo", chat.getNombre());
                context.startActivity(intent);
            } else if (TIPO_CHAT_INDIVIDUAL_ID.equals(chat.getTipoChat())) {
                Intent intent = new Intent(context, chatActivity.class);
                intent.putExtra("nombreUsuario", chat.getNombre() != null ? chat.getNombre() : "Usuario desconocido");
                intent.putExtra("USER_ID", chat.getOtherUserId());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Tipo de chat desconocido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaDeChats != null ? listaDeChats.size() : 0;
    }

    public void actualizarListaSinDuplicados(List<Chat_individual> nuevosChats) {
        // 🔍 Crear un mapa temporal para buscar por ID
        Map<String, Chat_individual> mapa = new HashMap<>();

        // 1️⃣ Agrega los existentes
        for (Chat_individual chat : listaDeChats) {
            mapa.put(chat.getChatId(), chat);
        }

        // 2️⃣ Agrega o reemplaza con los nuevos
        for (Chat_individual nuevo : nuevosChats) {
            mapa.put(nuevo.getChatId(), nuevo);
        }

        // 3️⃣ Limpia y agrega todos en orden
        listaDeChats.clear();
        listaDeChats.addAll(mapa.values());

        // 4️⃣ Ordena por timestamp descendente
        listaDeChats.sort((a, b) -> {
            if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
            return Objects.requireNonNull(b.getTimestamp()).compareTo(a.getTimestamp());
        });

        notifyDataSetChanged();
    }
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvUltimoMensaje, tvHora;
        ImageView ivPerfil;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvUltimoMensaje = itemView.findViewById(R.id.tvUltimoMensaje);
            tvHora = itemView.findViewById(R.id.tvHora);
            ivPerfil = itemView.findViewById(R.id.ivPerfil);
        }
    }
}
