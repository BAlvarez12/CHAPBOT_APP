package com.example.minechapapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minechapapp.R;
import com.example.minechapapp.chatActivity;
import com.example.minechapapp.GrupoActivity;
import com.example.minechapapp.Chat_individual;

import java.util.List;

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
        holder.tvUltimoMensaje.setText(chat.getUltimoMensaje() != null ? chat.getUltimoMensaje() : "");
        holder.tvHora.setText(chat.getHora() != null ? chat.getHora() : "");
        holder.ivPerfil.setImageResource(R.drawable.ic_launcher_foreground);

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            if (TIPO_CHAT_GRUPAL_ID.equals(chat.getTipoChat())) {
                // Chat grupal
                Intent intent = new Intent(context, GrupoActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("nombreGrupo", chat.getNombre());
                context.startActivity(intent);
            } else if (TIPO_CHAT_INDIVIDUAL_ID.equals(chat.getTipoChat())) {
                // Chat individual
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
