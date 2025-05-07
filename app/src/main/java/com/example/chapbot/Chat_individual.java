package com.example.chapbot;

import java.util.Date;

public class Chat_individual {

    private String chatId;
    private String otherUserId;
    private String nombre;
    private String ultimoMensaje;
    private String hora;
    private boolean esGrupal;
    private String tipoChat;
    private Date timestamp;
    private String fotoPerfilBase64;
    private String fotoUrl;

    public Chat_individual() {}

    // Chat individual
    public Chat_individual(String chatId, String otherUserId, String nombre, String ultimoMensaje, String hora) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.nombre = nombre;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.esGrupal = false;
        this.tipoChat = "NCm3QCIsKw8MjjHycvm5";
    }

    public Chat_individual(String chatId, String nombre, String ultimoMensaje, String hora) {
        this.chatId = chatId;
        this.otherUserId = "";
        this.nombre = nombre;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.esGrupal = true;
        this.tipoChat = "97XeeFNzro7xurmKwKeh";
    }

    public Chat_individual(String chatId, String otherUserId, String nombre, String ultimoMensaje, String hora, boolean esGrupal, String tipoChat) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.nombre = nombre;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.esGrupal = esGrupal;
        this.tipoChat = tipoChat;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public boolean isGrupal() {
        return esGrupal;
    }

    public void setEsGrupal(boolean esGrupal) {
        this.esGrupal = esGrupal;
    }

    public String getTipoChat() {
        return tipoChat;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getFotoPerfilBase64() {
        return fotoPerfilBase64;
    }

    public void setFotoPerfilBase64(String fotoPerfilBase64) {
        this.fotoPerfilBase64 = fotoPerfilBase64;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}
