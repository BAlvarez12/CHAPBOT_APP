package com.example.minechapapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import android.widget.Filter;
import java.util.List;

public class UsuariosAdap extends RecyclerView.Adapter<UsuariosAdap.ViewHolder>implements Filterable {

    private List<Usuario> listaUsuarios;
    private final List<Usuario> listaUsuariosOriginal;
    private OnUsuarioClickListener listener;

    public UsuariosAdap(List<Usuario> listaUsuarios, OnUsuarioClickListener listener) {
        this.listaUsuariosOriginal = new ArrayList<>(listaUsuarios);
        this.listaUsuarios         = listaUsuarios;
        this.listener              = listener;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected Filter.FilterResults performFiltering(CharSequence constraint) {
                List<Usuario> filtrados = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtrados.addAll(listaUsuariosOriginal);
                } else {
                    String patron = constraint.toString().toLowerCase().trim();
                    for (Usuario u : listaUsuariosOriginal) {
                        if (u.getNombre().toLowerCase().contains(patron) ||
                                u.getEmail().toLowerCase().contains(patron)) {
                            filtrados.add(u);
                        }
                    }
                }
                Filter.FilterResults resultados = new Filter.FilterResults();
                resultados.values = filtrados;
                return resultados;
            }
            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                listaUsuarios.clear();
                //noinspection unchecked
                listaUsuarios.addAll((List<Usuario>) results.values);
                notifyDataSetChanged();
            }
        };
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

    public void updateList(List<Usuario> nuevaLista) {
        listaUsuarios.clear();
        listaUsuarios.addAll(nuevaLista);
        notifyDataSetChanged();
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
