package com.example.minechapapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UsuariosAdap extends RecyclerView.Adapter<UsuariosAdap.ViewHolder> {

    private List<Usuario> listaUsuarios;
    private OnUsuarioClickListener listener;

    public UsuariosAdap(List<Usuario> listaUsuarios, OnUsuarioClickListener listener) {
        this.listaUsuarios = listaUsuarios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Usuario usuario = listaUsuarios.get(position);
        holder.bind(usuario, listener);
    }

    @Override
    public int getItemCount() {
        return listaUsuarios.size();
    }

    public interface OnUsuarioClickListener {
        void onUsuarioClick(Usuario usuario);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNombre;
        private final TextView tvEmail;
        private final View itemLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            itemLayout = itemView;
        }

        public void bind(Usuario usuario, OnUsuarioClickListener listener) {
            tvNombre.setText(usuario.getNombre());
            tvEmail.setText(usuario.getEmail());
            if (usuario.isSeleccionado()) {
                itemLayout.setBackgroundColor(
                        ContextCompat.getColor(itemLayout.getContext(), R.color.teal_200)
                );
            } else {
                itemLayout.setBackgroundColor(
                        ContextCompat.getColor(itemLayout.getContext(), android.R.color.transparent)
                );
            }

            itemLayout.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUsuarioClick(usuario);
                }
            });
        }
    }
}
