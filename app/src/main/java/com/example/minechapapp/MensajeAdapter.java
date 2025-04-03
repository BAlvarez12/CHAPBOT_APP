package com.example.minechapapp;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ENVIADO = 1;
    private static final int TIPO_RECIBIDO = 2;
    private List<MensajeModel> listaMensajes;
    private boolean esGrupal;

    public MensajeAdapter(List<MensajeModel> listaMensajes, boolean esGrupal) {
        this.listaMensajes = listaMensajes;
        this.esGrupal = esGrupal;
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

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "GT"));
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Guatemala"));
        String hora = mensaje.getFecha() != null ? sdf.format(mensaje.getFecha()) : "";

        if (holder.getItemViewType() == TIPO_ENVIADO) {
            SentViewHolder sentHolder = (SentViewHolder) holder;

            if (esGrupal && mensaje.getNombreUsuario() != null) {
                sentHolder.tvNombreUsuario.setVisibility(View.VISIBLE);
                sentHolder.tvNombreUsuario.setText("Tú");
            } else {
                sentHolder.tvNombreUsuario.setVisibility(View.GONE);
            }

            if (mensaje.tieneImagen()) {
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

            sentHolder.tvHora.setText(hora);

        } else {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;

            if (esGrupal && mensaje.getNombreUsuario() != null) {
                receivedHolder.tvNombreUsuario.setVisibility(View.VISIBLE);
                receivedHolder.tvNombreUsuario.setText(mensaje.getNombreUsuario());
            } else {
                receivedHolder.tvNombreUsuario.setVisibility(View.GONE);
            }

            if (mensaje.tieneImagen()) {
                receivedHolder.tvMensaje.setVisibility(View.GONE);
                receivedHolder.imgMensaje.setVisibility(View.VISIBLE);
                Glide.with(receivedHolder.itemView.getContext())
                        .load(mensaje.getImageUrl())
                        .into(receivedHolder.imgMensaje);
            } else {
                receivedHolder.imgMensaje.setVisibility(View.GONE);
                receivedHolder.tvMensaje.setVisibility(View.VISIBLE);
                receivedHolder.tvMensaje.setText(mensaje.getMensaje());
            }

            receivedHolder.tvHora.setText(hora);
        }
    }

    public static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvNombreUsuario, tvHora;
        ImageView imgMensaje;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeEnviado);
            imgMensaje = itemView.findViewById(R.id.imgMensajeEnviado);
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuarioEnviado);
            tvHora = itemView.findViewById(R.id.tvHoraMensajeEnviado); // <-- Nuevo
        }
    }

    public static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvNombreUsuario, tvHora;
        ImageView imgMensaje;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeRecibido);
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuarioRecibido);
            imgMensaje = itemView.findViewById(R.id.imgMensajeRecibido);
            tvHora = itemView.findViewById(R.id.tvHoraMensajeRecibido); // <-- Nuevo
        }
    }
}
