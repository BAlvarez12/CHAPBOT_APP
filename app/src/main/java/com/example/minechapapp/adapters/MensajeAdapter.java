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

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TIPO_ENVIADO) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mensaje_enviado, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mensaje_recibido, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MensajeModel mensaje = listaMensajes.get(position);
        if (holder.getItemViewType() == TIPO_ENVIADO) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            if (mensaje.tieneImagen()) {
                // Para mensajes enviados con imagen:
                sentHolder.tvMensaje.setVisibility(View.GONE);
                sentHolder.imgMensaje.setVisibility(View.VISIBLE);
                Glide.with(sentHolder.itemView.getContext())
                        .load(mensaje.getImageUrl())
                        .into(sentHolder.imgMensaje);
            } else {
                sentHolder.imgMensaje.setVisibility(View.GONE);
                sentHolder.tvMensaje.setVisibility(View.VISIBLE);
                sentHolder.tvMensaje.setText(mensaje.getMensaje());
            }
        } else {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            receivedHolder.tvMensaje.setText(mensaje.getMensaje());
        }
    }

    public static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje;
        ImageView imgMensaje;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeEnviado);
            imgMensaje = itemView.findViewById(R.id.imgMensajeEnviado);
        }
    }

    public static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeRecibido);
        }
    }
}
