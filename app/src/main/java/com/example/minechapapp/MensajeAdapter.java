package com.example.minechapapp;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ENVIADO = 1;
    private static final int TIPO_RECIBIDO = 2;
    private static final int TIPO_AUDIO = 3;
    private static final int TIPO_SISTEMA = 4; // Nuevo tipo para mensajes del sistema

    private List<MensajeModel> listaMensajes;
    private boolean esGrupal;
    private Context context;

    private OnMensajeLongClickListener listener;

    public interface OnMensajeLongClickListener {
        void onMensajeLongClick(MensajeModel mensaje);
    }

    public void setOnMensajeLongClickListener(OnMensajeLongClickListener listener) {
        this.listener = listener;
    }

    public MensajeAdapter(Context context, List<MensajeModel> listaMensajes, boolean esGrupal) {
        this.context = context;
        this.listaMensajes = listaMensajes;
        this.esGrupal = esGrupal;
    }

    @Override
    public int getItemViewType(int position) {
        MensajeModel mensaje = listaMensajes.get(position);

        if (mensaje.getTipo() != null && mensaje.getTipo().equals("system")) {
            return TIPO_SISTEMA;
        } else if (mensaje.esAudio()) {
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
        } else {
            View view = inflater.inflate(R.layout.item_mensaje_sistema, parent, false);
            return new SystemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MensajeModel mensaje = listaMensajes.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "GT"));
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Guatemala"));
        String hora = mensaje.getFecha() != null ? sdf.format(mensaje.getFecha()) : "";

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;

            if (esGrupal && mensaje.getNombreUsuario() != null) {
                h.tvNombreUsuario.setVisibility(View.VISIBLE);
                h.tvNombreUsuario.setText("Tú");
            } else {
                h.tvNombreUsuario.setVisibility(View.GONE);
            }

            if (mensaje.tieneImagen()) {
                h.tvMensaje.setVisibility(View.GONE);
                h.imgMensaje.setVisibility(View.VISIBLE);
                Glide.with(context).load(mensaje.getImageUrl()).into(h.imgMensaje);
            } else {
                h.imgMensaje.setVisibility(View.GONE);
                h.tvMensaje.setVisibility(View.VISIBLE);
                h.tvMensaje.setText(mensaje.getMensaje());
            }

            h.tvHora.setText(hora);
            mostrarVistaPreviaRespuesta(h.layoutReply, h.tvReplyText, h.imgReply, mensaje);

            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMensajeLongClick(mensaje);
                    return true;
                }
                return false;
            });

        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;

            if (esGrupal && mensaje.getNombreUsuario() != null) {
                h.tvNombreUsuario.setVisibility(View.VISIBLE);
                h.tvNombreUsuario.setText(mensaje.getNombreUsuario());
            } else {
                h.tvNombreUsuario.setVisibility(View.GONE);
            }

            if (mensaje.tieneImagen()) {
                h.tvMensaje.setVisibility(View.GONE);
                h.imgMensaje.setVisibility(View.VISIBLE);
                Glide.with(context).load(mensaje.getImageUrl()).into(h.imgMensaje);
            } else {
                h.imgMensaje.setVisibility(View.GONE);
                h.tvMensaje.setVisibility(View.VISIBLE);
                h.tvMensaje.setText(mensaje.getMensaje());
            }

            h.tvHora.setText(hora);
            mostrarVistaPreviaRespuesta(h.layoutReply, h.tvReplyText, h.imgReply, mensaje);

            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMensajeLongClick(mensaje);
                    return true;
                }
                return false;
            });

        } else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;
            String duracion = mensaje.getDuracion() != null ? mensaje.getDuracion() : "0:00";
            h.tvDuracionAudio.setText(duracion);
            h.btnPlayAudio.setOnClickListener(v -> {
                new Thread(() -> {
                    try {
                        File audioLocal = descargarAudioDesdeUrl(mensaje.getAudioUrl(), "AUDIO_" + System.currentTimeMillis() + ".3gp");
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(audioLocal.getAbsolutePath());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        ((Activity) context).runOnUiThread(() ->
                                Toast.makeText(context, "Error al reproducir audio", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            });

        } else if (holder instanceof SystemViewHolder) {
            SystemViewHolder systemHolder = (SystemViewHolder) holder;
            systemHolder.tvMensajeSistema.setText(mensaje.getMensaje());
            systemHolder.tvHoraSistema.setText(hora);
        }

        Animation anim = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.fade);
        holder.itemView.startAnimation(anim);
    }

    private void mostrarVistaPreviaRespuesta(LinearLayout layout, TextView tvTexto, ImageView img, MensajeModel mensaje) {
        if (mensaje.getTipoRespuesta() == null) {
            layout.setVisibility(View.GONE);
            return;
        }

        layout.setVisibility(View.VISIBLE);
        tvTexto.setVisibility(View.VISIBLE);
        img.setVisibility(View.GONE);

        switch (mensaje.getTipoRespuesta()) {
            case "text":
                tvTexto.setText("↪️ " + mensaje.getContenidoRespuesta());
                break;
            case "image":
                tvTexto.setText("📷 Imagen");
                img.setVisibility(View.VISIBLE);
                Glide.with(context).load(mensaje.getUrlRespuesta()).into(img);
                break;
            case "audio":
                String dur = mensaje.getDuracionRespuesta() != null ? mensaje.getDuracionRespuesta() : "";
                tvTexto.setText("🎤 Audio " + dur);
                break;
            default:
                layout.setVisibility(View.GONE);
        }
    }

    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlayAudio;
        TextView tvDuracionAudio;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            btnPlayAudio = itemView.findViewById(R.id.btnPlayAudio);
            tvDuracionAudio = itemView.findViewById(R.id.tvDuracionAudio);
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
        TextView tvMensaje, tvNombreUsuario, tvHora, tvReplyText;
        ImageView imgMensaje, imgReply;
        LinearLayout layoutReply;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeEnviado);
            imgMensaje = itemView.findViewById(R.id.imgMensajeEnviado);
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuarioEnviado);
            tvHora = itemView.findViewById(R.id.tvHoraMensajeEnviado);
            tvReplyText = itemView.findViewById(R.id.tvReplyTextEnviado);
            imgReply = itemView.findViewById(R.id.imgReplyEnviado);
            layoutReply = itemView.findViewById(R.id.layoutReplyEnviado);
        }
    }

    public static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvNombreUsuario, tvHora, tvReplyText;
        ImageView imgMensaje, imgReply;
        LinearLayout layoutReply;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeRecibido);
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuarioRecibido);
            imgMensaje = itemView.findViewById(R.id.imgMensajeRecibido);
            tvHora = itemView.findViewById(R.id.tvHoraMensajeRecibido);
            tvReplyText = itemView.findViewById(R.id.tvReplyTextRecibido);
            imgReply = itemView.findViewById(R.id.imgReplyRecibido);
            layoutReply = itemView.findViewById(R.id.layoutReplyRecibido);
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

