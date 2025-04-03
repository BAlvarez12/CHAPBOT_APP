package com.example.minechapapp;

import java.util.Date;

public class MensajeModel {
    private String mensaje;
    private boolean enviado;
    private String imageUrl;
    private String nombreUsuario;
    private Date fecha;

    // Constructor para mensajes de texto
    public MensajeModel(String mensaje, boolean enviado, Date fecha) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.fecha = fecha;
    }

    // Constructor para imágenes
    public MensajeModel(String imageUrl, boolean enviado, boolean esImagen, Date fecha) {
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.fecha = fecha;
    }

    // Constructor con nombre de usuario (para mensajes grupales, si lo usas)
    public MensajeModel(String mensaje, boolean enviado, String nombreUsuario, Date fecha) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isEnviado() {
        return enviado;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean tieneImagen() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public Date getFecha() {
        return fecha;
    }
}
