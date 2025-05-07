package com.example.chapbot;

import java.util.Date;
import java.util.UUID;

public class MensajeModel {
    private String id; // ID único
    private String mensaje;
    private String imageUrl;
    private String audioUrl;
    private String duracion;
    private String nombreUsuario; // solo para grupo
    private boolean enviado;
    private boolean esAudio;
    private Date fecha;

    // ✅ Chat individual (sin nombreUsuario)
    public MensajeModel(String mensaje, boolean enviado, Date fecha) {
        this.id = UUID.randomUUID().toString();
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.fecha = fecha;
    }

    public MensajeModel(String imageUrl, boolean enviado, Date fecha, boolean esImagen) {
        this.id = UUID.randomUUID().toString();
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.fecha = fecha;
    }

    public MensajeModel(String audioUrl, String duracion, boolean enviado, Date fecha) {
        this.id = UUID.randomUUID().toString();
        this.audioUrl = audioUrl;
        this.duracion = duracion;
        this.enviado = enviado;
        this.fecha = fecha;
        this.esAudio = true;
    }

    // ✅ Grupo (con nombreUsuario)
    public MensajeModel(String mensaje, boolean enviado, String nombreUsuario, Date fecha) {
        this.id = UUID.randomUUID().toString();
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
    }

    public MensajeModel(String imageUrl, boolean enviado, String nombreUsuario, Date fecha, boolean esImagen) {
        this.id = UUID.randomUUID().toString();
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
    }

    public MensajeModel(String audioUrl, String duracion, boolean enviado, String nombreUsuario, Date fecha) {
        this.id = UUID.randomUUID().toString();
        this.audioUrl = audioUrl;
        this.duracion = duracion;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
        this.esAudio = true;
    }

    // ✅ Getters
    public String getId() {
        return id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean tieneImagen() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public boolean esImagen() {
        return tieneImagen();
    }

    public boolean esAudio() {
        return esAudio;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getDuracion() {
        return duracion;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public boolean isEnviado() {
        return enviado;
    }

    public Date getFecha() {
        return fecha;
    }
}
