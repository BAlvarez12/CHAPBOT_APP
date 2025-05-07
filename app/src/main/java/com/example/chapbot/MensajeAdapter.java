package com.example.chapbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ENVIADO = 1;
    private static final int TIPO_RECIBIDO = 2;
    private static final int TIPO_AUDIO = 3;
    private static final int TIPO_SISTEMA = 4;

    private List<MensajeModel> listaMensajes;
    private boolean esGrupal;
    private Context context;

    public MensajeAdapter(Context context, List<MensajeModel> listaMensajes, boolean esGrupal) {
        this.context = context;
        this.listaMensajes = listaMensajes;
        this.esGrupal = esGrupal;
    }

    @Override
    public int getItemViewType(int position) {
        MensajeModel mensaje = listaMensajes.get(position);
        if (mensaje.esAudio()) return TIPO_AUDIO;
        return mensaje.isEnviado() ? TIPO_ENVIADO : TIPO_RECIBIDO;
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENVIADO)
            return new SentViewHolder(inflater.inflate(R.layout.item_mensaje_enviado, parent, false));
        else if (viewType == TIPO_RECIBIDO)
            return new ReceivedViewHolder(inflater.inflate(R.layout.item_mensaje_recibido, parent, false));
        else if (viewType == TIPO_AUDIO)
            return new AudioViewHolder(inflater.inflate(R.layout.item_mensaje_audio, parent, false));
        else
            return new SystemViewHolder(inflater.inflate(R.layout.item_mensaje_sistema, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MensajeModel mensaje = listaMensajes.get(position);
        String hora = mensaje.getFecha() != null
                ? new SimpleDateFormat("hh:mm a", new Locale("es", "GT")).format(mensaje.getFecha())
                : "";

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            if (esGrupal && mensaje.getNombreUsuario() != null) {
                h.tvNombreUsuario.setVisibility(View.VISIBLE);
                h.tvNombreUsuario.setText("Tú");
            } else h.tvNombreUsuario.setVisibility(View.GONE);

            if (mensaje.tieneImagen()) {
                h.tvMensaje.setVisibility(View.GONE);
                h.imgMensaje.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(mensaje.getImageUrl())
                        .thumbnail(0.1f)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(h.imgMensaje);
                h.imgMensaje.setOnClickListener(v -> {
                    Intent i = new Intent(context, FullscreenImageActivity.class);
                    i.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, mensaje.getImageUrl());
                    context.startActivity(i);
                });
            } else {
                h.imgMensaje.setVisibility(View.GONE);
                h.tvMensaje.setVisibility(View.VISIBLE);
                h.tvMensaje.setText(mensaje.getMensaje());
            }
            h.tvHora.setText(hora);

        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            if (esGrupal && mensaje.getNombreUsuario() != null) {
                h.tvNombreUsuario.setVisibility(View.VISIBLE);
                h.tvNombreUsuario.setText(mensaje.getNombreUsuario());
            } else h.tvNombreUsuario.setVisibility(View.GONE);

            if (mensaje.tieneImagen()) {
                h.tvMensaje.setVisibility(View.GONE);
                h.imgMensaje.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(mensaje.getImageUrl())
                        .thumbnail(0.1f)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(h.imgMensaje);
                h.imgMensaje.setOnClickListener(v -> {
                    Intent i = new Intent(context, FullscreenImageActivity.class);
                    i.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, mensaje.getImageUrl());
                    context.startActivity(i);
                });
            } else {
                h.imgMensaje.setVisibility(View.GONE);
                h.tvMensaje.setVisibility(View.VISIBLE);
                h.tvMensaje.setText(mensaje.getMensaje());
            }
            h.tvHora.setText(hora);

        } else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;
            if (esGrupal && mensaje.getNombreUsuario() != null) {
                h.tvNombreUsuarioAudio.setVisibility(View.VISIBLE);
                h.tvNombreUsuarioAudio.setText(mensaje.isEnviado() ? "Tú" : mensaje.getNombreUsuario());
            } else h.tvNombreUsuarioAudio.setVisibility(View.GONE);

            h.tvDuracionAudio.setText(mensaje.getDuracion() != null ? mensaje.getDuracion() : "0:00");
            h.btnPlayAudio.setOnClickListener(v -> {
                String nombreArchivo = "AUDIO_" + mensaje.getId() + ".3gp";
                File archivoAudio = new File(context.getExternalFilesDir(null), nombreArchivo);
                if (archivoAudio.exists()) {
                    reproducirAudio(archivoAudio, h);
                } else {
                    new Thread(() -> {
                        try {
                            File descargado = descargarAudioDesdeUrl(mensaje.getAudioUrl(), nombreArchivo);
                            ((Activity) context).runOnUiThread(() -> reproducirAudio(descargado, h));
                        } catch (IOException e) {
                            ((Activity) context).runOnUiThread(() ->
                                    Toast.makeText(context, "Error al descargar audio", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                }
            });

        } else if (holder instanceof SystemViewHolder) {
            SystemViewHolder h = (SystemViewHolder) holder;
            h.tvMensajeSistema.setText(mensaje.getMensaje());
            h.tvHoraSistema.setText(hora);
        }

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade);
        holder.itemView.startAnimation(anim);
    }

    private void reproducirAudio(File archivoAudio, AudioViewHolder h) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(archivoAudio.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            h.btnPlayAudio.setImageResource(R.drawable.avd_pause_play_vector);
            mediaPlayer.setOnCompletionListener(mp ->
                    h.btnPlayAudio.setImageResource(R.drawable.avd_play_pause_vector)
            );
        } catch (IOException e) {
            Toast.makeText(context, "Error al reproducir audio", Toast.LENGTH_SHORT).show();
        }
    }

    private File descargarAudioDesdeUrl(String url, String nombreArchivo) throws IOException {
        URL audioUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) audioUrl.openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();
        File outputFile = new File(context.getExternalFilesDir(null), nombreArchivo);
        FileOutputStream output = new FileOutputStream(outputFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        output.close();
        input.close();
        return outputFile;
    }

    public static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvNombreUsuario, tvHora;
        ImageView imgMensaje;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeEnviado);
            imgMensaje = itemView.findViewById(R.id.imgMensajeEnviado);
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuarioEnviado);
            tvHora = itemView.findViewById(R.id.tvHoraMensajeEnviado);
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
            tvHora = itemView.findViewById(R.id.tvHoraMensajeRecibido);
        }
    }

    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlayAudio;
        TextView tvDuracionAudio, tvNombreUsuarioAudio;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            btnPlayAudio = itemView.findViewById(R.id.btnPlayAudio);
            tvDuracionAudio = itemView.findViewById(R.id.tvDuracionAudio);
            tvNombreUsuarioAudio = itemView.findViewById(R.id.tvNombreUsuarioAudio);
        }
    }

    public static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensajeSistema, tvHoraSistema;

        public SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensajeSistema = itemView.findViewById(R.id.tvMensajeSistema);
            tvHoraSistema = itemView.findViewById(R.id.tvHoraMensajeSistema);
        }
    }
}
