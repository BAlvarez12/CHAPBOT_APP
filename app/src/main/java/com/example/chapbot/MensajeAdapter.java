package com.example.chapbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.util.Base64;
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

    private static final int TIPO_ENVIADO   = 1;
    private static final int TIPO_RECIBIDO  = 2;
    private static final int TIPO_AUDIO     = 3;
    private static final int TIPO_SISTEMA   = 4;

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
        MensajeModel m = listaMensajes.get(position);
        if (m.esAudio()) return TIPO_AUDIO;
        return m.isEnviado() ? TIPO_ENVIADO : TIPO_RECIBIDO;
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENVIADO)
            return new SentViewHolder(inf.inflate(R.layout.item_mensaje_enviado, parent, false));
        else if (viewType == TIPO_RECIBIDO)
            return new ReceivedViewHolder(inf.inflate(R.layout.item_mensaje_recibido, parent, false));
        else if (viewType == TIPO_AUDIO)
            return new AudioViewHolder(inf.inflate(R.layout.item_mensaje_audio, parent, false));
        else
            return new SystemViewHolder(inf.inflate(R.layout.item_mensaje_sistema, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        MensajeModel m = listaMensajes.get(pos);
        String hora = m.getFecha() != null
                ? new SimpleDateFormat("hh:mm a", new Locale("es", "GT")).format(m.getFecha())
                : "";

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;

            if (esGrupal && m.getNombreUsuario() != null) {
                h.tvNombreUsuario.setVisibility(View.VISIBLE);
                h.tvNombreUsuario.setText("Tú");
            } else h.tvNombreUsuario.setVisibility(View.GONE);

            if (m.tieneImagen()) {
                showImage(m.getImageUrl(), h.imgMensaje, h.tvMensaje);
            } else {
                h.imgMensaje.setVisibility(View.GONE);
                h.tvMensaje.setVisibility(View.VISIBLE);
                h.tvMensaje.setText(m.getMensaje());
            }
            h.tvHora.setText(hora);

        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            if (esGrupal && m.getNombreUsuario() != null) {
                h.tvNombreUsuario.setVisibility(View.VISIBLE);
                h.tvNombreUsuario.setText(m.getNombreUsuario());
            } else h.tvNombreUsuario.setVisibility(View.GONE);

            if (m.tieneImagen()) {
                showImage(m.getImageUrl(), h.imgMensaje, h.tvMensaje);
            } else {
                h.imgMensaje.setVisibility(View.GONE);
                h.tvMensaje.setVisibility(View.VISIBLE);
                h.tvMensaje.setText(m.getMensaje());
            }
            h.tvHora.setText(hora);

        } else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;
            if (esGrupal && m.getNombreUsuario() != null) {
                h.tvNombreUsuarioAudio.setVisibility(View.VISIBLE);
                h.tvNombreUsuarioAudio.setText(m.isEnviado() ? "Tú" : m.getNombreUsuario());
            } else h.tvNombreUsuarioAudio.setVisibility(View.GONE);

            h.tvDuracionAudio.setText(m.getDuracion() != null ? m.getDuracion() : "0:00");
            h.btnPlayAudio.setOnClickListener(v -> playAudio(m, h));

        } else if (holder instanceof SystemViewHolder) {
            SystemViewHolder h = (SystemViewHolder) holder;
            h.tvMensajeSistema.setText(m.getMensaje());
            h.tvHoraSistema.setText(hora);
        }

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade);
        holder.itemView.startAnimation(anim);
    }

    private void showImage(String src, ImageView iv, TextView tv) {
        tv.setVisibility(View.GONE);
        iv.setVisibility(View.VISIBLE);
        if (src.startsWith("http")) {
            // Carga normal con Glide
            Glide.with(context)
                    .load(src)
                    .thumbnail(0.1f)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(iv);
        } else {
            try {
                byte[] data = Base64.decode(src, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                iv.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                iv.setImageResource(R.drawable.default_profile_image);
            }
        }
        iv.setOnClickListener(v -> {
            Intent i = new Intent(context, FullscreenImageActivity.class);
            i.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, src);
            context.startActivity(i);
        });
    }
    private void playAudio(MensajeModel m, AudioViewHolder h) {
        String name = "AUDIO_" + m.getId() + ".3gp";
        File f = new File(context.getExternalFilesDir(null), name);
        if (f.exists()) {
            reproducir(f, h);
        } else {
            new Thread(() -> {
                try {
                    File d = descargarAudioDesdeUrl(m.getAudioUrl(), name);
                    ((Activity)context).runOnUiThread(() -> reproducir(d, h));
                } catch (IOException ex) {
                    ((Activity)context).runOnUiThread(() ->
                            Toast.makeText(context, "Error audio", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }
    private void reproducir(File f, AudioViewHolder h) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(f.getAbsolutePath());
            mp.prepare();
            mp.start();
            h.btnPlayAudio.setImageResource(R.drawable.avd_pause_play_vector);
            mp.setOnCompletionListener(__ -> h.btnPlayAudio.setImageResource(R.drawable.avd_play_pause_vector));
        } catch (IOException e) {
            Toast.makeText(context, "Error reproducir", Toast.LENGTH_SHORT).show();
        }
    }
    private File descargarAudioDesdeUrl(String url, String name) throws IOException {
        URL u = new URL(url);
        HttpURLConnection c = (HttpURLConnection)u.openConnection();
        c.connect();
        InputStream in = c.getInputStream();
        File out = new File(context.getExternalFilesDir(null), name);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[4096]; int r;
        while ((r=in.read(buf))!=-1) fos.write(buf,0,r);
        fos.close(); in.close();
        return out;
    }

    public static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvNombreUsuario, tvHora;
        ImageView imgMensaje;
        public SentViewHolder(@NonNull View v) {
            super(v);
            tvMensaje = v.findViewById(R.id.tvMensajeEnviado);
            imgMensaje = v.findViewById(R.id.imgMensajeEnviado);
            tvNombreUsuario = v.findViewById(R.id.tvNombreUsuarioEnviado);
            tvHora = v.findViewById(R.id.tvHoraMensajeEnviado);
        }
    }
    public static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvNombreUsuario, tvHora;
        ImageView imgMensaje;
        public ReceivedViewHolder(@NonNull View v) {
            super(v);
            tvMensaje = v.findViewById(R.id.tvMensajeRecibido);
            imgMensaje = v.findViewById(R.id.imgMensajeRecibido);
            tvNombreUsuario = v.findViewById(R.id.tvNombreUsuarioRecibido);
            tvHora = v.findViewById(R.id.tvHoraMensajeRecibido);
        }
    }
    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlayAudio;
        TextView tvDuracionAudio, tvNombreUsuarioAudio;
        public AudioViewHolder(@NonNull View v) {
            super(v);
            btnPlayAudio = v.findViewById(R.id.btnPlayAudio);
            tvDuracionAudio = v.findViewById(R.id.tvDuracionAudio);
            tvNombreUsuarioAudio = v.findViewById(R.id.tvNombreUsuarioAudio);
        }
    }
    public static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensajeSistema, tvHoraSistema;
        public SystemViewHolder(@NonNull View v) {
            super(v);
            tvMensajeSistema = v.findViewById(R.id.tvMensajeSistema);
            tvHoraSistema = v.findViewById(R.id.tvHoraMensajeSistema);
        }
    }

    public void updateList(List<MensajeModel> nueva) {
        this.listaMensajes = nueva;
        notifyDataSetChanged();
    }
    public List<MensajeModel> getListaMensajes() {
        return listaMensajes;
    }
}
