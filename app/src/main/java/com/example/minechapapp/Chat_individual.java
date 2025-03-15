package com.example.minechapapp;

public class Chat_individual {
    private String chatId;
    private String otherUserId;
    private String nombre;
    private String ultimoMensaje;
    private String hora;
    public Chat_individual(String chatId, String otherUserId, String nombre, String ultimoMensaje, String hora) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.nombre = nombre;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
    }
    public Chat_individual(String nombre, String ultimoMensaje, String hora) {
        this("", "", nombre, ultimoMensaje, hora);
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
}
