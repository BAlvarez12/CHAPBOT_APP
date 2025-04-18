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
    public MensajeModel(String mensaje, boolean enviado, Date fecha) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.fecha = fecha;
    }
    public MensajeModel(String imageUrl, boolean enviado, boolean esImagen, Date fecha) {
        this.imageUrl = imageUrl;
        this.enviado = enviado;
        this.fecha = fecha;
    }
    public MensajeModel(String mensaje, boolean enviado, String nombreUsuario, Date fecha) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
    }
    public MensajeModel(String audioUrl, String duracion, boolean esEnviado, Date fecha) {
        this.audioUrl = audioUrl;
        this.duracion = duracion;
        this.esAudio = true;
        this.enviado = esEnviado;
        this.fecha = fecha;
    }

    public MensajeModel(String mensaje, boolean enviado, String nombreUsuario, Date fecha, String imageUrl) {
        this.mensaje = mensaje;
        this.enviado = enviado;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
        this.imageUrl = imageUrl;
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

}
