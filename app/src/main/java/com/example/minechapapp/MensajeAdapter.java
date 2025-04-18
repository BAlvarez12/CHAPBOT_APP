package com.example.minechapapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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
        if (mensaje.esAudio()) {
            return TIPO_AUDIO;
        } else {
            return mensaje.isEnviado() ? TIPO_ENVIADO : TIPO_RECIBIDO;
        }
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TIPO_ENVIADO) {
            View view = inflater.inflate(R.layout.item_mensaje_enviado, parent, false);
            return new SentViewHolder(view);
        } else if (viewType == TIPO_RECIBIDO) {
            View view = inflater.inflate(R.layout.item_mensaje_recibido, parent, false);
            return new ReceivedViewHolder(view);
        } else if (viewType == TIPO_AUDIO) {
            View view = inflater.inflate(R.layout.item_mensaje_audio, parent, false);
            return new AudioViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MensajeModel mensaje = listaMensajes.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "GT"));
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Guatemala"));
        String hora = mensaje.getFecha() != null ? sdf.format(mensaje.getFecha()) : "";

        if (holder instanceof SentViewHolder) {
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

        } else if (holder instanceof ReceivedViewHolder) {
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

        } else if (holder instanceof AudioViewHolder) {
            AudioViewHolder audioHolder = (AudioViewHolder) holder;
            Context context = holder.itemView.getContext();

            if (esGrupal && mensaje.getNombreUsuario() != null) {
                audioHolder.tvNombreUsuarioAudio.setVisibility(View.VISIBLE);
                audioHolder.tvNombreUsuarioAudio.setText(mensaje.isEnviado() ? "Tú" : mensaje.getNombreUsuario());
            } else {
                audioHolder.tvNombreUsuarioAudio.setVisibility(View.GONE);
            }

            String duracion = mensaje.getDuracion() != null ? mensaje.getDuracion() : "0:00";
            audioHolder.tvDuracionAudio.setText(duracion);

            audioHolder.btnPlayAudio.setOnClickListener(v -> {
                new Thread(() -> {
                    try {
                        File audioLocal = descargarAudioDesdeUrl(mensaje.getAudioUrl(), "AUDIO_" + System.currentTimeMillis() + ".3gp");
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(audioLocal.getAbsolutePath());
                        mediaPlayer.prepare();
                        mediaPlayer.start();

                        ((Activity) context).runOnUiThread(() -> {
                            audioHolder.btnPlayAudio.setImageResource(R.drawable.avd_pause_play_vector);
                        });

                        mediaPlayer.setOnCompletionListener(mp -> {
                            ((Activity) context).runOnUiThread(() -> {
                                audioHolder.btnPlayAudio.setImageResource(R.drawable.avd_play_pause_vector);
                            });
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                        ((Activity) context).runOnUiThread(() ->
                                Toast.makeText(context, "Error al reproducir audio", Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
            });
        }

        Animation anim = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.fade);
        holder.itemView.startAnimation(anim);
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

    private File descargarAudioDesdeUrl(String url, String nombreArchivo) throws IOException {
        URL audioUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) audioUrl.openConnection();
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error al descargar archivo: " + connection.getResponseMessage());
        }
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
        connection.disconnect();
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
}
