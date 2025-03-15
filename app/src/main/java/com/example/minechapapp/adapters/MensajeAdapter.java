package com.example.minechapapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.minechapapp.R;
import com.example.minechapapp.models.MensajeModel;
import java.util.List;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.ViewHolder> {

    private static final int TIPO_ENVIADO = 1;
    private static final int TIPO_RECIBIDO = 2;
    private List<MensajeModel> listaMensajes;

    public MensajeAdapter(List<MensajeModel> listaMensajes) {
        this.listaMensajes = listaMensajes;
    }

    @Override
    public int getItemViewType(int position) {
        return listaMensajes.get(position).isEnviado() ? TIPO_ENVIADO : TIPO_RECIBIDO;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TIPO_ENVIADO) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_enviado, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_recibido, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MensajeModel mensaje = listaMensajes.get(position);

        if (mensaje.tieneImagen()) {
            // Si el mensaje es una imagen, ocultamos el TextView y mostramos la imagen
            holder.tvMensaje.setVisibility(View.GONE);
            holder.imgMensaje.setVisibility(View.VISIBLE);

            // Cargar la imagen con Glide
            Glide.with(holder.itemView.getContext())
                    .load(mensaje.getImageUrl())
                    .into(holder.imgMensaje);
        } else {
            // Si el mensaje es texto, ocultamos la imagen y mostramos el texto
            holder.tvMensaje.setVisibility(View.VISIBLE);
            holder.imgMensaje.setVisibility(View.GONE);
            holder.tvMensaje.setText(mensaje.getMensaje());
        }
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje;
        ImageView imgMensaje;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeEnviado); // Asume que el id es el mismo para ambos
            imgMensaje = itemView.findViewById(R.id.imgMensajeEnviado); // Nuevo ImageView
        }
    }
}
