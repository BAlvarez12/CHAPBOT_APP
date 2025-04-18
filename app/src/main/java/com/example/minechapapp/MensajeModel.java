package com.example.minechapapp;

import java.util.Date;

public class MensajeModel {
    private String mensaje;
    private boolean enviado;
    private String imageUrl;
    private String nombreUsuario;
    private boolean esAudio;
    private String audioUrl;
    private String duracion;
    private Date fecha;

    private String tipo;  // Campo para definir el tipo de mensaje: "text", "image", "audio", "system", etc.

    // Campos para respuestas
    private String tipoRespuesta;
    private String contenidoRespuesta;
    private String urlRespuesta;
    private String duracionRespuesta;

    // Constructor para texto
    public MensajeModel(String mensaje, boolean enviado, Date fecha) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.fecha = fecha;
        this.tipo = "text";
    }

    // Constructor para imagen
    public MensajeModel(String imageUrl, boolean enviado, boolean esImagen, Date fecha) {
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.fecha = fecha;
        this.tipo = "image";
    }

    // Constructor con nombre para grupales
    public MensajeModel(String mensaje, boolean enviado, String nombreUsuario, Date fecha) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
        this.tipo = "text";
    }

    // Constructor para audio
    public MensajeModel(String audioUrl, String duracion, boolean esEnviado, Date fecha) {
        this.audioUrl = audioUrl;
        this.duracion = duracion;
        this.esAudio = true;
        this.enviado = esEnviado;
        this.fecha = fecha;
        this.tipo = "audio";
    }

    // Constructor completo con datos de respuesta
    public MensajeModel(String mensaje, boolean enviado, Date fecha,
                        String tipoRespuesta, String contenidoRespuesta,
                        String urlRespuesta, String duracionRespuesta) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.fecha = fecha;
        this.tipoRespuesta = tipoRespuesta;
        this.contenidoRespuesta = contenidoRespuesta;
        this.urlRespuesta = urlRespuesta;
        this.duracionRespuesta = duracionRespuesta;
    }

    // Constructor específico para mensajes del sistema
    public MensajeModel(String mensaje, Date fecha, String tipo) {
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.tipo = tipo;  // Debe ser "system"
    }

    // Getters y Setters
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

    public boolean esAudio() {
        return audioUrl != null && !audioUrl.isEmpty();
    }

    public void setEsAudio(boolean esAudio) {
        this.esAudio = esAudio;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipoRespuesta() {
        return tipoRespuesta;
    }

    public void setTipoRespuesta(String tipoRespuesta) {
        this.tipoRespuesta = tipoRespuesta;
    }

    public String getContenidoRespuesta() {
        return contenidoRespuesta;
    }

    public void setContenidoRespuesta(String contenidoRespuesta) {
        this.contenidoRespuesta = contenidoRespuesta;
    }

    public String getUrlRespuesta() {
        return urlRespuesta;
    }

    public void setUrlRespuesta(String urlRespuesta) {
        this.urlRespuesta = urlRespuesta;
    }

    public String getDuracionRespuesta() {
        return duracionRespuesta;
    }

    public void setDuracionRespuesta(String duracionRespuesta) {
        this.duracionRespuesta = duracionRespuesta;
    }

    // NUEVO: Saber si es respuesta
    public boolean esRespuesta() {
        return tipoRespuesta != null && !tipoRespuesta.isEmpty();
    }
}

