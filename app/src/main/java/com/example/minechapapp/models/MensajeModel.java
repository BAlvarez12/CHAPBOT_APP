package com.example.minechapapp.models;

public class MensajeModel {
    private String mensaje;
    private boolean enviado;
    private String imageUrl; // URI de la imagen (null si es solo texto)

    // Constructor para mensajes de texto
    public MensajeModel(String mensaje, boolean enviado) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.imageUrl = null; // No hay imagen
    }

    // Constructor para mensajes con imagen
    public MensajeModel(String imageUrl, boolean enviado, boolean esImagen) {
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.mensaje = null; // No hay texto
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
}
