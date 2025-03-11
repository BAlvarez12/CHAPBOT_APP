package com.example.minechapapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class mensajeAdap extends RecyclerView.Adapter<mensajeAdap.MensajeViewHolder> {

    private List<String> listaMensajes;
    public mensajeAdap(List<String> listaMensajes) {
        this.listaMensajes = listaMensajes;
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vistaItem = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mensaje, parent, false);
        return new MensajeViewHolder(vistaItem);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        String mensaje = listaMensajes.get(position);
        holder.tvMensaje.setText(mensaje);
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    public static class MensajeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje;

        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensaje);
        }
    }
}
