package com.example.minechapapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.minechapapp.R;
import com.example.minechapapp.chatActivity;
import com.example.minechapapp.Chat_individual;
import java.util.List;

public class ChatAdap extends RecyclerView.Adapter<ChatAdap.ChatViewHolder> {

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
            Intent intent = new Intent(context, chatActivity.class);
            intent.putExtra("nombreUsuario", chat.getNombre() != null ? chat.getNombre() : "Usuario desconocido");
            intent.putExtra("USER_ID", chat.getOtherUserId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaDeChats.size();
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
