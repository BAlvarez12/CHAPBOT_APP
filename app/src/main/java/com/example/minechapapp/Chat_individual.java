package com.example.minechapapp;

public class Chat_individual {

    private String chatId;
    private String otherUserId;
    private String nombre;
    private String ultimoMensaje;
    private String hora;
    private boolean esGrupal;
    private String tipoChat;
    public Chat_individual() {
    }
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

    public String getOtherUserId() {
        return otherUserId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public String getHora() {
        return hora;
    }

    public boolean isGrupal() {
        return esGrupal;
    }

    public String getTipoChat() {
        return tipoChat;
    }
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public void setEsGrupal(boolean esGrupal) {
        this.esGrupal = esGrupal;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }
}
