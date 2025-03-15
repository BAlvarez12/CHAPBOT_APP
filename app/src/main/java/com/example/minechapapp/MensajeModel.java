package com.example.minechapapp;

public class MensajeModel {
    private String mensaje;
    private boolean enviado;
    private String imageUrl;
    private String nombreUsuario;
    public MensajeModel(String mensaje, boolean enviado) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.imageUrl = null;
        this.nombreUsuario = null;
    }
    public MensajeModel(String imageUrl, boolean enviado, boolean esImagen) {
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.mensaje = null;
        this.nombreUsuario = null;
    }
    public MensajeModel(String mensaje, boolean enviado, String nombreUsuario) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
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
        return imageUrl != null;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }
}
